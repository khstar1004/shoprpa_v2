package com.iflytek.rpa.auth.blacklist.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.auth.blacklist.entity.UserBlacklist;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 사용자이름단일 Mapper 연결
 *
 * @author system
 * @date 2025-12-16
 */
@Mapper
public interface UserBlacklistDao extends BaseMapper<UserBlacklist> {

    /**
     * 조회사용자현재의기록
     *
     * @param userId 사용자ID
     * @return 기록
     */
    UserBlacklist findActiveBlacklist(@Param("userId") String userId);

    /**
     * 조회완료경과의기록
     *
     * @param now 현재시간
     * @param limit 제한제어수
     * @return 경과의기록목록
     */
    List<UserBlacklist> findExpiredBlacklist(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 해제사용자(업데이트상태로완료해제)
     *
     * @param id 기록ID
     * @return 행데이터
     */
    int unban(@Param("id") Long id);

    /**
     * 조회사용자의기록
     *
     * @param userId 사용자ID
     * @return 목록
     */
    List<UserBlacklist> findHistoryByUserId(@Param("userId") String userId);
}