package com.iflytek.rpa.example.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.example.entity.SampleUsers;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 사용자에서시스템중비고입력의데이터(SampleUsers)테이블데이터베이스방문
 *
 * @author makejava
 * @since 2024-12-19
 */
@Mapper
public interface SampleUsersDao extends BaseMapper<SampleUsers> {

    /**
     * 량삽입
     *
     * @param entities 목록
     * @return 삽입행데이터
     */
    int insertBatch(@Param("entities") List<SampleUsers> entities);

    /**
     * 량업데이트
     *
     * @param entities 목록
     * @return 업데이트행데이터
     */
    int updateBatch(@Param("entities") List<SampleUsers> entities);

    /**
     * 근거사용자ID및ID조회
     *
     * @param creatorId 사용자ID
     * @param sampleId ID
     * @return 사용자
     */
    SampleUsers selectByCreatorIdAndSampleId(@Param("creatorId") String creatorId, @Param("sampleId") String sampleId);

    /**
     * 근거사용자ID조회모든
     *
     * @param creatorId 사용자ID
     * @return 사용자목록
     */
    List<SampleUsers> selectByCreatorId(@Param("creatorId") String creatorId);

    @Select(
            "select count(1) from sample_users where creator_id = #{creatorId} and tenant_id = #{tenantId} and version_injected = #{version}")
    Integer getExistSampleUsers(
            @Param("creatorId") String creatorId, @Param("tenantId") String tenantId, @Param("version") String version);
}