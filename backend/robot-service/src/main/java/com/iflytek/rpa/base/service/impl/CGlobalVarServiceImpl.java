package com.iflytek.rpa.base.service.impl;

import com.iflytek.rpa.base.annotation.RobotVersionAnnotation;
import com.iflytek.rpa.base.dao.CGlobalVarDao;
import com.iflytek.rpa.base.entity.CGlobalVar;
import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.base.entity.dto.CGlobalDto;
import com.iflytek.rpa.base.service.CGlobalVarService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클라이언트-전역 변수(CGlobalVar)테이블서비스유형
 *
 * @author mjren
 * @since 2024-10-14 17:21:34
 */
@Service("cGlobalVarService")
public class CGlobalVarServiceImpl implements CGlobalVarService {
    @Resource
    private CGlobalVarDao globalVarDao;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    RpaAuthFeign rpaAuthFeign;

    @Override
    @RobotVersionAnnotation
    public AppResponse<?> getGlobalVarInfoList(BaseDto baseDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        baseDto.setCreatorId(userId);
        List<CGlobalVar> processNameList = globalVarDao.getGlobalVarInfoList(baseDto);
        return AppResponse.success(processNameList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> createGlobalVar(CGlobalDto globalDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        globalDto.setCreatorId(userId);
        globalDto.setUpdaterId(userId);
        int count = globalVarDao.countVarByName(globalDto);
        if (count > 0) {
            return AppResponse.error("현재저장된 이름변수, 요청다시 명령이름");
        }
        String globalId = String.valueOf(idWorker.nextId());
        globalDto.setGlobalId(globalId);
        boolean result = globalVarDao.createGlobalVar(globalDto);
        return AppResponse.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> saveGlobalVar(CGlobalDto globalDto) {
        // 여부재이름
        CGlobalVar globalVar = globalVarDao.getGlobalVarOne(globalDto);
        String globalId = globalDto.getGlobalId();
        if (globalVar != null && !globalId.equals(globalVar.getGlobalId())) {
            return AppResponse.error("저장된 이름변수, 요청다시 명령이름");
        }
        boolean result = globalVarDao.saveGlobalVar(globalDto);
        return AppResponse.success(result);
    }

    @Override
    public AppResponse<?> getGlobalVarNameList(String robotId) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        List<CGlobalVar> globalVarNameList = globalVarDao.getGlobalVarNameList(userId, robotId);
        return AppResponse.success(globalVarNameList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> deleteGlobalVar(CGlobalDto globalDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        globalDto.setCreatorId(userId);
        boolean result = globalVarDao.deleteGlobalVar(globalDto);
        return AppResponse.success(result);
    }
}