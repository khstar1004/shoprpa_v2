package com.iflytek.rpa.robot.controller;

import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.dto.DeleteDesignDto;
import com.iflytek.rpa.robot.entity.dto.ExeUpdateCheckDto;
import com.iflytek.rpa.robot.entity.dto.ExecuteListDto;
import com.iflytek.rpa.robot.entity.dto.RobotExecuteByNameNDeptDto;
import com.iflytek.rpa.robot.entity.vo.RobotExecuteByNameNDeptVo;
import com.iflytek.rpa.robot.service.RobotExecuteService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 단말봇테이블(RobotExecute)테이블제어
 *
 * @author mjren
 * @since 2024-10-22 16:07:33
 */
@RestController
@RequestMapping("/robot-execute")
public class RobotExecuteController {

    @Resource
    private RobotExecuteService robotExecuteService;

    /**
     * 실행기기봇목록
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/execute-list")
    public AppResponse<?> executeList(@RequestBody ExecuteListDto queryDto) throws NoLoginException {
        return robotExecuteService.executeList(queryDto);
    }

    /**
     * 계획기기-삭제봇-
     * @param robotId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/delete-robot-res")
    public AppResponse<?> deleteRobotRes(@RequestParam("robotId") String robotId) throws NoLoginException {
        return robotExecuteService.deleteRobotRes(robotId);
    }

    /**
     * 계획기기-삭제봇- 삭제
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/delete-robot")
    public AppResponse<?> deleteRobot(@RequestBody DeleteDesignDto queryDto) throws Exception {
        return robotExecuteService.deleteRobot(queryDto);
    }

    /**
     * 업데이트-에서실행기기클릭업데이트
     * @paramMarketResourceDto
     * @return
     * @throws Exception
     */
    @PostMapping("/update/pull")
    public AppResponse<?> updateRobotByPull(@RequestBody RobotExecute robotExecute) throws Exception {
        if (null == robotExecute.getRobotId()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "봇ID비워 둘 수 없습니다");
        }
        return robotExecuteService.updateRobotByPull(robotExecute.getRobotId());
    }

    @GetMapping("/robot-detail")
    public AppResponse<?> robotDetail(@RequestParam("robotId") String robotId) throws Exception {
        return robotExecuteService.robotDetail(robotId);
    }

    @PostMapping("/execute-update-check")
    public AppResponse<?> executeUpdateCheck(@RequestBody ExeUpdateCheckDto queryDto) throws Exception {
        return robotExecuteService.executeUpdateCheck(queryDto);
    }

    @PostMapping("/list/NameNDept")
    public AppResponse<List<RobotExecuteByNameNDeptVo>> getRobotExecuteList(
            @RequestBody RobotExecuteByNameNDeptDto queryDto) throws NoLoginException {
        return robotExecuteService.getRobotExecuteList(queryDto);
    }
}