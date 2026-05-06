package com.iflytek.rpa.auth.sp.casdoor.service.extend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.auth.sp.casdoor.entity.CasdoorLoginDto;
import com.iflytek.rpa.auth.sp.casdoor.entity.CasdoorLoginResult;
import com.iflytek.rpa.auth.sp.casdoor.entity.CasdoorSignupDto;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.casbin.casdoor.config.Config;
import org.casbin.casdoor.util.http.CasdoorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@Slf4j
@org.springframework.stereotype.Service
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorLoginExtendService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Config config;

    private final RestTemplate restTemplate = new RestTemplate();

    public CasdoorLoginResult login(CasdoorLoginDto loginDto) throws IOException {
        //  application
        if (loginDto.getApplication() == null || loginDto.getApplication().isEmpty()) {
            loginDto.setApplication(config.applicationName);
        }

        // 생성 URL
        String endpoint = config.endpoint.replaceAll("/$", "");
        String url = endpoint + "/api/login?clientId=" + URLEncoder.encode(config.clientId, "UTF-8")
                + "&responseType=scope&redirectUri=";

        // 전송요청 
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = objectMapper.writeValueAsString(loginDto);
        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);

        // 파싱
        CasdoorResponse<String, String> casdoorResponse =
                objectMapper.readValue(response.getBody(), new TypeReference<CasdoorResponse<String, String>>() {});

        // 조회상태
        if (!"ok".equals(casdoorResponse.getStatus())) {
            throw new IOException("로그인실패: " + (casdoorResponse.getMsg() != null ? casdoorResponse.getMsg() : "지원하지 않는오류"));
        }

        // 가져오기결과
        String userId = casdoorResponse.getData();
        if (userId == null || userId.isEmpty()) {
            throw new IOException("로그인 응답에 사용자 ID가 없습니다");
        }

        // 가져오기 Set-Cookie 중의 casdoor_session_id
        String casdoorSessionId = extractCasdoorSessionId(response);

        CasdoorLoginResult result = new CasdoorLoginResult();
        result.setUserId(userId);
        result.setSession(casdoorSessionId);
        return result;
    }

    public CasdoorLoginResult signup(CasdoorSignupDto signupDto) throws IOException {
        // 값
        if (signupDto.getApplication() == null || signupDto.getApplication().isEmpty()) {
            signupDto.setApplication(config.applicationName);
        }
        if (signupDto.getOrganization() == null || signupDto.getOrganization().isEmpty()) {
            signupDto.setOrganization(config.organizationName);
        }

        // 생성 URL
        String endpoint = config.endpoint.replaceAll("/$", "");
        String url = endpoint + "/api/signup";

        // 전송요청 
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = objectMapper.writeValueAsString(signupDto);
        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);

        // 파싱
        CasdoorResponse<String, String> casdoorResponse =
                objectMapper.readValue(response.getBody(), new TypeReference<CasdoorResponse<String, String>>() {});

        // 조회상태
        if (!"ok".equals(casdoorResponse.getStatus())) {
            throw new IOException("회원가입실패: " + (casdoorResponse.getMsg() != null ? casdoorResponse.getMsg() : "지원하지 않는오류"));
        }

        // 가져오기결과
        String userId = casdoorResponse.getData();
        if (userId == null || userId.isEmpty()) {
            throw new IOException("회원가입 응답에 사용자 ID가 없습니다");
        }

        CasdoorLoginResult result = new CasdoorLoginResult();
        result.setUserId(userId);
        return result;
    }

    /**
     * 에서중가져오기 casdoor_session_id
     * Casdoor 반환의 cookie 형식: casdoor_session_id=dbf0c10e8a8486c61a612a69594df0cc
     */
    private String extractCasdoorSessionId(ResponseEntity<String> response) {
        List<String> cookies = response.getHeaders().get("Set-Cookie");
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }

        // 조회 casdoor_session_id
        for (String cookie : cookies) {
            String[] parts = cookie.split(";");
            if (parts.length > 0) {
                String pair = parts[0].trim();
                if (pair.startsWith("casdoor_session_id=")) {
                    return pair.substring("casdoor_session_id=".length());
                }
            }
        }

        return null;
    }

    /**
     * 에서요청 중가져오기 casdoor_session_id
     * HTTP 요청 중의 Cookie 형식: Cookie: casdoor_session_id=dbf0c10e8a8486c61a612a69594df0cc; other_cookie=value
     *
     * @param request HTTP 요청 
     * @return Casdoor session ID, 결과가찾을 수 없는 이면반환 null
     */
    public String extractCasdoorSessionIdFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 에서요청 중가져오기 Cookie 문자열
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader == null || cookieHeader.isEmpty()) {
            return null;
        }

        // 파싱 Cookie 문자열, 조회 casdoor_session_id
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String pair = cookie.trim();
            if (pair.startsWith("casdoor_session_id=")) {
                return pair.substring("casdoor_session_id=".length());
            }
        }

        return null;
    }

    /**
     * 호출 Casdoor 로그아웃연결
     *
     * @param casdoorSessionId Casdoor session ID
     * @throws IOException
     */
    public void logout(String casdoorSessionId) throws IOException {
        if (casdoorSessionId == null || casdoorSessionId.isEmpty()) {
            log.warn("Casdoor session ID 비어 있습니다, 건너뛰기로그아웃");
            return;
        }

        // 생성 URL
        String endpoint = config.endpoint.replaceAll("/$", "");
        String url = endpoint + "/api/logout";

        // 요청 
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "casdoor_session_id=" + casdoorSessionId);

        // 전송 POST 요청 (없음요청 )
        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), String.class);

        // 파싱
        CasdoorResponse<String, Object> casdoorResponse =
                objectMapper.readValue(response.getBody(), new TypeReference<CasdoorResponse<String, Object>>() {});

        // 조회상태
        if (!"ok".equals(casdoorResponse.getStatus())) {
            log.warn("Casdoor 로그아웃실패: {}", casdoorResponse.getMsg());
            throw new IOException(
                    "Casdoor 로그아웃실패: " + (casdoorResponse.getMsg() != null ? casdoorResponse.getMsg() : "지원하지 않는오류"));
        }

        log.info("Casdoor 로그아웃성공");
    }
}