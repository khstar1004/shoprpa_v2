package com.iflytek.rpa.robot.controller;

import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.example.service.SampleUsersService;
import com.iflytek.rpa.robot.entity.RobotDesign;
import com.iflytek.rpa.robot.entity.dto.DeleteDesignDto;
import com.iflytek.rpa.robot.entity.dto.DesignListDto;
import com.iflytek.rpa.robot.service.RobotDesignService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

/**
 * 단말봇테이블(RobotDesign)테이블제어
 *
 * @author makejava
 * @since 2024-09-29 15:27:34
 */
@RestController
@RequestMapping("/robot-design")
public class RobotDesignController {

    @Resource
    private RobotDesignService robotDesignService;

    @Autowired
    SampleUsersService sampleUsersService;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 생성봇
     *
     * @param robot
     * @return
     * @throws Exception
     */
    @PostMapping("/create")
    public AppResponse<?> createRobot(@Valid @RequestBody RobotDesign robot) throws Exception {
        return robotDesignService.createRobot(robot);
    }

    /**
     * 새생성봇-가져오기 이름
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/create-name")
    public AppResponse<?> createRobotName() throws Exception {
        return robotDesignService.createRobotName();
    }

    /**
     * 계획기기봇목록
     *
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/design-list")
    public AppResponse<?> designList(@RequestBody DesignListDto queryDto) throws NoLoginException {
        AppResponse<User> res = rpaAuthFeign.getLoginUser();
        if (res == null || res.getData() == null) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = res.getData();
        String userId = loginUser.getId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        // 트리거예시비고입력
        AppResponse<Boolean> response = sampleUsersService.insertUserSample(userId, tenantId);

        return robotDesignService.designList(queryDto);
    }

    /**
     * 계획기기이름 변경연결
     *
     * @param newName
     * @param robotId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/rename")
    public AppResponse<?> rename(@RequestParam("newName") String newName, @RequestParam("robotId") String robotId)
            throws NoLoginException {
        return robotDesignService.rename(newName, robotId);
    }

    /**
     * 계획기기명령이름재복사연결
     *
     * @param newName
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/design-name-dup")
    public AppResponse<?> designNameDup(
            @RequestParam("newName") String newName, @RequestParam("robotId") String robotId) throws NoLoginException {

        return robotDesignService.designNameDup(newName, robotId);
    }

    /**
     * 계획기기-생성의봇-
     *
     * @param robotId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/my-robot-detail")
    public AppResponse<?> myRobotDetail(@RequestParam("robotId") String robotId) throws NoLoginException {
        return robotDesignService.myRobotDetail(robotId);
    }

    /**
     * 계획기기-가져오기의봇-
     *
     * @param robotId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/market-robot-detail")
    public AppResponse<?> marketRobotDetail(@RequestParam("robotId") String robotId) throws Exception {
        return robotDesignService.marketRobotDetail(robotId);
    }

    /**
     * 계획기기-생성본
     *
     * @param robotId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/copy-design-robot")
    public AppResponse<?> copyRobot(
            @RequestParam("robotId") String robotId, @RequestParam("robotName") String robotName) throws Exception {
        return robotDesignService.copyDesignRobot(robotId, robotName);
    }

    /**
     * 계획기기-삭제봇-
     *
     * @param robotId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("/delete-robot-res")
    public AppResponse<?> deleteRobotRes(@RequestParam("robotId") String robotId) throws Exception {
        return robotDesignService.deleteRobotRes(robotId);
    }

    /**
     * 계획기기-삭제봇- 삭제
     *
     * @param queryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/delete-robot")
    public AppResponse<?> deleteRobot(@RequestBody DeleteDesignDto queryDto) throws Exception {
        return robotDesignService.deleteRobot(queryDto);
    }
}