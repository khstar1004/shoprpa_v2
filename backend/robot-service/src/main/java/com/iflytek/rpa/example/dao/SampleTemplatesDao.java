package com.iflytek.rpa.example.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.example.entity.SampleTemplates;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 시스템지정의라이브러리(SampleTemplates)테이블데이터베이스방문
 *
 * @author makejava
 * @since 2024-12-19
 */
@Mapper
public interface SampleTemplatesDao extends BaseMapper<SampleTemplates> {

    /**
     * 량삽입
     *
     * @param entities 목록
     * @return 삽입행데이터
     */
    int insertBatch(@Param("entities") List<SampleTemplates> entities);

    /**
     * 량업데이트
     *
     * @param entities 목록
     * @return 업데이트행데이터
     */
    int updateBatch(@Param("entities") List<SampleTemplates> entities);

    /**
     * 근거ID조회
     *
     * @param sampleId ID
     * @return 
     */
    SampleTemplates selectBySampleId(@Param("sampleId") String sampleId);

    /**
     * 근거유형조회있음
     *
     * @param type 유형
     * @return 목록
     */
    List<SampleTemplates> selectActiveByType(@Param("type") String type);

    @Select("select version from sample_templates where is_deleted = 0 and is_active = 1")
    List<String> getVersionList();

    @Select("select * from sample_templates where is_deleted = 0 and is_active = 1 and version = #{version}")
    List<SampleTemplates> getSamples(@Param("version") String version);
}