package com.iflytek.rpa.robot.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.robot.entity.SharedSubVar;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 공유 변수변수DAO
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Mapper
public interface SharedSubVarDao extends BaseMapper<SharedSubVar> {

    /**
     * 량삽입변수
     *
     * @param entities 변수목록
     * @return 행데이터
     */
    Integer insertBatch(@Param("entities") List<SharedSubVar> entities);
}