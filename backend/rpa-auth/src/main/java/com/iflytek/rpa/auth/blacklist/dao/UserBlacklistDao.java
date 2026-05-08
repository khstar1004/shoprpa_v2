package com.iflytek.rpa.auth.blacklist.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.auth.blacklist.entity.UserBlacklist;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 사용자 차단 목록 Mapper
 *
 * @author system
 * @date 2025-12-16
 */
@Mapper
public interface UserBlacklistDao extends BaseMapper<UserBlacklist> {

    /**
     * 현재 차단 중인 사용자 기록을 조회합니다.
     *
     * @param userId 사용자ID
     * @return 기록
     */
    UserBlacklist findActiveBlacklist(@Param("userId") String userId);

    /**
     * status=1 상태로 남아 있는 최신 차단 기록을 조회합니다.
     *
     * @param userId 사용자ID
     * @return 기록
     */
    UserBlacklist findLatestEnabledBlacklist(@Param("userId") String userId);

    /**
     * 만료된 차단 기록을 조회합니다.
     *
     * @param now 현재시간
     * @param limit 제한제어수
     * @return 경과의기록목록
     */
    List<UserBlacklist> findExpiredBlacklist(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 사용자의 차단 상태를 해제 처리합니다.
     *
     * @param id 기록ID
     * @return 행데이터
     */
    int unban(@Param("id") Long id);

    /**
     * 사용자의 차단 이력을 조회합니다.
     *
     * @param userId 사용자ID
     * @return 목록
     */
    List<UserBlacklist> findHistoryByUserId(@Param("userId") String userId);
}
