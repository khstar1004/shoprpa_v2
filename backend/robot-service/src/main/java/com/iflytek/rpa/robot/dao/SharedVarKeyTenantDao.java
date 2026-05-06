package com.iflytek.rpa.robot.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.robot.entity.SharedVarKeyTenant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 공유 변수테넌트키DAO
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Mapper
public interface SharedVarKeyTenantDao extends BaseMapper<SharedVarKeyTenant> {

    @Select("SELECT id from shared_var_key_tenant WHERE deleted = 0")
    List<String> getExistsTenantIds();
    /**
     * 량삽입테넌트키
     */
    void insertBatch(@Param("entities") List<SharedVarKeyTenant> entities);

    /**
     * 근거테넌트ID조회키
     *
     * @param tenantId 테넌트ID
     * @return 키
     */
    SharedVarKeyTenant selectByTenantId(@Param("tenantId") String tenantId);
}