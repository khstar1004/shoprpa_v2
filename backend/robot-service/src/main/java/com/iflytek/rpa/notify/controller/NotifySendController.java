package com.iflytek.rpa.notify.controller;

import com.iflytek.rpa.base.annotation.NoApiLog;
import com.iflytek.rpa.notify.entity.dto.CreateNotifyDto;
import com.iflytek.rpa.notify.entity.dto.NotifyListDto;
import com.iflytek.rpa.notify.service.NotifySendService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify")
public class NotifySendController {
    @Resource
    private NotifySendService notifySendService;

    /**
     * 제품메시지
     *
     * @param createNotifyDto
     * @return 제품메시지여부성공
     */
    @PostMapping("/create-notify")
    public AppResponse<?> createNotify(@RequestBody CreateNotifyDto createNotifyDto) throws NoLoginException {
        return notifySendService.createNotify(createNotifyDto);
    }

    /**
     * 메시지목록
     *
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/notify-List")
    public AppResponse<?> notifyList(@RequestBody NotifyListDto notifyListDto) throws NoLoginException {
        return notifySendService.notifyList(notifyListDto);
    }

    @NoApiLog("문의연결-조회여부있음알림")
    @GetMapping("/hasNotify")
    public AppResponse<?> hasNotify() throws NoLoginException {
        return notifySendService.hasNotify();
    }

    /**
     * 일완료
     *
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/set-all-notify-read")
    public AppResponse<?> setAllNotifyRead() throws NoLoginException {
        return notifySendService.setAllNotifyRead();
    }

    /**
     * 완료지정메시지
     *
     * @param notifyId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/set-selected-notify-read")
    public AppResponse<?> setSelectedNotifyRead(@RequestParam("notifyId") Long notifyId) throws NoLoginException {
        return notifySendService.setSelectedNotifyRead(notifyId);
    }

    @GetMapping("/reject-join-team")
    public AppResponse<?> rejectJoinTeam(@RequestParam("notifyId") Long notifyId) throws NoLoginException {
        return notifySendService.rejectJoinTeam(notifyId);
    }

    @GetMapping("/accept-join-team")
    public AppResponse<?> acceptJoinTeam(@RequestParam("notifyId") Long notifyId) throws NoLoginException {
        return notifySendService.acceptJoinTeam(notifyId);
    }
}