package com.iflytek.rpa.base.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.base.entity.CSmartComponent;
import com.iflytek.rpa.robot.entity.RobotDesign;
import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.robot.entity.dto.RobotVersionDto;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CSmartComponentDao extends BaseMapper<CSmartComponent> {

    /**
     * 삽입새의가능컴포넌트기록(생성사람정보)
     *
     * @param component 가능컴포넌트
     * @return 의행데이터
     */
    @Insert(
            "INSERT INTO c_smart_version (smart_id, smart_type, robot_id, robot_version, content, creator_id, updater_id) "
                    + "VALUES (#{smartId}, #{smartType}, #{robotId}, #{robotVersion}, #{content}, #{creatorId}, #{updaterId})")
    int insert(CSmartComponent component);

    /**
     * 업데이트가능컴포넌트내용(업데이트사람정보, 트리거update_time업데이트)
     *
     * @param component 가능컴포넌트
     * @return 의행데이터: 1-업데이트성공, 0-찾을 수 없는 기호합치기파일의기록또는내용미완료변수변경
     */
    @Update("UPDATE c_smart_version " + "SET content = #{content}, updater_id = #{updaterId} "
            + "WHERE robot_id = #{robotId} AND smart_id = #{smartId} AND robot_version = 0 AND deleted = 0")
    int updateContent(CSmartComponent component);

    /**
     * 근거robotId및smartId조회삭제되지 않음의기록
     */
    @Select(
            "SELECT *" + "FROM c_smart_version "
                    + "WHERE smart_id = #{smartId} AND robot_id = #{robotId} AND robot_version = #{robotVersion} AND deleted = 0")
    CSmartComponent getBySmartId(
            @Param("smartId") String smartId,
            @Param("robotId") String robotId,
            @Param("robotVersion") Integer robotVersion);

    /**
     * 삭제가능컴포넌트(업데이트사람정보)
     *
     * @param robotId   봇ID
     * @param smartId  가능컴포넌트ID
     * @param updaterId 업데이트사람ID
     * @return 의행데이터
     */
    @Update("UPDATE c_smart_version " + "SET deleted = 1, updater_id = #{updaterId} "
            + "WHERE robot_id = #{robotId} AND smart_id = #{smartId} AND deleted = 0")
    int delete(
            @Param("robotId") String robotId, @Param("smartId") String smartId, @Param("updaterId") String updaterId);

    /**
     * 가져오기마켓위의프로세스시, 가능컴포넌트데이터
     */
    Integer createSmartComponentForObtainedVersion(
            @Param("obtainedRobotDesign") RobotDesign obtainedRobotDesign,
            @Param("authorRobotVersion") RobotVersion authorRobotVersion);

    @Select("select * " + "from c_smart_version "
            + "where deleted = 0 "
            + "and creator_id = #{userId} and robot_id = #{robotId} and robot_version = #{robotVersion} "
            + "order by create_time desc")
    List<CSmartComponent> getAllSmartComponentList(
            @Param("robotId") String robotId,
            @Param("robotVersion") Integer robotVersion,
            @Param("userId") String userId);

    Integer createSmartComponentForCurrentVersion(RobotVersionDto robotVersionDto);

    /**
     * 량삽입가능컴포넌트
     */
    void insertBatch(List<CSmartComponent> smartComponentList);

    /**
     * 삭제의버전(버전로0)의가능컴포넌트데이터
     * 사용버전돌아가기시빈현재데이터
     */
    @Update("update c_smart_version " + "set deleted = 1 "
            + "where robot_id = #{robotId} and robot_version = 0 and creator_id = #{userId}")
    boolean deleteOldEditVersion(@Param("robotId") String robotId, @Param("userId") String userId);
}