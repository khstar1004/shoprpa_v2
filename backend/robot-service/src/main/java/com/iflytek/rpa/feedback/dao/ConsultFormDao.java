package com.iflytek.rpa.feedback.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.feedback.entity.ConsultForm;
import org.apache.ibatis.annotations.Mapper;

/**
 * 문의테이블단일DAO
 *
 * @author system
 * @since 2024-12-15
 */
@Mapper
public interface ConsultFormDao extends BaseMapper<ConsultForm> {}