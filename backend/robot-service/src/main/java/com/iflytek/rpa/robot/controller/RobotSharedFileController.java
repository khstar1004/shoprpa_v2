package com.iflytek.rpa.robot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iflytek.rpa.robot.entity.dto.SharedFilePageDto;
import com.iflytek.rpa.robot.entity.vo.SharedFilePageVo;
import com.iflytek.rpa.robot.service.SharedFileService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공유파일관리관리
 *
 * @author yfchen40
 * @since 2025-07-21
 */
@RestController
@RequestMapping("/robot-shared-file")
public class RobotSharedFileController {
    @Autowired
    private SharedFileService sharedFileService;

    /**
     * 가져오기공유파일분목록
     *
     * @param queryDto 조회파일
     * @return 분결과
     * @throws NoLoginException 결과가사용자로그인되지 않았습니다
     */
    @PostMapping("/page")
    public AppResponse<IPage<SharedFilePageVo>> getSharedFilePageList(@RequestBody SharedFilePageDto queryDto)
            throws NoLoginException {
        return sharedFileService.getSharedFilePageList(queryDto);
    }
}