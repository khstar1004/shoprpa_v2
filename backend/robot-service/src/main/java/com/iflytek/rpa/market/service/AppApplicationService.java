package com.iflytek.rpa.market.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iflytek.rpa.market.entity.AppMarketResource;
import com.iflytek.rpa.market.entity.dto.*;
import com.iflytek.rpa.market.entity.vo.LatestVersionRobotVo;
import com.iflytek.rpa.market.entity.vo.MyApplicationPageListVo;
import com.iflytek.rpa.robot.entity.vo.ExecuteListVo;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.validation.Valid;

public interface AppApplicationService {
    /**
     * 조회현재테넌트의검토열기닫기상태
     *
     * @return 검토열기닫기상태
     * @throws NoLoginException
     */
    AppResponse<String> getAuditStatus() throws NoLoginException;

    List<LatestVersionRobotVo> getRobotListApplicationStatus(List<LatestVersionRobotVo> voList);

    AppResponse<IPage<MyApplicationPageListVo>> getMyApplicationPageList(MyApplicationPageListDto queryDto)
            throws NoLoginException;

    AppResponse<String> cancelMyApplication(MyApplicationDto dto) throws NoLoginException;

    AppResponse<String> deleteMyApplication(MyApplicationDto dto) throws NoLoginException;

    /**
     * 가져오기 비밀단계식별자 및 중지시간
     *
     * @param appResourceList
     * @param resVerDtoList
     */
    void packageApplicationInfo(List<AppMarketResource> appResourceList, List<ResVerDto> resVerDtoList, String userId);

    /**
     * 클라이언트 - 실행기기-사용권한검증
     *
     * @param ansRecords
     */
    void packageUsePermission(List<ExecuteListVo> ansRecords) throws NoLoginException;

    /**
     * 조회현재버전봇여부필요위검토
     */
    AppResponse<Integer> preReleaseCheck(PreReleaseCheckDto dto) throws Exception;

    /**
     * 클라이언트-제출위신청
     *
     * @param applicationDto 신청매개변수
     * @return 결과
     * @throws NoLoginException
     */
    AppResponse<String> submitReleaseApplication(ReleaseApplicationDto applicationDto) throws Exception;

    AppResponse<?> preSubmitAfterPublishCheck(@Valid PreReleaseCheckDto dto) throws NoLoginException;
    /**
     * 클라이언트-제출위신청후, 제출발송버전정보
     * @param dto
     * @return
     * @throws NoLoginException
     */
    AppResponse<String> submitAfterPublish(SubmitAfterPublishDto dto) throws Exception;

    /**
     * 클라이언트-추가사용신청
     *
     * @param dto
     * @return
     * @throws Exception
     */
    AppResponse<String> submitUseApplication(UsePermissionCheckDto dto) throws Exception;

    /**
     * 클라이언트사용권한조회
     *
     * @param dto
     * @return
     * @throws Exception
     */
    AppResponse<Integer> usePermissionCheck(UsePermissionCheckDto dto) throws Exception;
}