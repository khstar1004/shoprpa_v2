package com.iflytek.rpa.auth.utils;

import java.io.Serializable;
import org.springframework.util.StringUtils;

public class AppResponse<T> implements Serializable {
    /**
     * 요청 반환코드
     */
    private String code;
    /**
     * 요청 반환데이터
     */
    private T data;
    /**
     * 설명정보
     */
    private String message;

    private AppResponse() {}

    /**
     * 반환오류정보
     *
     * @param codeEnum 오류정보코드
     * @return AppResponse
     */
    public static <T> AppResponse<T> error(ErrorCodeEnum codeEnum) {
        return error(codeEnum, null);
    }

    /**
     * 반환오류정보
     *
     * @param codeEnum 오류정보코드
     * @param message  오류정보
     * @return AppResponse
     */
    public static <T> AppResponse<T> error(ErrorCodeEnum codeEnum, String message) {
        if (StringUtils.isEmpty(message)) {
            message = codeEnum.getFlag();
        }

        AppResponse<T> response = new AppResponse<>();
        response.setCode(codeEnum.getCode());
        response.setData(null);
        response.setMessage(message);
        return response;
    }

    /**
     * 반환오류정보
     *
     * @param code    오류정보코드
     * @param message 오류정보
     * @return AppResponse
     */
    public static AppResponse<String> error(String code, String message) {
        AppResponse<String> response = new AppResponse<>();
        response.setCode(code);
        response.setData("");
        response.setMessage(message);
        return response;
    }

    /**
     * 반환오류정보
     *
     * @param message 오류정보
     * @return AppResponse
     */
    public static AppResponse<String> error(String message) {
        return error(ErrorCodeEnum.E_COMMON.getCode(), message);
    }

    /**
     * 성공요청 정보
     *
     * @param data 요청 데이터
     * @return AppResponse
     */
    public static <T> AppResponse<T> success(T data) {
        AppResponse<T> response = new AppResponse<>();
        response.setCode(ErrorCodeEnum.S_SUCCESS.getCode());
        response.setData(data);
        response.setMessage("");
        return response;
    }

    /**
     * 요청 여부성공
     *
     * @return boolean
     */
    public boolean ok() {
        return this.code.equals(ErrorCodeEnum.S_SUCCESS.getCode());
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}