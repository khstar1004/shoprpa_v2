package com.iflytek.rpa.component.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.component.entity.ComponentRobotBlock;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 봇컴포넌트테이블(ComponentRobotBlock)테이블데이터베이스방문
 *
 * @author makejava
 * @since 2024-12-19
 */
@Mapper
public interface ComponentRobotBlockDao extends BaseMapper<ComponentRobotBlock> {

    /**
     * 근거봇ID및컴포넌트ID조회기록
     */
    ComponentRobotBlock getBlockByRobotAndComponent(
            @Param("robotId") String robotId,
            @Param("componentId") String componentId,
            @Param("tenantId") String tenantId);

    /**
     * 근거봇ID조회모든의컴포넌트
     */
    List<ComponentRobotBlock> getBlocksByRobotId(@Param("robotId") String robotId, @Param("tenantId") String tenantId);

    /**
     * 근거컴포넌트ID조회모든해당컴포넌트의봇
     */
    List<ComponentRobotBlock> getBlocksByComponentId(
            @Param("componentId") String componentId, @Param("tenantId") String tenantId);

    /**
     * 조회봇여부완료지정컴포넌트
     */
    @Select("select count(*) from component_robot_block "
            + "where robot_id = #{robotId} and component_id = #{componentId} and robot_version = #{robotVersion} "
            + "and deleted = 0 and creator_id = #{userId}")
    Long checkBlockExists(
            @Param("robotId") String robotId,
            @Param("robotVersion") Integer robotVersion,
            @Param("componentId") String componentId,
            @Param("userId") String userId);

    /**
     * 삭제기록
     */
    @Update("update component_robot_block " + "set deleted = 1, updater_id = #{updaterId}, update_time = now() "
            + "where id = #{id} and tenant_id = #{tenantId}")
    Integer deleteBlock(@Param("id") Long id, @Param("updaterId") String updaterId, @Param("tenantId") String tenantId);

    /**
     * 근거봇ID및컴포넌트ID삭제기록
     */
    @Update("update component_robot_block " + "set deleted = 1, updater_id = #{updaterId}, update_time = now() "
            + "where robot_id = #{robotId} and component_id = #{componentId} and robot_version = #{robotVersion} "
            + "and deleted = 0 and tenant_id = #{tenantId}")
    Integer deleteBlockByRobotAndComponent(
            @Param("robotId") String robotId,
            @Param("robotVersion") Integer robotVersion,
            @Param("componentId") String componentId,
            @Param("updaterId") String updaterId,
            @Param("tenantId") String tenantId);

    /**
     * 근거봇ID및버전조회의컴포넌트ID목록
     */
    @Select("select component_id from component_robot_block "
            + "where robot_id = #{robotId} and robot_version = #{robotVersion} "
            + "and deleted = 0 and tenant_id = #{tenantId}")
    List<String> getBlockedComponentIds(
            @Param("robotId") String robotId,
            @Param("robotVersion") Integer robotVersion,
            @Param("tenantId") String tenantId);

    /**
     * 량삽입컴포넌트기록
     */
    int insertBatch(@Param("entities") List<ComponentRobotBlock> entities);

    /**
     * 삭제전의기록
     */
    @Update("update component_robot_block " + "set deleted = 1 "
            + "where robot_id = #{robotId} and robot_version = 0 and creator_id = #{userId}")
    boolean deleteOldEditVersion(@Param("robotId") String robotId, @Param("userId") String userId);

    /**
     * 조회지정버전의컴포넌트기록
     */
    @Select("select * from component_robot_block "
            + "where robot_id = #{robotId} and robot_version = #{version} and creator_id = #{userId} and deleted = 0")
    List<ComponentRobotBlock> getComponentRobotBlock(
            @Param("robotId") String robotId, @Param("version") Integer version, @Param("userId") String userId);

    /**
     * 조회지정버전의컴포넌트기록(사용복사, 사용tenantId)
     */
    @Select("select * from component_robot_block "
            + "where robot_id = #{robotId} and robot_version = #{version} and tenant_id = #{tenantId} and deleted = 0")
    List<ComponentRobotBlock> getComponentRobotBlockForCopy(
            @Param("robotId") String robotId, @Param("version") Integer version, @Param("tenantId") String tenantId);
}