-- lua/auth_handler.lua

local http = require("resty.http")
local json = require("cjson")
local ngx_log = ngx.log
local ngx_DEBUG = ngx.DEBUG
local ngx_ERR = ngx.ERR
local ngx_WARN = ngx.WARN
local ngx_HTTP_OK = ngx.HTTP_OK
local ngx_HTTP_UNAUTHORIZED = ngx.HTTP_UNAUTHORIZED
local ngx_HTTP_INTERNAL_SERVER_ERROR = ngx.HTTP_INTERNAL_SERVER_ERROR

-- Validate user context before proxying protected service requests.
local function authenticate_user()
    local ctx_type = ngx.var.context_type or "HTTP"
    ngx_log(ngx_DEBUG, "Starting authentication for " .. ctx_type .. " request. URI: " .. ngx.var.request_uri)

    local session_token = nil
    local cookie_type = nil

    -- 1. Try Authorization: Bearer <token>.
    local authorization_header = ngx.req.get_headers()["authorization"]
    if authorization_header then
        ngx_log(ngx_DEBUG, "Found Authorization header: " .. authorization_header)
        local _, _, token_type, token_value = string.find(authorization_header, "^(%S+)%s+(.+)$")
        if token_type and token_type:lower() == "bearer" then
            session_token = token_value
            ngx_log(ngx_DEBUG, "Extracted Bearer token from Authorization header.")
        else
            ngx_log(ngx_DEBUG, "Authorization header is present but not Bearer type, type: " .. (token_type or "nil"))
        end
    else
        ngx_log(ngx_DEBUG, "No Authorization header found.")
    end

    -- 2. Try the legacy Token header.
    if not session_token then
        local custom_token_header = ngx.req.get_headers()["token"]
        if custom_token_header then
            session_token = custom_token_header
            ngx_log(ngx_DEBUG, "Extracted Token from custom 'Token' header: " .. session_token)
        else
            ngx_log(ngx_DEBUG, "No custom 'Token' header found.")
        end
    end

    -- 3. Try SESSION or JSESSIONID cookies.
    if not session_token then
        local cookie_header = ngx.var.http_cookie
        if cookie_header then
            ngx_log(ngx_DEBUG, "Found Cookie header: " .. cookie_header)
            for cookie_pair in string.gmatch(cookie_header, "[^;]+") do
                local cookie_name, cookie_value = string.match(cookie_pair, "^%s*(.-)%s*=%s*(.-)%s*$")
                if cookie_name == "SESSION" then
                    session_token = cookie_value
                    cookie_type = "SESSION"
                    ngx_log(ngx_DEBUG, "Extracted Token from Cookie SESSION: " .. session_token)
                    break
                elseif cookie_name == "JSESSIONID" then
                    session_token = cookie_value
                    cookie_type = "JSESSIONID"
                    ngx_log(ngx_DEBUG, "Extracted Token from Cookie JSESSIONID: " .. session_token)
                    break
                end
            end
            if not session_token then
                ngx_log(ngx_DEBUG, "Cookie header present but no SESSION or JSESSIONID found.")
            end
        else
            ngx_log(ngx_DEBUG, "No Cookie header found.")
        end
    end

    -- 4. Try API key query parameter for MCP/API-key flows.
    if not session_token then
        local args = ngx.req.get_uri_args()
        if args.key then
            session_token = args.key
            ngx_log(ngx_DEBUG, "Extracted Token from query parameter 'key': " .. session_token)
        else
            ngx_log(ngx_DEBUG, "No query parameter 'key' found.")
        end
    end

    if not session_token or session_token == "" or session_token == " " then
        ngx_log(ngx_ERR, "Missing SESSION/Token in " .. ctx_type .. " request after trying all sources.")
        ngx.status = ngx_HTTP_UNAUTHORIZED
        ngx.say(json.encode({code = "4001", msg = "Missing SESSION/Token in request"}))
        return ngx.exit(ngx_HTTP_UNAUTHORIZED)
    end

    ngx_log(ngx_DEBUG, "Successfully extracted session_token: '" .. session_token .. "'")

    local getUserUrl = "http://robot-service:8040/api/robot/user/info"
    local httpc = http.new()

    -- robot-service validates the session through the same cookie contract used by the app.
    local cookie_name_for_service = cookie_type or "JSESSIONID"
    local headers_to_robot_service = {
        ["Content-Type"] = "application/json",
        ["Cookie"] = cookie_name_for_service .. "=" .. session_token
    }

    ngx_log(ngx_DEBUG, "Calling robot-service (" .. getUserUrl .. ") with headers: " .. json.encode(headers_to_robot_service))

    local res, err = httpc:request_uri(getUserUrl, {
        method = "GET",
        headers = headers_to_robot_service,
        ssl_verify_host = false, -- 내부부서통신통신일반아니오필요 SSL 검증인증
        ssl_verify_peer = false,
        read_timeout = 5000,
        connect_timeout = 5000
    })

    if err then
        ngx_log(ngx_ERR, "Failed to connect to robot-service for " .. ctx_type .. " auth: " .. err .. ", URL: " .. getUserUrl)
        ngx.status = ngx_HTTP_INTERNAL_SERVER_ERROR
        ngx.say(json.encode({code = "5000", message = "Internal Server Error: Auth service unavailable"}))
        return ngx.exit(ngx_HTTP_INTERNAL_SERVER_ERROR)
    end

    ngx_log(ngx_DEBUG, "robot-service response status: " .. res.status .. ", body (first 200 chars): " .. (res.body and string.sub(res.body, 1, 200) or "No body"))

    if res.status ~= ngx_HTTP_OK then
        ngx_log(ngx_ERR, "robot-service returned unexpected status " .. res.status .. " for " .. ctx_type .. " auth, full body: " .. (res.body or "No body"))
        ngx.status = res.status
        ngx.say(res.body)
        return ngx.exit(res.status)
    end

    local userResponse, json_err = json.decode(res.body)
    if json_err then
        ngx_log(ngx_ERR, "Failed to decode robot-service response for " .. ctx_type .. " auth: " .. json_err .. ", full body: " .. (res.body or "No body"))
        ngx.status = ngx_HTTP_INTERNAL_SERVER_ERROR
        ngx.say(json.encode({code = "5000", message = "Internal Server Error: Invalid auth service response"}))
        return ngx.exit(ngx_HTTP_INTERNAL_SERVER_ERROR)
    end

    ngx_log(ngx_DEBUG, "Decoded robot-service response: " .. json.encode(userResponse))

    local response_code = userResponse.code
    local is_success = (response_code == "000000") or (response_code == 200) or (tostring(response_code) == "000000")
    
    if not is_success then
        ngx_log(ngx_ERR, "robot-service returned error code: " .. (response_code or "nil") .. ", message: " .. (userResponse.message or "nil") .. " for " .. ctx_type .. " auth. Full response: " .. json.encode(userResponse))
        ngx.status = ngx_HTTP_UNAUTHORIZED
        ngx.say(json.encode({
            code = response_code or "U_AUTH_FAIL",
            data = userResponse.data,
            message = userResponse.message or "Authentication failed by robot-service"
        }))
        return ngx.exit(ngx_HTTP_UNAUTHORIZED)
    end

    local user_id = userResponse.data and userResponse.data["id"]
    if not user_id then
        ngx_log(ngx_ERR, "robot-service response missing 'id' in 'data' field for " .. ctx_type .. " auth: " .. json.encode(userResponse))
        ngx.status = ngx_HTTP_INTERNAL_SERVER_ERROR
        ngx.say(json.encode({code = "5000", message = "Internal Server Error: Auth service response missing user_id"}))
        return ngx.exit(ngx_HTTP_INTERNAL_SERVER_ERROR)
    end

    ngx_log(ngx_WARN, "User authenticated successfully. user_id: " .. user_id .. " in " .. ctx_type .. " context. Setting headers.")
    ngx.req.set_header("user_id", user_id)
    ngx.req.set_header("user-info", json.encode({id = user_id}))

    return true
end

local _M = {
    authenticate_user = authenticate_user
}

_M.authenticate_user()

return _M
