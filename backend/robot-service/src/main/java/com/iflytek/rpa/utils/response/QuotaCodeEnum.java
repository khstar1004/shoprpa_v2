package com.iflytek.rpa.utils.response;

public enum QuotaCodeEnum {
    S_SUCCESS("000", "성공"),

    S_REPEAT_JOIN("001", "재복사추가입력"),

    E_OVER_MARKET_USER_NUM_LIMIT("100", "초과출력마켓사람데이터위제한"),

    E_OVER_LIMIT("101", "초과출력위제한"),

    E_EXPIRE("102", "실패");

    private String resultCode;

    private String msg;

    QuotaCodeEnum(String resultCode, String msg) {
        this.resultCode = resultCode;
        this.msg = msg;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getMsg() {
        return msg;
    }
}