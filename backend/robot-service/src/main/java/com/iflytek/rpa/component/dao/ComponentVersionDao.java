package com.iflytek.rpa.component.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.component.entity.ComponentVersion;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 컴포넌트버전테이블(ComponentVersion)테이블데이터베이스방문
 *
 * @author makejava
 * @since 2024-12-19
 */
@Mapper
public interface ComponentVersionDao extends BaseMapper<ComponentVersion> {

    /**
     * 근거컴포넌트ID조회버전목록
     */
    List<ComponentVersion> getVersionsByComponentId(
            @Param("componentId") String componentId, @Param("tenantId") String tenantId);

    /**
     * 근거컴포넌트ID및버전조회버전정보
     */
    ComponentVersion getVersionByComponentIdAndVersion(
            @Param("componentId") String componentId,
            @Param("version") Integer version,
            @Param("tenantId") String tenantId);

    /**
     * 가져오기컴포넌트의새버전
     */
    @Select("select max(version) from component_version "
            + "where component_id = #{componentId} and deleted = 0 and tenant_id = #{tenantId}")
    Integer getLatestVersion(@Param("componentId") String componentId, @Param("tenantId") String tenantId);

    /**
     * 가져오기컴포넌트의새버전정보
     */
    @Select("select * from component_version "
            + "where component_id = #{componentId} and deleted = 0 and tenant_id = #{tenantId} "
            + "order by version desc limit 1")
    ComponentVersion getLatestVersionInfo(@Param("componentId") String componentId, @Param("tenantId") String tenantId);

    /**
     * 업데이트버전사용상태
     */
    @Update("update component_version set online = #{online}, " + "updater_id = #{userId}, update_time = now() "
            + "where component_id = #{componentId} and deleted = 0 and tenant_id = #{tenantId}")
    Integer updateOnlineStatus(
            @Param("userId") String userId,
            @Param("componentId") String componentId,
            @Param("online") Integer online,
            @Param("tenantId") String tenantId);

    /**
     * 삭제버전
     */
    @Update("update component_version " + "set deleted = 1, update_time = now(), updater_id = #{userId} "
            + "where component_id = #{componentId} and deleted = 0 and tenant_id = #{tenantId}")
    Integer deleteVersion(
            @Param("componentId") String componentId,
            @Param("userId") String userId,
            @Param("tenantId") String tenantId);

    /**
     * 근거컴포넌트ID조회사용의버전
     */
    @Select("select * from component_version "
            + "where component_id = #{componentId} and online = 1 and deleted = 0 and tenant_id = #{tenantId}")
    ComponentVersion getOnlineVersion(@Param("componentId") String componentId, @Param("tenantId") String tenantId);

    /**
     * 가져오기완료발송경과버전의컴포넌트ID목록
     * @param tenantId 테넌트ID
     * @return 컴포넌트ID목록
     */
    @Select("select distinct component_id from component_version " + "where deleted = 0 and tenant_id = #{tenantId}")
    List<String> getPublishedComponentIds(@Param("tenantId") String tenantId);

    /**
     * 량가져오기컴포넌트의새버전정보
     * @param componentIds 컴포넌트ID목록
     * @param tenantId 테넌트ID
     * @return 컴포넌트새버전정보목록
     */
    List<ComponentVersion> getLatestVersionInfoBatch(
            @Param("componentIds") List<String> componentIds, @Param("tenantId") String tenantId);
}