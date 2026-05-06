package com.iflytek.rpa.robot.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.robot.entity.SharedVar;
import com.iflytek.rpa.robot.entity.vo.SharedSubVarVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 공유 변수DAO
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Mapper
public interface SharedVarDao extends BaseMapper<SharedVar> {

    /**
     * 근거공유 변수ID가져오기 변수목록
     *
     * @param sharedVarIds 공유 변수ID목록
     * @return 변수목록
     */
    List<SharedSubVarVo> getSubVarListBySharedVarIds(@Param("sharedVarIds") List<Long> sharedVarIds);

    /**
     * 조회사용자가능사용의공유 변수(usage_type='all'및dept_id매칭의)
     *
     * @param tenantId     테넌트ID
     * @param deptId       모듈ID
     * @param selectVarIds
     * @return 공유 변수목록
     */
    List<SharedVar> getAvailableSharedVars(
            @Param("tenantId") String tenantId,
            @Param("deptId") String deptId,
            @Param("selectVarIds") List<String> selectVarIds);

    List<SharedVar> getAvailableByIds(@Param("ids") List<Long> ids);
}