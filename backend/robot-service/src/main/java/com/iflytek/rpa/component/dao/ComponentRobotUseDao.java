package com.iflytek.rpa.component.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.component.entity.ComponentRobotUse;
import com.iflytek.rpa.component.entity.bo.ComponentRobotUseDeleteBo;
import com.iflytek.rpa.component.entity.bo.ComponentRobotUseUpdateBo;
import com.iflytek.rpa.component.entity.vo.CompUseInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 봇컴포넌트사용테이블(ComponentRobotUse)테이블데이터베이스방문
 *
 * @author makejava
 * @since 2024-12-19
 */
@Mapper
public interface ComponentRobotUseDao extends BaseMapper<ComponentRobotUse> {

    /**
     * 근거봇ID조회컴포넌트사용목록
     */
    List<ComponentRobotUse> getByRobotId(@Param("robotId") String robotId, @Param("tenantId") String tenantId);

    /**
     * 근거컴포넌트ID조회봇사용목록
     */
    List<ComponentRobotUse> getByComponentId(
            @Param("componentId") String componentId, @Param("tenantId") String tenantId);

    /**
     * 근거봇ID및버전조회컴포넌트사용
     */
    @Select("select * from component_robot_use " + "where deleted = 0 and robot_id = #{robotId} "
            + "and robot_version = #{robotVersion} and tenant_id = #{tenantId}")
    List<ComponentRobotUse> getByRobotIdAndVersion(
            @Param("robotId") String robotId,
            @Param("robotVersion") Integer robotVersion,
            @Param("tenantId") String tenantId);
    /**
     * 근거봇ID및버전조회컴포넌트사용, 으로사용의버전
     */
    List<CompUseInfo> getCompUseInfoList(
            @Param("robotId") String robotId,
            @Param("robotVersion") Integer robotVersion,
            @Param("tenantId") String tenantId);

    /**
     * 근거봇ID, 버전및컴포넌트ID조회컴포넌트사용
     */
    @Select("select * from component_robot_use where deleted = 0 and robot_id = #{robotId} "
            + "and robot_version = #{robotVersion} and component_id = #{componentId} and creator_id = #{userId}")
    ComponentRobotUse getByRobotIdVersionAndComponentId(
            @Param("robotId") String robotId,
            @Param("robotVersion") Integer robotVersion,
            @Param("componentId") String componentId,
            @Param("userId") String userId);

    /**
     * 근거컴포넌트ID및버전조회봇사용
     */
    @Select("select * from component_robot_use where deleted = 0 and component_id = #{componentId} "
            + "and component_version = #{componentVersion} and tenant_id = #{tenantId}")
    List<ComponentRobotUse> getByComponentIdAndVersion(
            @Param("componentId") String componentId,
            @Param("componentVersion") Integer componentVersion,
            @Param("tenantId") String tenantId);

    /**
     * 근거봇ID, 버전, 컴포넌트ID및컴포넌트버전조회컴포넌트사용
     */
    @Select("select * from component_robot_use where deleted = 0 and robot_id = #{robotId} "
            + "and robot_version = #{robotVersion} and component_id = #{componentId} "
            + "and component_version = #{componentVersion} and tenant_id = #{tenantId}")
    ComponentRobotUse getByRobotIdVersionAndComponentIdVersion(
            @Param("robotId") String robotId,
            @Param("robotVersion") Integer robotVersion,
            @Param("componentId") String componentId,
            @Param("componentVersion") Integer componentVersion,
            @Param("tenantId") String tenantId);

    /**
     * 삭제컴포넌트사용(삭제)
     */
    int deleteComponentUse(ComponentRobotUseDeleteBo deleteBo);

    /**
     * 업데이트컴포넌트사용버전
     */
    int updateComponentUse(ComponentRobotUseUpdateBo updateBo);

    /**
     * 량삽입컴포넌트사용기록
     */
    int insertBatch(@Param("entities") List<ComponentRobotUse> entities);

    /**
     * 삭제전의기록
     */
    @Update("update component_robot_use " + "set deleted = 1 "
            + "where robot_id = #{robotId} and robot_version = 0 and creator_id = #{userId}")
    boolean deleteOldEditVersion(@Param("robotId") String robotId, @Param("userId") String userId);

    /**
     * 조회지정버전의컴포넌트사용기록
     */
    @Select("select * from component_robot_use "
            + "where robot_id = #{robotId} and robot_version = #{version} and creator_id = #{userId} and deleted = 0")
    List<ComponentRobotUse> getComponentRobotUse(
            @Param("robotId") String robotId, @Param("version") Integer version, @Param("userId") String userId);
}