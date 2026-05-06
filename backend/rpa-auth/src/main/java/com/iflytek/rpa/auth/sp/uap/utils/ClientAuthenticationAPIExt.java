package com.iflytek.rpa.auth.sp.uap.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.iflytek.rpa.auth.conf.condition.ConditionalOnSaaSOrUAP;
import com.iflytek.rpa.auth.core.entity.enums.LoginModeEnum;
import com.iflytek.rpa.auth.sp.uap.entity.LoginResultDto;
import com.iflytek.rpa.auth.utils.Sha256Utils;
import com.iflytek.sec.uap.base.util.ClientConfigUtil;
import com.iflytek.sec.uap.client.api.ClientAuthenticationAPI;
import com.iflytek.sec.uap.client.core.client.AuthenticationClient;
import com.iflytek.sec.uap.client.core.dto.ResponseDto;
import com.iflytek.sec.uap.client.core.enums.MethodEnum;
import com.iflytek.sec.uap.client.core.model.AuthenticationClientOptions;
import com.iflytek.sec.uap.client.core.model.UapRequestConfig;
import com.iflytek.sec.uap.client.core.util.JsonUtils;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * ClientAuthenticationAPI 유형
 * 통신경과계정행로그인의방법법
 *
 * @author lihang
 * @date 2025-11-25
 */
@Slf4j
@ConditionalOnSaaSOrUAP
public class ClientAuthenticationAPIExt extends ClientAuthenticationAPI {

    /**
     * 통신경과계정행로그인, 가져오기 ticket
     *
     * @param loginName 로그인계정
     * @param tenantId 테넌트ID(가능선택, 결과가비어 있습니다이면사용빈문자열)
     * @return 로그인결과, 패키지 ticket
     */
    public static LoginResultDto loginUapByAccount(String loginName, String tenantId) {
        // 생성 AuthenticationClient, 매칭에서 ClientConfigUtil 가져오기
        AuthenticationClientOptions clientOptions = new AuthenticationClientOptions();
        clientOptions.setAppAuthCode(ClientConfigUtil.instance().getAppAuthCode());
        clientOptions.setAppCode(ClientConfigUtil.instance().getAppCode());
        clientOptions.setUapHost(ClientConfigUtil.instance().getCasServerContext());
        clientOptions.setProtocol(ClientConfigUtil.instance().getProtocol());
        AuthenticationClient authenticationClient = new AuthenticationClient(clientOptions);

        // 에서매칭가져오기 appCode 및 appAuthCode, 사용완료이름
        String appCode = clientOptions.getAppCode();
        String appAuthCode = clientOptions.getAppAuthCode();

        // 에서매칭가져오기 cas-client-context 로 service
        String service = ClientConfigUtil.instance().getCasClientContext();

        // 완료시간
        String timeStamp = String.valueOf(System.currentTimeMillis());

        // 생성로그인매개변수
        Map<String, String> loginParams = new HashMap<>(8);
        loginParams.put("loginName", loginName);
        loginParams.put("tenantId", StringUtils.isNotBlank(tenantId) ? tenantId : "default-tenant");
        loginParams.put("service", service);
        loginParams.put("orgId", "");
        loginParams.put("credentialType", LoginModeEnum.NOPASSWORD.getCode());
        loginParams.put("appCode", appCode);

        // 완료이름: sha256Hmac(appAuthCode, loginName + "|" + appCode + "|" + timeStamp)
        String signData = loginName + "|" + appCode + "|" + timeStamp;
        String sign = Sha256Utils.sha256Hmac(appAuthCode, signData);
        loginParams.put("sign", sign);
        loginParams.put("timeStamp", timeStamp);

        // 사용 UapRequestConfig 및 request 방법법전송요청 
        UapRequestConfig config = new UapRequestConfig("/api/v2/login", MethodEnum.FORM_POST, loginParams);
        String response = authenticationClient.request(config);

        // 파싱
        ResponseDto<LoginResultDto> result =
                JsonUtils.parseObject(response, new TypeReference<ResponseDto<LoginResultDto>>() {});

        if (result == null || !result.isFlag()) {
            String errorMsg = result != null ? result.getMessage() : "로그인파싱실패";
            log.error("로그인실패: {}", errorMsg);
            throw new RuntimeException("로그인실패: " + errorMsg);
        }

        return result.getData();
    }

    /**
     * 통신경과계정행로그인, 가져오기 ticket(사용테넌트ID)
     *
     * @param loginName 로그인계정
     * @return 로그인결과, 패키지 ticket
     */
    public static LoginResultDto loginUapByAccount(String loginName) {
        return loginUapByAccount(loginName, null);
    }
}