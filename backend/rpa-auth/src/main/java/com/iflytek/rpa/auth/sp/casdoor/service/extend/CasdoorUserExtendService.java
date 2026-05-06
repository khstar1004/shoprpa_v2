package com.iflytek.rpa.auth.sp.casdoor.service.extend;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.List;
import org.casbin.casdoor.config.Config;
import org.casbin.casdoor.entity.User;
import org.casbin.casdoor.service.UserService;
import org.casbin.casdoor.util.Map;
import org.casbin.casdoor.util.http.CasdoorResponse;
import org.casbin.casdoor.util.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @desc: casdoor기존서비스의사용자서비스, 에서casdoor profile아래
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/12/11 10:17
 */
@Service
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorUserExtendService extends UserService {

    public CasdoorUserExtendService(Config config) {
        super(config);
    }

    public User getUserById(String id) throws IOException {
        CasdoorResponse<User, Object> resp =
                doGet("get-user", Map.of("userId", id), new TypeReference<CasdoorResponse<User, Object>>() {});
        return objectMapper.convertValue(resp.getData(), User.class);
    }

    public List<User> getUsers(String organizationName) throws IOException {
        CasdoorResponse<List<User>, Object> resp = doGet(
                "get-users",
                Map.of("owner", organizationName),
                new TypeReference<CasdoorResponse<List<User>, Object>>() {});
        return resp.getData();
    }

    public User getUserByPhone(String phone) throws IOException {
        CasdoorResponse<User, Object> resp =
                doGet("get-user", Map.of("phone", phone), new TypeReference<CasdoorResponse<User, Object>>() {});
        return objectMapper.convertValue(resp.getData(), User.class);
    }

    /**
     * 조회사용자비밀번호여부정상
     * @param user 사용자 정보(패키지사용자명및비밀번호)
     * @return true 결과가비밀번호정상, false 결과가비밀번호오류
     * @throws IOException 결과가발송IO예외
     */
    public boolean checkUserPassword(User user) throws IOException {
        String payload = objectMapper.writeValueAsString(user);

        // 직선연결호출HTTP방법법, doPost에서status != "ok"시출력예외
        String url = String.format("%s/api/check-user-password", config.endpoint);
        String response = HttpClient.postString(url, payload, credential);

        // 파싱
        CasdoorResponse<User, Boolean> resp =
                objectMapper.readValue(response, new TypeReference<CasdoorResponse<User, Boolean>>() {});

        // 근거status비밀번호여부정상
        if ("ok".equals(resp.getStatus())) {
            return true;
        } else {
            return false;
        }
    }
}