package com.iflytek.rpa.component.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.rpa.component.entity.Component;
import com.iflytek.rpa.component.entity.vo.ComponentVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 컴포넌트테이블(Component)테이블데이터베이스방문
 *
 * @author makejava
 * @since 2024-12-19
 */
@Mapper
public interface ComponentDao extends BaseMapper<Component> {

    /**
     * 근거컴포넌트ID조회컴포넌트정보
     */
    Component getComponentById(
            @Param("componentId") String componentId,
            @Param("userId") String userId,
            @Param("tenantId") String tenantId);

    Component getShownComponentById(
            @Param("componentId") String componentId,
            @Param("userId") String userId,
            @Param("tenantId") String tenantId);

    /**
     * 근거이름조회컴포넌트수
     */
    Long countByName(
            @Param("name") String name,
            @Param("tenantId") String tenantId,
            @Param("userId") String userId,
            @Param("excludeId") Long excludeId);

    /**
     * 근거사용ID조회컴포넌트
     */
    @Select("select * from component where deleted = 0 and market_id = #{marketId} "
            + "and tenant_id = #{tenantId} and app_id = #{appId} "
            + "order by app_version desc limit 1")
    Component getComponentByAppId(
            @Param("userId") Long userId,
            @Param("tenantId") Long tenantId,
            @Param("marketId") String marketId,
            @Param("appId") String appId);

    /**
     * 근거사용ID목록조회컴포넌트
     */
    List<Component> getComponentsByAppIdList(
            @Param("userId") Long userId,
            @Param("tenantId") Long tenantId,
            @Param("marketId") String marketId,
            @Param("appIdList") List<String> appIdList);

    /**
     * 삭제컴포넌트
     */
    @Update("update component " + "set is_shown = 0, "
            + "update_time = now() "
            + "where component_id = #{componentId} "
            + "and tenant_id = #{tenantId}")
    Integer deleteComponent(
            @Param("componentId") String componentId,
            @Param("userId") String userId,
            @Param("tenantId") String tenantId);

    /**
     * 근거테넌트ID및사용자ID조회컴포넌트목록
     */
    List<Component> getComponentsByTenantAndUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    List<String> getComponentNameList(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId,
            @Param("componentNameBase") String componentNameBase);

    /**
     * 분조회컴포넌트목록
     * @param page 분매개변수
     * @param name 컴포넌트이름(조회)
     * @param dataSource 데이터
     * @param sortType 정렬유형
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @return 분컴포넌트목록
     */
    IPage<ComponentVo> getComponentPageList(
            Page<ComponentVo> page,
            @Param("name") String name,
            @Param("dataSource") String dataSource,
            @Param("sortType") String sortType,
            @Param("tenantId") String tenantId,
            @Param("userId") String userId);

    /**
     * 가져오기사용자권한내부가능가져오기의컴포넌트목록(shown = 1)
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @return 컴포넌트목록
     */
    List<Component> getAvailableComponentsByUser(@Param("tenantId") String tenantId, @Param("userId") String userId);

    /**
     * 근거컴포넌트ID목록조회컴포넌트정보
     * @param componentIds 컴포넌트ID목록
     * @param tenantId 테넌트ID
     * @return 컴포넌트목록
     */
    List<Component> getComponentsByIds(
            @Param("componentIds") List<String> componentIds, @Param("tenantId") String tenantId);

    Integer updateTransformStatus(
            @Param("userId") String userId,
            @Param("componentId") String componentId,
            @Param("name") String name,
            @Param("transformStatus") String transformStatus);
}