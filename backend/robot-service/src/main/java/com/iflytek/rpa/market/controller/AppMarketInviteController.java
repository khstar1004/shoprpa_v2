package com.iflytek.rpa.market.controller;

import com.iflytek.rpa.market.entity.dto.InviteKeyDto;
import com.iflytek.rpa.market.entity.dto.InviteLinkDto;
import com.iflytek.rpa.market.entity.vo.AcceptResultVo;
import com.iflytek.rpa.market.entity.vo.InviteInfoVo;
import com.iflytek.rpa.market.entity.vo.InviteLinkVo;
import com.iflytek.rpa.market.service.AppMarketInviteService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팀마켓-연결초대
 */
@RestController
@RequestMapping("market-invite")
public class AppMarketInviteController {

    @Resource
    private AppMarketInviteService appMarketInviteService;

    /**
     * 완료/가져오기초대연결
     * 있음내부아니요완료새연결
     * @param inviteLinkDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/generate-invite-link")
    public AppResponse<InviteLinkVo> generateInviteLink(@RequestBody InviteLinkDto inviteLinkDto)
            throws NoLoginException {
        return appMarketInviteService.generateInviteLink(inviteLinkDto);
    }

    /**
     * 재초대연결
     *
     * @param inviteLinkDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/reset-invite-link")
    public AppResponse<InviteLinkVo> resetInviteLink(@RequestBody InviteLinkDto inviteLinkDto) throws NoLoginException {
        return appMarketInviteService.resetInviteLink(inviteLinkDto);
    }

    /**
     * 근거초대key가져오기초대정보
     *
     * @param inviteKeyDto
     * @return
     */
    @PostMapping("/get-invite-info-by-invite-key")
    public AppResponse<InviteInfoVo> getInviteInfoByInviteKey(@RequestBody InviteKeyDto inviteKeyDto) {
        return appMarketInviteService.getInviteInfoByInviteKey(inviteKeyDto.getInviteKey());
    }

    /**
     * 연결초대
     *
     * @param inviteKeyDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/accept-invite")
    public AppResponse<AcceptResultVo> acceptInvite(@RequestBody InviteKeyDto inviteKeyDto) throws NoLoginException {
        return appMarketInviteService.acceptInvite(inviteKeyDto.getInviteKey());
    }
}