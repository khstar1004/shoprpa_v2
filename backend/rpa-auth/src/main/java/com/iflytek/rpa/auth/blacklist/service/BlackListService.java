package com.iflytek.rpa.auth.blacklist.service;

import com.iflytek.rpa.auth.blacklist.dto.BlacklistCacheDto;
import com.iflytek.rpa.auth.blacklist.entity.UserBlacklist;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 사용자 차단 목록 서비스
 *
 * @author system
 * @date 2025-12-16
 */
public interface BlackListService {

    /**
     * 사용자를 차단 목록에 추가합니다.
     * 이미 차단 중이면 차단 단계와 기간을 갱신합니다.
     *
     * @param userId 사용자ID
     * @param username 사용자명
     * @param reason 차단 사유
     * @param operator 처리자
     * @return 차단 기록
     */
    UserBlacklist add(String userId, String username, String reason, String operator);

    /**
     * 사용자가 현재 차단 중인지 조회합니다.
     * Redis 캐시 miss 시 DB를 다시 확인합니다.
     *
     * @param userId 사용자ID
     * @return 차단 정보, 차단 중이 아니면 null
     */
    BlacklistCacheDto isBlocked(String userId);

    /**
     * 차단 기간이 만료된 사용자를 해제합니다.
     *
     * @param userId 사용자ID
     */
    void unbanIfExpired(String userId);

    /**
     * 사용자를 수동 해제합니다.
     *
     * @param userId 사용자ID
     * @param operator 처리자
     * @return 성공 여부
     */
    boolean unban(String userId, String operator);

    /**
     * 사용자의 차단 이력을 조회합니다.
     *
     * @param userId 사용자ID
     * @return 목록
     */
    List<UserBlacklist> getHistory(String userId);

    /**
     * 예약 작업: 만료된 사용자 차단을 일괄 해제합니다.
     *
     * @return 해제 수
     */
    int batchUnbanExpired();

    /**
     * 사용자 세션을 강제 로그아웃합니다.
     *
     * @param request HTTP 요청 
     * @param response HTTP 응답
     */
    void forceLogout(HttpServletRequest request, HttpServletResponse response);
}
