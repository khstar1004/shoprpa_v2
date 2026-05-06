package com.iflytek.rpa.auth.sp.casdoor.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iflytek.rpa.auth.core.entity.GetMarketUserListDto;
import com.iflytek.rpa.auth.core.entity.MarketDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 마켓사용자데이터방문연결(방문RPA서비스데이터베이스)
 * 비고: 연결의방법법방문rpa데이터베이스의app_market_user테이블
 *
 * @author Auto Generated
 * @create 2025/12/11
 */
@Mapper
public interface MarketUserDao {

    /**
     * 가져오기마켓사용자목록(분)- 조회rpa데이터베이스의app_market_user테이블
     *
     * @param page 분객체
     * @param dto 조회파일
     * @return 마켓사용자분목록(패키지마켓사용자본정보, 아니요패키지사용자정보)
     */
    IPage<MarketDto> getMarketUserListFromRpa(
            IPage<MarketDto> page, @Param("dto") GetMarketUserListDto dto);

    /**
     * 가져오기마켓아래완료저장에서의사용자ID목록
     *
     * @param marketId 마켓ID
     * @return 사용자ID목록
     */
    List<String> getExistingUserIdsByMarketId(@Param("marketId") String marketId);
}
