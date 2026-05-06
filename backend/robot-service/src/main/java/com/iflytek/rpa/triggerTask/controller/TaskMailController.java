package com.iflytek.rpa.triggerTask.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iflytek.rpa.triggerTask.entity.TaskMail;
import com.iflytek.rpa.triggerTask.entity.dto.EnableBo;
import com.iflytek.rpa.triggerTask.service.ITaskMailService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/taskMail")
public class TaskMailController {

    @Autowired
    private ITaskMailService taskMailService;

    /**
     * 메일함목록분
     * @param pageNo
     * @param pageSize
     * @param userId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/page/list")
    public AppResponse<IPage<TaskMail>> getTaskExecute(
            @RequestParam(value = "pageNo", defaultValue = "1") Long pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize,
            @RequestParam(value = "userId", required = false) String userId)
            throws NoLoginException {
        return AppResponse.success(taskMailService.getTaskMailPage(pageNo, pageSize, userId));
    }

    /**
     * 메일함연결시도 todo 시간, 예실행클라이언트, 후필요
     * @param mail
     * @return
     */
    @PostMapping("/connect")
    public Map<String, String> connectMail(@RequestBody TaskMail mail) {
        String msg = taskMailService.connectMail(mail);
        return new HashMap<String, String>() {
            {
                put("code", "000000");
                put("data", StringUtils.isEmpty(msg) ? "1" : "0");
                put("message", msg);
            }
        };
    }

    /**
     * 저장메일함연결
     * @param mail
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/save")
    public AppResponse<ErrorCodeEnum> saveMail(@RequestBody TaskMail mail) throws NoLoginException {
        taskMailService.saveMail(mail);
        return AppResponse.success(ErrorCodeEnum.S_SUCCESS);
    }

    /**
     * 삭제메일함연결
     * @param enableBo
     * @return
     */
    @PostMapping("/delete")
    public AppResponse<ErrorCodeEnum> deleteMail(@RequestBody EnableBo enableBo) {
        if (taskMailService.deleteMail(enableBo.getResourceId())) {
            return AppResponse.success(ErrorCodeEnum.S_SUCCESS);
        }
        throw new ServiceException("메일함예약 작업사용");
    }
}