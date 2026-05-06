-- lua/auth_handler.lua

local http = require("resty.http")
local json = require("cjson")
local ngx_log = ngx.log
local ngx_DEBUG = ngx.DEBUG -- 사용항목항목디버그
local ngx_ERR = ngx.ERR
local ngx_WARN = ngx.WARN
local ngx_HTTP_OK = ngx.HTTP_OK
local ngx_HTTP_UNAUTHORIZED = ngx.HTTP_UNAUTHORIZED
local ngx_HTTP_INTERNAL_SERVER_ERROR = ngx.HTTP_INTERNAL_SERVER_ERROR

-- 지정항목일개항목데이터항목관리인증항목
local function authenticate_user()
    local ctx_type = ngx.var.context_type or "HTTP"
    ngx_log(ngx_DEBUG, "Starting authentication for " .. ctx_type .. " request. URI: " .. ngx.var.request_uri)

    local session_token = nil
    local cookie_type = nil -- 항목기록에서항목개 cookie 가져오기의 token (SESSION 또는 JSESSIONID)

    -- 1. 항목시도에서 Authorization header 가져오기 Bearer Token
    local authorization_header = ngx.req.get_headers()["authorization"] -- 비고항목, headers 항목예소항목
    if authorization_header then
        ngx_log(ngx_DEBUG, "Found Authorization header: " .. authorization_header)
        local _, _, token_type, token_value = string.find(authorization_header, "^(%S+)%s+(.+)$")
        if token_type and token_type:lower() == "bearer" then
            -- session_token = token_value
            -- ngx_log(ngx_DEBUG, "Extracted Bearer Token from Authorization header: " .. session_token)
            return
        else
            ngx_log(ngx_DEBUG, "Authorization header is present but not Bearer type, type: " .. (token_type or "nil"))
        end
    else
        ngx_log(ngx_DEBUG, "No Authorization header found.")
    end

    -- 2. 예결과 Authorization header 항목있음, 항목시도에서항목지정항목 'token' header 가져오기 (항목예 X-Token 또는 Token)
    -- 항목항목의 'http_token' 항목의예이름로 'Token' 의항목지정항목
    if not session_token then
        local custom_token_header = ngx.req.get_headers()["token"]
        if custom_token_header then
            session_token = custom_token_header
            ngx_log(ngx_DEBUG, "Extracted Token from custom 'Token' header: " .. session_token)
        else
            ngx_log(ngx_DEBUG, "No custom 'Token' header found.")
        end
    end

    -- 3. 예결과항목있음token, 항목시도에서Cookie중가져오기 SESSION 또는 JSESSIONID
    if not session_token then
        local cookie_header = ngx.var.http_cookie
        if cookie_header then
            ngx_log(ngx_DEBUG, "Found Cookie header: " .. cookie_header)
            -- 파싱Cookie, 항목조회항목 SESSION, 항목후조회항목 JSESSIONID
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

    -- 4. 예결과항목예항목있음 token, 항목시도에서조회매개변수중가져오기 API Key (지원 MCP 및항목 API Key 인증)
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

    -- 호출 robot-service 항목행인증
    local getUserUrl = "http://robot-service:8040/api/robot/user/info"
    local httpc = http.new()

    -- 준비전송항목 robot-service 의 Headers
    -- 사용Cookie방법방식항목SESSION또는JSESSIONID항목robot-service
    -- 항목근거가져오기까지의 cookie 유형항목지정전송항목개 cookie
    local cookie_name_for_service = cookie_type or "JSESSIONID" -- 예결과예에서항목항목가져오기의, 항목사용 JSESSIONID
    local headers_to_robot_service = {
        ["Content-Type"] = "application/json",
        -- 예시: 예결과 robot-service 항목 Authorization 항목
        -- ["Authorization"] = "Bearer " .. session_token,
        -- 항목근거 cookie 유형전송항목의 cookie
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
        ngx.say(res.body) -- 를 robot-service 의오류항목직선연결반환
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

    -- robot-service 성공시반환 code 로 "000000" (문자열) 또는 200 (숫자)
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

    return true -- 인증성공
end

-- 에서 access_by_lua_file 실행시, 항목본항목직선연결실행.
-- 항목으로항목필요직선연결호출 authenticate_user 항목데이터.
local _M = {
    authenticate_user = authenticate_user
}

_M.authenticate_user()

return _M