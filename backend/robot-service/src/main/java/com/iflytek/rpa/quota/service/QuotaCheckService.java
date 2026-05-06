package com.iflytek.rpa.quota.service;

public interface QuotaCheckService {

    /**
     * 조회계획기기수매칭금액
     * @return true테이블미완료초과제한, false테이블완료초과제한
     */
    boolean checkDesignerQuota();

    /**
     * 조회마켓추가입력수매칭금액
     * @return true테이블미완료초과제한, false테이블완료초과제한
     */
    boolean checkMarketJoinQuota();
}