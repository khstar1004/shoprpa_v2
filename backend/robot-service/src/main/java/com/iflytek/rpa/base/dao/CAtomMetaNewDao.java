package com.iflytek.rpa.base.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.rpa.base.entity.CAtomMetaNew;
import com.iflytek.rpa.base.entity.vo.CAtomMetaNewVo;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 새기존가능DAO
 */
@Mapper
public interface CAtomMetaNewDao extends BaseMapper<CAtomMetaNew> {

    /**
     * 근거atomKey가져오기기존가능내용
     */
    String getAtomContentByKey(@Param("atomKey") String atomKey);

    /**
     * 근거key목록조회기존가능
     */
    List<CAtomMetaNewVo> getListByKeys(@Param("keys") List<String> keys);

    /**
     * 가져오기전체기존가능
     */
    List<CAtomMetaNewVo> getAll();

    /**
     * 근거atomKey삭제기록
     */
    int deleteByAtomKey(@Param("atomKey") String atomKey);

    /**
     * 근거atomKey업데이트기록
     */
    int updateByAtomKey(
            @Param("atomKey") String atomKey, @Param("atomContent") String atomContent, @Param("sort") Integer sort);

    List<CAtomMetaNew> getAtomListByKeySet(Set<String> atomKeySet);
}