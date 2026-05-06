package com.iflytek.rpa.robot.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.robot.entity.SharedVarUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 공유 변수사용자닫기시스템DAO
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Mapper
public interface SharedVarUserDao extends BaseMapper<SharedVarUser> {

    /**
     * 량삽입사용자닫기시스템
     *
     * @param entities 사용자닫기시스템목록
     * @return 행데이터
     */
    Integer insertBatch(@Param("entities") List<SharedVarUser> entities);

    List<String> getAvailableSharedVarIds(String userId);
}