package com.iflytek.rpa.auth.blacklist.service;

/**
 * 비밀번호오류계획데이터서비스
 *
 * @author system
 * @date 2025-12-16
 */
public interface PasswordErrorService {

    /**
     * 기록비밀번호오류
     * 결과가까지값, 출력 ShouldBeBlackException
     *
     * @param userId 사용자ID
     * @param username 사용자명
     * @return 현재오류데이터
     */
    int recordPasswordError(String userId, String username);

    /**
     * 지우기비밀번호오류 기록
     * 로그인성공후호출
     *
     * @param userId 사용자ID
     */
    void clearPasswordError(String userId);

    /**
     * 가져오기현재비밀번호오류데이터
     *
     * @param userId 사용자ID
     * @return 오류데이터
     */
    int getPasswordErrorCount(String userId);
}