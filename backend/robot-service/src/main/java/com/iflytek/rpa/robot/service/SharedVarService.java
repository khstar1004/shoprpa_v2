package com.iflytek.rpa.robot.service;

import com.iflytek.rpa.robot.entity.dto.SharedVarBatchDto;
import com.iflytek.rpa.robot.entity.vo.ClientSharedVarVo;
import com.iflytek.rpa.robot.entity.vo.SharedVarKeyVo;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;

/**
 * 공유 변수서비스연결
 *
 * @author jqfang3
 * @since 2025-07-21
 */
public interface SharedVarService {
    /**
     * 가져오기공유 변수테넌트키
     *
     * @return 키정보
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<SharedVarKeyVo> getSharedVarKey() throws NoLoginException;

    /**
     * 클라이언트-조회해당사용자가능사용의모든공유 변수
     *
     * @return 공유 변수목록
     * @throws NoLoginException 로그인되지 않았습니다예외
     */
    AppResponse<List<ClientSharedVarVo>> getClientSharedVars() throws NoLoginException;

    AppResponse<List<ClientSharedVarVo>> getBatchSharedVar(SharedVarBatchDto updateDto) throws NoLoginException;
}