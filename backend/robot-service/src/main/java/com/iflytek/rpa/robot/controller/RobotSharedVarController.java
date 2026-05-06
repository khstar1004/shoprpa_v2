package com.iflytek.rpa.robot.controller;

import com.iflytek.rpa.robot.entity.dto.SharedVarBatchDto;
import com.iflytek.rpa.robot.entity.vo.ClientSharedVarVo;
import com.iflytek.rpa.robot.entity.vo.SharedVarKeyVo;
import com.iflytek.rpa.robot.service.SharedVarService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 공유 변수관리관리
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@RestController
@RequestMapping("/robot-shared-var")
public class RobotSharedVarController {

    @Resource
    private SharedVarService sharedVarService;

    /**
     * 가져오기공유 변수테넌트키
     *
     * @return 키정보
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    @GetMapping("/shared-var-key")
    public AppResponse<SharedVarKeyVo> getSharedVarKey() throws NoLoginException {
        return sharedVarService.getSharedVarKey();
    }

    /**
     * 클라이언트-조회해당사용자가능사용의모든공유 변수
     *
     * @return 공유 변수목록
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    @GetMapping("/get-shared-var")
    public AppResponse<List<ClientSharedVarVo>> getClientSharedVars() throws NoLoginException {
        return sharedVarService.getClientSharedVars();
    }

    /**
     * 클라이언트-근거id량조회공유 변수
     *
     * @param dto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/get-batch-shared-var")
    public AppResponse<List<ClientSharedVarVo>> getBatchSharedVar(@RequestBody SharedVarBatchDto dto)
            throws NoLoginException {
        return sharedVarService.getBatchSharedVar(dto);
    }
}