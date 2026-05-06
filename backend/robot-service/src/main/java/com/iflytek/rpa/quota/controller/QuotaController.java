package com.iflytek.rpa.quota.controller;

import com.iflytek.rpa.quota.service.QuotaCheckService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매칭금액검증제어기기
 * 매칭금액검증닫기의연결
 *
 * @author system
 * @since 2024-12-20
 */
@RestController
@RequestMapping("/quota")
public class QuotaController {

    @Autowired
    private QuotaCheckService quotaCheckService;

    /**
     * 검증계획기기매칭금액(designer_count)
     * 조회현재사용자의계획기기수여부초과제한
     *
     * @return true테이블미완료초과제한, false테이블완료초과제한
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    @GetMapping("/check-designer")
    public AppResponse<Boolean> checkDesignerQuota() throws NoLoginException {
        boolean result = quotaCheckService.checkDesignerQuota();
        return AppResponse.success(result);
    }

    /**
     * 검증마켓추가입력수매칭금액(market_join_count)
     * 조회현재사용자완료추가입력의마켓수여부초과제한
     *
     * @return true테이블미완료초과제한, false테이블완료초과제한
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    @GetMapping("/check-market-join")
    public AppResponse<Boolean> checkMarketJoinQuota() throws NoLoginException {
        boolean result = quotaCheckService.checkMarketJoinQuota();
        return AppResponse.success(result);
    }
}