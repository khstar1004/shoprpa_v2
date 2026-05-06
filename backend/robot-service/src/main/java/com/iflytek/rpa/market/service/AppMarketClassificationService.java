package com.iflytek.rpa.market.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iflytek.rpa.market.entity.dto.AppMarketClassificationEditDto;
import com.iflytek.rpa.market.entity.dto.AppMarketClassificationManageRequest;
import com.iflytek.rpa.market.entity.dto.AppMarketClassificationManageVo;
import com.iflytek.rpa.market.entity.vo.AppMarketClassificationVo;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;

/**
 * 앱 마켓분유형서비스연결
 *
 * @author auto-generated
 */
public interface AppMarketClassificationService {

    /**
     * 가져오기테넌트아래의분유형목록
     *
     * @return 분유형목록
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<List<AppMarketClassificationVo>> getClassificationList() throws NoLoginException;

    /**
     * 분유형관리관리-분유형조회
     *
     * @param request 조회요청 매개변수
     * @return 분유형목록(sort및생성 시간정렬)
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<List<AppMarketClassificationManageVo>> getClassificationManageList(
            AppMarketClassificationManageRequest request) throws NoLoginException, JsonProcessingException;

    /**
     * 분유형관리관리-추가분유형
     *
     * @param request 추가요청 매개변수
     * @return 결과
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<String> addClassification(AppMarketClassificationEditDto request) throws NoLoginException;

    /**
     * 분유형관리관리-수정분유형
     *
     * @param request 수정요청 매개변수
     * @return 결과
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<String> editClassification(AppMarketClassificationEditDto request) throws NoLoginException;

    /**
     * 분유형관리관리-삭제분유형
     *
     * @param request 삭제요청 매개변수
     * @return 결과
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<String> deleteClassification(AppMarketClassificationEditDto request) throws NoLoginException;
}