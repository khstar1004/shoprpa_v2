package com.iflytek.rpa.quota.service;

/**
 * 매칭금액수조회서비스연결
 * 의현재수조회(저장)
 */
public interface QuotaCountService {

    /**
     * 가져오기 계획기기수(현재테넌트아래사용자생성의봇수)
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @return 계획기기수
     */
    Integer getDesignerCount(String tenantId, String userId);

    /**
     * 가져오기마켓추가입력수(사용자완료추가입력의마켓수)
     *
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @return 완료추가입력의마켓수
     */
    Integer getMarketJoinCount(String tenantId, String userId);
}