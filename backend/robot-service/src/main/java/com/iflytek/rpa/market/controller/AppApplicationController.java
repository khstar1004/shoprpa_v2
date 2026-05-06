package com.iflytek.rpa.market.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iflytek.rpa.market.entity.dto.*;
import com.iflytek.rpa.market.entity.vo.MyApplicationPageListVo;
import com.iflytek.rpa.market.service.AppApplicationService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 위, 사용신청관리관리
 */
@RestController
@RequestMapping("/application")
public class AppApplicationController {

    @Autowired
    private AppApplicationService appApplicationService;

    /**
     * 조회검토열기닫기상태
     */
    @GetMapping("/get-audit-status")
    public AppResponse<String> getAuditStatus() throws NoLoginException {
        return appApplicationService.getAuditStatus();
    }

    /**
     * 클라이언트-의신청목록
     *
     * @param queryDto
     * @return
     * @throws Exception
     */
    @PostMapping("/my-application-page-list")
    public AppResponse<IPage<MyApplicationPageListVo>> getMyApplicationPageList(
            @RequestBody MyApplicationPageListDto queryDto) throws Exception {
        return appApplicationService.getMyApplicationPageList(queryDto);
    }

    /**
     * 클라이언트-판매 의신청
     *
     * @param dto
     * @return
     * @throws Exception
     */
    @PostMapping("/my-application-cancel")
    public AppResponse<String> cancelMyApplication(@RequestBody MyApplicationDto dto) throws Exception {
        return appApplicationService.cancelMyApplication(dto);
    }

    /**
     * 클라이언트-삭제 의신청
     *
     * @param dto
     * @return
     * @throws Exception
     */
    @PostMapping("/my-application-delete")
    public AppResponse<String> deleteMyApplication(@RequestBody MyApplicationDto dto) throws Exception {
        return appApplicationService.deleteMyApplication(dto);
    }

    /**
     * 클라이언트-제출위신청전, 조회현재버전봇여부필요위검토
     *
     * @param dto
     * @return
     * @throws Exception
     */
    @PostMapping("/pre-release-check")
    public AppResponse<Integer> preReleaseCheck(@Valid @RequestBody PreReleaseCheckDto dto) throws Exception {
        return appApplicationService.preReleaseCheck(dto);
    }

    /**
     * 클라이언트-제출위신청
     */
    @PostMapping("/submit-release-application")
    public AppResponse<String> submitReleaseApplication(@Valid @RequestBody ReleaseApplicationDto applicationDto)
            throws Exception {
        if (CollectionUtils.isEmpty(applicationDto.getMarketIdList())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "마켓id비워 둘 수 없습니다");
        }
        return appApplicationService.submitReleaseApplication(applicationDto);
    }

    /**
     * 클라이언트-발송버전후, 제출위신청전, 조회여부필요위검토
     *
     * @param dto
     * @return
     * @throws Exception
     */
    @PostMapping("/pre-submit-after-publish-check")
    public AppResponse<?> preSubmitAfterPublishCheck(@Valid @RequestBody PreReleaseCheckDto dto) throws Exception {
        return appApplicationService.preSubmitAfterPublishCheck(dto);
    }

    /**
     * 클라이언트-발송버전후, 제출위신청
     *
     * @param dto
     * @return
     * @throws Exception
     */
    @PostMapping("/submit-after-publish")
    public AppResponse<String> submitAfterPublish(@Valid @RequestBody SubmitAfterPublishDto dto) throws Exception {
        if (CollectionUtils.isEmpty(dto.getMarketIdList())) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "마켓id비워 둘 수 없습니다");
        }
        return appApplicationService.submitAfterPublish(dto);
    }

    /**
     * 클라이언트-제출사용신청
     *
     * @param dto
     * @return
     * @throws Exception
     */
    @PostMapping("/submit-use-application")
    public AppResponse<String> submitUseApplication(@Valid @RequestBody UsePermissionCheckDto dto) throws Exception {
        return appApplicationService.submitUseApplication(dto);
    }

    /**
     * 클라이언트-사용전권한조회
     * @param dto
     * @return
     * @throws Exception
     */
    @PostMapping("/use-permission-check")
    public AppResponse<Integer> usePermissionCheck(@Valid @RequestBody UsePermissionCheckDto dto) throws Exception {
        return appApplicationService.usePermissionCheck(dto);
    }
}