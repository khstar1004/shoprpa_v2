package com.iflytek.rpa.base.service.impl;

import com.iflytek.rpa.base.annotation.RobotVersionAnnotation;
import com.iflytek.rpa.base.dao.CRequireDao;
import com.iflytek.rpa.base.entity.CRequire;
import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.base.entity.dto.CRequireDeleteDto;
import com.iflytek.rpa.base.entity.dto.CRequireDto;
import com.iflytek.rpa.base.service.CRequireService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * python관리관리(CRequire)테이블서비스유형
 *
 * @author mjren
 * @since 2024-10-14 17:21:35
 */
@Service("cRequireService")
public class CRequireServiceImpl implements CRequireService {
    @Resource
    private CRequireDao cRequireDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    @RobotVersionAnnotation
    public AppResponse<?> getRequireInfoList(BaseDto baseDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        baseDto.setCreatorId(userId);
        List<CRequire> requireList = cRequireDao.getRequireList(baseDto.getRobotId(), baseDto.getRobotVersion());
        return AppResponse.success(requireList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> addRequire(CRequireDto crequireDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        crequireDto.setCreatorId(userId);
        crequireDto.setUpdaterId(userId);
        crequireDto.setRobotVersion(0);
        int count = cRequireDao.countRequire(crequireDto);
        if (count > 0) {
            return AppResponse.error("해당완료저장에서: " + crequireDto.getPackageName());
        }
        boolean result = cRequireDao.addRequire(crequireDto);
        return AppResponse.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> deleteProject(CRequireDeleteDto cRequireDeleteDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        cRequireDeleteDto.setCreatorId(userId);
        if (cRequireDeleteDto.getIdList() == null
                || cRequireDeleteDto.getIdList().isEmpty()) {
            return AppResponse.error("삭제실패, id비워 둘 수 없습니다");
        }
        boolean result = cRequireDao.deleteRequire(cRequireDeleteDto);
        return AppResponse.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> updateRequire(CRequireDto crequireDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        crequireDto.setUpdaterId(userId);
        boolean result = cRequireDao.updateRequire(crequireDto);
        return AppResponse.success(result);
    }
}