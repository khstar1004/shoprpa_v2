package com.iflytek.rpa.utils.response;

/**
 * @author wyzhou3
 * @version 1.0
 * @description Shared API response code definitions.
 **/
public enum ErrorCodeEnum {
    /**/
    S_SUCCESS("000000", "완료"),

    OPEN_AUTH_UAC_SMS_VERIFY_CODE_ERROR("005001001", "인증 코드오류"),
    OPEN_AUTH_UAC_USER_IS_EMPTY("005001002", "사용자데이터비어 있습니다"),
    OPEN_AUTH_XFYUN_OPEN_PLATFORM_DATA_IS_EMPTY("005002001", "ShopRPA 연동 플랫폼 데이터가 비어 있습니다"),
    OPEN_AUTH_XFYUN_OPEN_PLATFORM_BINDING_DATA_IS_EMPTY("005002002", "ShopRPA 연동 플랫폼 바인딩 데이터가 비어 있습니다"),
    OPEN_AUTH_XFYUN_OPEN_PLATFORM_ALREADY_IS_BINDING("005002003", "ShopRPA 연동 플랫폼이 이미 바인딩되었습니다"),

    E_PARAM("500000", "매개변수예외"),
    E_PARAM_LOSE("500001", "매개변수 실패"),
    E_PARAM_PARSE("500002", "매개변수파싱실패"),
    E_PARAM_CHECK("500003", "매개변수검증실패"),

    E_SERVICE("600000", "서비스예외"),
    E_SERVICE_NOT_SUPPORT("600001", "서비스지원하지 않음"),
    E_SERVICE_INFO_LOSE("600002", "서비스정보 실패"),
    E_SERVICE_POWER_LIMIT("600003", "서비스권한제한제어"),

    E_SQL("700000", "데이터예외"),
    E_SQL_EMPTY("700001", "데이터비어 있습니다"),
    E_SQL_REPEAT("700002", "데이터재복사"),
    E_SQL_EXCEPTION("700003", "데이터예외"),

    E_MONGO("710000", "데이터예외"),
    E_MONGO_EMPTY("710001", "데이터비어 있습니다"),
    E_MONGO_REPEAT("710002", "데이터재복사"),
    E_MONGO_EXCEPTION("710003", "데이터예외"),

    E_API("800000", "삼방법연결예외"),
    E_API_FAIL("800001", "삼방법연결요청 실패"),
    E_API_EXCEPTION("800002", "삼방법연결요청 예외"),

    E_COMMON("999999", "요청 예외,다시 시도하세요"),
    E_NOT_LOGIN("900001", "로그인되지 않았습니다"),
    E_NO_POWER("900002", "권한이 없습니다"),
    E_NO_ACCOUNT("900003", "계정찾을 수 없습니다"),
    E_EXCEPTION("900004", "지원하지 않는예외"),

    E_AUTH_STATUS_FAILED("302", "로그인상태인증 실패");

    private final String code;

    private final String flag;

    ErrorCodeEnum(String code, String flag) {
        this.code = code;
        this.flag = flag;
    }

    public String getCode() {
        return code;
    }

    public String getFlag() {
        return flag;
    }
}
