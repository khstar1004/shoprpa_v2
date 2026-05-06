package com.iflytek.rpa.market.service;

import com.iflytek.rpa.market.entity.dto.InviteLinkDto;
import com.iflytek.rpa.market.entity.vo.AcceptResultVo;
import com.iflytek.rpa.market.entity.vo.InviteInfoVo;
import com.iflytek.rpa.market.entity.vo.InviteLinkVo;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 팀마켓-SaaS서비스연결
 */
public interface AppMarketInviteService {

    /**
     * 완료초대연결
     *
     * @param inviteLinkDto 요청 매개변수
     * @return 초대연결
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<InviteLinkVo> generateInviteLink(InviteLinkDto inviteLinkDto) throws NoLoginException;

    /**
     * 재초대연결
     *
     * @param inviteLinkDto 요청 매개변수(필요marketId)
     * @return 초대연결
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<InviteLinkVo> resetInviteLink(InviteLinkDto inviteLinkDto) throws NoLoginException;

    /**
     * 근거초대key가져오기초대정보
     *
     * @param inviteKey 초대key
     * @return 초대정보 (패키지초대사람이름및팀이름)
     */
    AppResponse<InviteInfoVo> getInviteInfoByInviteKey(String inviteKey);

    /**
     * 연결초대
     *
     * @param inviteKey 초대key
     * @return 결과
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<AcceptResultVo> acceptInvite(String inviteKey) throws NoLoginException;
}