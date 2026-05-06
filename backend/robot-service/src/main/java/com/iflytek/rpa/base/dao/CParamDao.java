package com.iflytek.rpa.base.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.base.entity.CParam;
import com.iflytek.rpa.robot.entity.RobotExecute;
import java.util.List;
import javax.validation.constraints.NotBlank;
import org.apache.ibatis.annotations.*;

/**
 * @author tzzhang
 * @date 2025/3/13 16:51
 */
@Mapper
public interface CParamDao extends BaseMapper<CParam> {

    List<CParam> getAllParams(
            @Param("processId") String processId, @Param("robotId") String robotId, @Param("version") Integer version);

    List<CParam> getAllParamsByModuleId(
            @Param("moduleId") String moduleId, @Param("robotId") String robotId, @Param("version") Integer version);

    List<CParam> getParams(@Param("robotId") String robotId, @Param("userId") String userId);

    void insertParamBatch(List<CParam> params);

    @Select(
            "select process_id from c_process where process_name = '프로세스' and robot_id=#{robotId} and robot_version=#{robotVersion} and deleted = 0")
    String getMianProcessId(String robotId, Integer robotVersion);

    @Insert(
            "insert into c_param(id,var_direction,var_name,var_type,var_value,var_describe,process_id,creator_id,updater_id,create_time,update_time,deleted,robot_id,robot_version,module_id) "
                    + "values"
                    + "(#{id},#{varDirection},#{varName},#{varType},#{varValue},#{varDescribe},#{processId},#{creatorId},#{updaterId},#{createTime},#{updateTime},#{deleted},#{robotId},#{robotVersion},#{moduleId})")
    void addParam(CParam cParam);

    // 삭제수정deleted아니요필요정상삭제
    @Update("update c_param set deleted=1 where id=#{id}")
    void deleteParam(String id);

    Long countParamByName(CParam param);

    void updateParam(CParam cParamDto);

    void createParamForCurrentVersion(@Param("entities") List<CParam> cParamList);

    /**
     * 조회생성봇매개변수
     *
     * @param robotId
     * @param processId
     * @param robotVersion
     * @return
     */
    List<CParam> getSelfRobotParam(String robotId, String processId, Integer robotVersion);

    /**
     * 조회기존봇의robot_id
     * @param robotExecute
     * @return
     */
    String getMarketRobotId(RobotExecute robotExecute);
    /**
     * 조회모듈의기존봇의robot_id
     * @param robotExecute
     * @return
     */
    String getDeployOriginalRobotId(RobotExecute robotExecute);

    /**
     * 조회에서봇버전
     * @param robotId
     * @return
     */
    Integer getRobotVersion(String robotId);

    /**
     * 근거robotId삭제매개변수
     * @param robotId
     */
    @Update("update c_param set deleted = 1 where robot_id =#{robotId} and robot_version = 0")
    void deleteParamByRobotId(String robotId);

    CParam getParamInfoById(@Param("id") String id);

    List<CParam> getParamsByModuleId(
            @NotBlank(message = "moduleId비워 둘 수 없습니다") String moduleId,
            @NotBlank(message = "robotId비워 둘 수 없습니다") String robotId,
            Integer robotVersion);

    List<CParam> getSelfRobotParamByModuleId(String robotId, String moduleId, Integer enabledVersion);
}