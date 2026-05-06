package com.iflytek.rpa.component.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.base.dao.*;
import com.iflytek.rpa.base.service.CParamService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.component.dao.ComponentDao;
import com.iflytek.rpa.component.dao.ComponentVersionDao;
import com.iflytek.rpa.component.entity.Component;
import com.iflytek.rpa.component.entity.ComponentVersion;
import com.iflytek.rpa.component.entity.dto.CreateVersionDto;
import com.iflytek.rpa.component.service.ComponentVersionService;
import com.iflytek.rpa.robot.constants.RobotConstant;
import com.iflytek.rpa.robot.entity.dto.RobotVersionDto;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.Date;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 컴포넌트버전테이블(ComponentVersion)테이블서비스유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Service("componentVersionService")
public class ComponentVersionServiceImpl extends ServiceImpl<ComponentVersionDao, ComponentVersion>
        implements ComponentVersionService {

    @Resource
    private ComponentVersionDao componentVersionDao;

    @Resource
    private ComponentDao componentDao;

    @Autowired
    private CProcessDao processDao;

    @Autowired
    private CGroupDao groupDao;

    @Autowired
    private CElementDao elementDao;

    @Autowired
    private CGlobalVarDao globalVarDao;

    @Autowired
    private CRequireDao requireDao;

    @Autowired
    private CModuleDao moduleDao;

    @Autowired
    private CSmartComponentDao smartComponentDao;

    @Autowired
    private CParamService paramService;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> createComponentVersion(CreateVersionDto createVersionDto) throws NoLoginException {
        // 가져오기현재사용자 정보
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        String componentId = createVersionDto.getComponentId();

        // 가져오기 새버전
        Integer nextVersion = createVersionDto.getNextVersion();
        Integer latestVersion = componentVersionDao.getLatestVersion(componentId, tenantId);
        if (null != latestVersion && latestVersion >= nextVersion) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM.getCode(), "버전오류");
        }

        // 의component
        updateComponent(componentId, createVersionDto.getName(), userId, tenantId);

        // 삽입의base라이브러리테이블
        createDataForComponentNewVersion(userId, nextVersion, componentId);

        // 삽입아래일개component Version
        boolean result = insertNextComponentVer(createVersionDto, userId, tenantId);

        if (result) {
            return AppResponse.success(true);
        } else {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "컴포넌트버전생성실패");
        }
    }

    public void createDataForComponentNewVersion(String userId, Integer nextVersion, String componentId) {

        RobotVersionDto robotVersionDto = new RobotVersionDto();
        robotVersionDto.setVersion(nextVersion);
        robotVersionDto.setRobotId(componentId);
        robotVersionDto.setCreatorId(userId);

        // 생성새버전의프로세스대기데이터
        processDao.createProcessForCurrentVersion(robotVersionDto);
        // 요소그룹데이터
        groupDao.createGroupForCurrentVersion(robotVersionDto);
        // 원데이터
        elementDao.createElementForCurrentVersion(robotVersionDto);
        // 전역 변수데이터
        globalVarDao.createGlobalVarForCurrentVersion(robotVersionDto);
        // python데이터
        requireDao.createRequireForCurrentVersion(robotVersionDto);
        // python모듈 module데이터
        moduleDao.createModuleForCurrentVersion(robotVersionDto);
        // 가능컴포넌트
        smartComponentDao.createSmartComponentForCurrentVersion(robotVersionDto);
        // 프로세스매개변수
        paramService.createParamForCurrentVersion(null, robotVersionDto, 0);
    }

    public void updateComponent(String componentId, String name, String userId, String tenantId) {
        Component component = componentDao.getComponentById(componentId, userId, tenantId);
        if (component == null) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode());

        // 이름 변경검증
        Long count = componentDao.countByName(name, tenantId, userId, component.getId());
        if (count > 0) throw new ServiceException(ErrorCodeEnum.E_SQL_REPEAT.getCode(), "컴포넌트이름완료저장에서");

        component.setTransformStatus(RobotConstant.PUBLISHED);
        component.setName(name);
        component.setUpdateTime(new Date());

        int i = componentDao.updateById(component);
        if (i < 1) throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode());
    }

    public boolean insertNextComponentVer(CreateVersionDto createVersionDto, String userId, String tenantId) {
        ComponentVersion componentVersion = new ComponentVersion();

        // 버전정보
        componentVersion.setComponentId(createVersionDto.getComponentId());
        componentVersion.setVersion(createVersionDto.getNextVersion());
        componentVersion.setUpdateLog(createVersionDto.getUpdateLog());
        componentVersion.setIcon(createVersionDto.getIcon());
        componentVersion.setIntroduction(createVersionDto.getIntroduction());
        componentVersion.setCreatorId(userId);
        componentVersion.setTenantId(tenantId);
        componentVersion.setCreateTime(new Date());
        componentVersion.setUpdateTime(new Date());
        componentVersion.setDeleted(0);

        // 저장버전
        return save(componentVersion);
    }

    @Override
    public AppResponse<Integer> getNextVersionNumber(String componentId) throws NoLoginException {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        Integer latestVersion = componentVersionDao.getLatestVersion(componentId, tenantId);

        // 결과가있음버전, 반환1;아니요이면반환새버전+1
        Integer nextVersion = (latestVersion == null) ? 1 : latestVersion + 1;
        return AppResponse.success(nextVersion);
    }
}