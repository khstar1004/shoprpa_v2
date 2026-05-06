package com.iflytek.rpa.auth.utils;

import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.apache.commons.codec.digest.Md5Crypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author jqfang3
 * @since 2025-08-21
 */
@Component
public class SmsUtils {
    @Value("${sms.apiUrl:}")
    public String apiSendSms;

    @Value("${sms.secretKey:}")
    public String secretKey;

    @Value("${sms.appId:}")
    public String appId;

    @Value("${sms.tid:}")
    public String tid;

    /**
     * 전송짧음정보
     *
     * @param phone 휴대폰 번호, 다중개사용`,`열기, 아니요빈격식
     * @param text  필요전송의내용
     */
    public AppResponse send(String phone, String text) {
        // 검증휴대폰 번호여부비어 있습니다
        if (StringUtils.isEmpty(phone)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "휴대폰 번호가 비어 있습니다");
        }
        Map<String, Object> tpMap = new HashMap<>();
        tpMap.put("sendContent", text);
        return sendSms(phone, tid, tpMap);
    }

    /**
     * 전송짧음정보
     *
     * @param phone 휴대폰 번호, 다중개사용`,`열기, 아니요빈격식
     * @param tid   짧음정보 ID
     * @param tpMap 짧음정보 매개변수
     * @return SmsResponse
     */
    public AppResponse sendSms(String phone, String tid, Map<String, Object> tpMap) {
        // 분할휴대폰 번호
        String[] phoneArr = phone.split(",");
        String tpJson = JSONObject.toJSONString(tpMap);

        return sendSms(phoneArr, tid, tpJson);
    }

    /**
     * 전송짧음정보
     *
     * @param phone 휴대폰 번호, 다중개사용`,`열기, 아니요빈격식
     * @param tid   짧음정보 ID
     * @param tp    짧음정보 매개변수
     * @return SmsResponse
     */
    public AppResponse sendSms(String[] phone, String tid, String tp) {
        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("phone", phone);
        params.put("tid", tid);
        params.put("tp", tp);
        String sign = getSign(params);
        params.put("sign", sign);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpMethod method = HttpMethod.POST;
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(JSONObject.toJSONString(params), headers);
        ResponseEntity<String> response = restTemplate.exchange(apiSendSms, method, requestEntity, String.class);
        return JSONObject.parseObject(response.getBody(), AppResponse.class);
    }

    /**
     * 가져오기 이름
     * 일, 전송데이터로합치기 M, 를합치기 M 내부가득파일의매개변수 매개변수이름 ASCII 코드에서소까지대정렬(딕셔너리순서), 
     * 사용 URL 값의형식( key1=value1&key2=value2…)연결성공문자열 stringRequest.
     * 비고으로아래재필요이면: 
     * • 매개변수값비어 있습니다의아니요매개및이름;
     * • sign 매개변수본아니요매개및이름;
     * • 매개변수이름분크기.
     * 이, 에서 stringRequest 후연결위 API 키 key 까지 stringSignRequest 문자열, 
     *  stringSignRequest 행 MD5 실행, 를까지의문자열모든문자기호변환로대, 
     * 까지 sign 값 signValue.API 키필요에서짧음정보관리관리행신청매칭
     *
     * @param params 요청 매개변수
     * @return String
     */
    private String getSign(Map<String, Object> params) {
        // 요청 매개변수변환문자열
        String stringRequest = getRequestString(params);
        // 연결키
        String signStr = stringRequest + "key=" + secretKey;
        // Md5 + 전체변환대
        return Md5Crypt.apr1Crypt(signStr.getBytes()).toUpperCase();
    }

    /**
     * 요청 매개변수변환문자열
     * 전송데이터로합치기 M, 를합치기 M 내부가득파일의매개변수 매개변수이름 ASCII 코드에서소까지대정렬(딕셔너리순서), 
     * 사용 URL 값의형식( key1=value1&key2=value2…)연결성공문자열 stringRequest.
     * 비고으로아래재필요이면: 
     * • 매개변수값비어 있습니다의아니요매개및이름;
     * • sign 매개변수본아니요매개및이름;
     * • 매개변수이름분크기.
     *
     * @param params 요청 매개변수
     * @return String
     */
    private String getRequestString(Map<String, Object> params) {
        if (params == null) {
            return null;
        }
        if (params.get("sign") != null) {
            params.remove("sign");
        }
        List<String> keyList = new ArrayList<>(params.keySet());
        Collections.sort(keyList);
        StringBuilder sb = new StringBuilder();
        for (String key : keyList) {
            String value = params.get(key).toString();
            sb.append(key).append("=").append(value).append("&");
        }
        return sb.toString();
    }
}