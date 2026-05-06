package com.iflytek.rpa.component.service;

import com.iflytek.rpa.component.entity.dto.AddRobotBlockDto;
import com.iflytek.rpa.component.entity.dto.GetRobotBlockDto;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;

/**
 * 봇컴포넌트테이블(ComponentRobotBlock)테이블서비스연결
 *
 * @author makejava
 * @since 2024-12-19
 */
public interface ComponentRobotBlockService {

    /**
     * 추가봇컴포넌트의기록
     * @param addRobotBlockDto 추가기록요청 매개변수
     * @return 결과
     * @throws Exception 예외정보
     */
    AppResponse<Boolean> addRobotBlock(AddRobotBlockDto addRobotBlockDto) throws Exception;

    /**
     * 삭제봇컴포넌트의기록
     * @param addRobotBlockDto 삭제기록요청 매개변수
     * @return 결과
     * @throws Exception 예외정보
     */
    AppResponse<Boolean> deleteRobotBlock(AddRobotBlockDto addRobotBlockDto) throws Exception;

    /**
     * 가져오기봇의컴포넌트ID목록
     * @param queryDto
     * @return 의컴포넌트ID목록
     * @throws Exception 예외정보
     */
    AppResponse<List<String>> getBlockedComponentIds(GetRobotBlockDto queryDto) throws Exception;
}