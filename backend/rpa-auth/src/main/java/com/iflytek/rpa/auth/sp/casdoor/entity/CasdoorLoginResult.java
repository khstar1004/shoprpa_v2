package com.iflytek.rpa.auth.sp.casdoor.entity;

import lombok.Data;

/**
 * Casdoor 로그인결과
 */
@Data
public class CasdoorLoginResult {
    /**
     * 사용자ID
     */
    private String userId;

    /**
     * Casdoor Session ID(에서 Set-Cookie 중가져오기의 casdoor_session_id 값)
     */
    private String session;
}