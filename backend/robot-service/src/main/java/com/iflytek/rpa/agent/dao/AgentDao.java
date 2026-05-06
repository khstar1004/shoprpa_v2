package com.iflytek.rpa.agent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.agent.entity.Agent;
import java.util.List;
import org.apache.ibatis.annotations.*;

/**
 * Agent DAO연결
 */
@Mapper
public interface AgentDao extends BaseMapper<Agent> {

    /**
     * 삽입새의Agent기록(생성사람정보)
     *
     * @param agent Agent
     * @return 의행데이터
     */
    @Insert("INSERT INTO agent_table (agent_id, content, creator_id, updater_id) "
            + "VALUES (#{agentId}, #{content}, #{creatorId}, #{updaterId})")
    int insertAgent(Agent agent);

    /**
     * 업데이트Agent내용(업데이트사람정보, 트리거update_time업데이트)
     *
     * @param agent Agent
     * @return 의행데이터: 1-업데이트성공, 0-찾을 수 없는 기호합치기파일의기록또는내용미완료변수변경
     */
    @Update("UPDATE agent_table " + "SET content = #{content}, updater_id = #{updaterId} "
            + "WHERE agent_id = #{agentId} AND deleted = 0")
    int updateContent(Agent agent);

    /**
     * 근거agentId조회삭제되지 않음의기록
     *
     * @param agentId Agent ID
     * @return Agent, 찾을 수 없습니다이면반환null
     */
    @Select("SELECT * FROM agent_table " + "WHERE agent_id = #{agentId} AND deleted = 0")
    Agent getByAgentId(@Param("agentId") String agentId);

    /**
     * 조회모든삭제되지 않음의Agent기록
     *
     * @return Agent목록
     */
    @Select("SELECT * FROM agent_table WHERE deleted = 0 ORDER BY create_time DESC")
    List<Agent> listAllAgents();

    /**
     * 생성사람필터링조회삭제되지 않음의Agent기록
     *
     * @param userId 생성사람사용자ID
     * @return Agent목록
     */
    @Select("SELECT * FROM agent_table WHERE deleted = 0 AND creator_id = #{userId} ORDER BY create_time DESC")
    List<Agent> listAgentsByUserId(@Param("userId") String userId);

    /**
     * 삭제Agent(업데이트사람정보)
     *
     * @param agentId   Agent ID
     * @param updaterId 업데이트사람ID
     * @return 의행데이터
     */
    @Update("UPDATE agent_table " + "SET deleted = 1, updater_id = #{updaterId} "
            + "WHERE agent_id = #{agentId} AND deleted = 0")
    int deleteAgent(@Param("agentId") String agentId, @Param("updaterId") String updaterId);
}