package com.iflytek.rpa.auth.blacklist.service;

import com.iflytek.rpa.auth.blacklist.dto.BlacklistCacheDto;
import com.iflytek.rpa.auth.blacklist.entity.UserBlacklist;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 이름단일서비스연결
 *
 * @author system
 * @date 2025-12-16
 */
public interface BlackListService {

    /**
     * 추가사용자까지이름단일
     * 결과가사용자완료에서이름단일중, 이면업그레이드대기단계
     *
     * @param userId 사용자ID
     * @param username 사용자명
     * @param reason 원인
     * @param operator 사람
     * @return 이름단일기록
     */
    UserBlacklist add(String userId, String username, String reason, String operator);

    /**
     * 조회사용자여부
     * 에서 Redis 조회, miss 시조회 DB 돌아가기
     *
     * @param userId 사용자ID
     * @return 결과가반환정보, 아니요이면반환 null
     */
    BlacklistCacheDto isBlocked(String userId);

    /**
     * 해제사용자(결과가완료경과)
     *
     * @param userId 사용자ID
     */
    void unbanIfExpired(String userId);

    /**
     * 해제사용자
     *
     * @param userId 사용자ID
     * @param operator 사람
     * @return 여부성공
     */
    boolean unban(String userId, String operator);

    /**
     * 조회사용자의
     *
     * @param userId 사용자ID
     * @return 목록
     */
    List<UserBlacklist> getHistory(String userId);

    /**
     * 예약작업: 량해제완료경과의사용자
     *
     * @return 해제수
     */
    int batchUnbanExpired();

    /**
     * 강함제어비고판매사용자
     * 에서 request 중지우기정보, 호출 UapUserInfoAPI.logout 비고판매
     *
     * @param request HTTP 요청 
     * @param response HTTP 
     */
    void forceLogout(HttpServletRequest request, HttpServletResponse response);
}