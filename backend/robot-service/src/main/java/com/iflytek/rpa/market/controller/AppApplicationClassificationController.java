package com.iflytek.rpa.market.controller;

import com.iflytek.rpa.market.entity.vo.AppMarketClassificationVo;
import com.iflytek.rpa.market.service.AppMarketClassificationService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 앱 마켓-분유형
 *
 * @author auto-generated
 */
@RestController
@RequestMapping("/classification")
public class AppApplicationClassificationController {

    @Autowired
    private AppMarketClassificationService appMarketClassificationService;

    /**
     * 클라이언트-가져오기분유형목록
     *
     * @return 분유형목록(id및name)
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    @GetMapping("/list")
    public AppResponse<List<AppMarketClassificationVo>> getClassificationList() throws NoLoginException {
        return appMarketClassificationService.getClassificationList();
    }
}