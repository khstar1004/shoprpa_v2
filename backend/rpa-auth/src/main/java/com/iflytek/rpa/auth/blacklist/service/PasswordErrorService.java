package com.iflytek.rpa.auth.blacklist.service;

/**
 * 비밀번호 오류 횟수 관리 서비스
 *
 * @author system
 * @date 2025-12-16
 */
public interface PasswordErrorService {

    /**
     * 비밀번호 오류를 기록합니다.
     * 허용 횟수를 초과하면 ShouldBeBlackException을 발생시킵니다.
     *
     * @param userId 사용자ID
     * @param username 사용자명
     * @return 현재 오류 횟수
     */
    int recordPasswordError(String userId, String username);

    /**
     * 비밀번호 오류 기록을 삭제합니다.
     * 로그인 성공 후 호출합니다.
     *
     * @param userId 사용자ID
     */
    void clearPasswordError(String userId);

    /**
     * 현재 비밀번호 오류 횟수를 조회합니다.
     *
     * @param userId 사용자ID
     * @return 오류 횟수
     */
    int getPasswordErrorCount(String userId);
}
