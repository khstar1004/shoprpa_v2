package com.iflytek.rpa.market.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.market.entity.AppMarketClassification;
import com.iflytek.rpa.market.entity.dto.AppMarketClassificationManageVo;
import com.iflytek.rpa.market.entity.vo.AppMarketClassificationVo;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 앱 마켓분유형테이블(AppMarketClassification)데이터베이스방문
 *
 * @author auto-generated
 */
@Mapper
public interface AppMarketClassificationDao extends BaseMapper<AppMarketClassification> {

    /**
     * 근거테넌트ID조회분유형목록
     *
     * @param tenantId 테넌트ID
     * @return 분유형목록
     */
    List<AppMarketClassificationVo> getClassificationListByTenantId(@Param("tenantId") String tenantId);

    /**
     * 분유형관리관리-분유형조회
     *
     * @param tenantId 테넌트ID
     * @param name 분유형이름
     * @param source 
     * @return 분유형목록(sort및생성 시간정렬)
     */
    List<AppMarketClassificationManageVo> getClassificationManageList(
            @Param("tenantId") String tenantId, @Param("name") String name, @Param("source") Integer source);

    List<Map> getCategoryReferenceCount();

    Integer insertDefaultClassification(@Param("tenantId") String tenantId);
}