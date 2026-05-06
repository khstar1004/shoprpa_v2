package com.iflytek.rpa.component.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.DISPATCH;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.base.annotation.RobotVersionAnnotation;
import com.iflytek.rpa.base.dao.CProcessDao;
import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.component.dao.ComponentDao;
import com.iflytek.rpa.component.dao.ComponentRobotUseDao;
import com.iflytek.rpa.component.dao.ComponentVersionDao;
import com.iflytek.rpa.component.entity.Component;
import com.iflytek.rpa.component.entity.ComponentRobotUse;
import com.iflytek.rpa.component.entity.ComponentVersion;
import com.iflytek.rpa.component.entity.bo.ComponentRobotUseDeleteBo;
import com.iflytek.rpa.component.entity.bo.ComponentRobotUseUpdateBo;
import com.iflytek.rpa.component.entity.dto.*;
import com.iflytek.rpa.component.entity.vo.ComponentUseVo;
import com.iflytek.rpa.component.entity.vo.EditCompUseVo;
import com.iflytek.rpa.component.service.ComponentRobotUseService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 봇컴포넌트사용테이블(ComponentRobotUse)테이블서비스유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Service("componentRobotUseService")
public class ComponentRobotUseServiceImpl extends ServiceImpl<ComponentRobotUseDao, ComponentRobotUse>
        implements ComponentRobotUseService {

    @Autowired
    private ComponentRobotUseDao componentRobotUseDao;

    @Autowired
    private ComponentVersionDao componentVersionDao;

    @Autowired
    private ComponentDao componentDao;

    @Autowired
    private CProcessDao cProcessDao;

    @Autowired
    private ComponentRobotUseServiceImpl self;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    private static List<ComponentUseVo> getComponentUseVos(List<ComponentRobotUse> componentRobotUses) {
        List<ComponentUseVo> componentUseVos = new ArrayList<>();
        if (componentRobotUses != null && !componentRobotUses.isEmpty()) {
            for (ComponentRobotUse componentRobotUse : componentRobotUses) {
                ComponentUseVo componentUseVo = new ComponentUseVo();
                componentUseVo.setComponentId(componentRobotUse.getComponentId());
                componentUseVo.setVersion(componentRobotUse.getComponentVersion());
                componentUseVos.add(componentUseVo);
            }
        }
        return componentUseVos;
    }

    @Override
    public AppResponse<List<ComponentUseVo>> getComponentUse(GetComponentUseDto getComponentUseDto)
            throws NoLoginException {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        if (getComponentUseDto.getMode().equals(DISPATCH)) {
            getComponentUseDto.setVersion(getComponentUseDto.getRobotVersion());
        }
        Integer robotVersion = getRobotVersion(
                getComponentUseDto.getRobotId(),
                getComponentUseDto.getMode(),
                getComponentUseDto.getVersion(),
                new BaseDto());

        // 근거봇ID및버전조회컴포넌트사용
        List<ComponentRobotUse> componentRobotUses =
                componentRobotUseDao.getByRobotIdAndVersion(getComponentUseDto.getRobotId(), robotVersion, tenantId);
        if (CollectionUtils.isEmpty(componentRobotUses)) return AppResponse.success(Collections.EMPTY_LIST);

        List<ComponentUseVo> componentUseVos = getComponentUseVos(componentRobotUses);

        return AppResponse.success(componentUseVos);
    }

    @Override
    public AppResponse<String> addComponentUse(AddCompUseDto addCompUseDto) throws NoLoginException {
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

        // 가져오기봇버전
        Integer robotVersion = getRobotVersion(
                addCompUseDto.getRobotId(), addCompUseDto.getMode(), addCompUseDto.getRobotVersion(), new BaseDto());

        // 생성사용의시예 새예컴포넌트의새버전
        Integer latestVersion = componentVersionDao.getLatestVersion(addCompUseDto.getComponentId(), tenantId);

        // 조회여부완료저장된 의컴포넌트사용기록
        ComponentRobotUse existingRecord = componentRobotUseDao.getByRobotIdVersionAndComponentIdVersion(
                addCompUseDto.getRobotId(), robotVersion, addCompUseDto.getComponentId(), latestVersion, tenantId);

        if (existingRecord != null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_REPEAT.getCode(), "해당봇버전아래완료저장된 의컴포넌트사용기록");
        }

        // 생성컴포넌트사용기록
        ComponentRobotUse componentRobotUse = new ComponentRobotUse();
        componentRobotUse.setRobotId(addCompUseDto.getRobotId());
        componentRobotUse.setRobotVersion(robotVersion);
        componentRobotUse.setComponentId(addCompUseDto.getComponentId());
        componentRobotUse.setComponentVersion(latestVersion);
        componentRobotUse.setCreatorId(userId);
        componentRobotUse.setCreateTime(new Date());
        componentRobotUse.setUpdaterId(userId);
        componentRobotUse.setUpdateTime(new Date());
        componentRobotUse.setDeleted(0);
        componentRobotUse.setTenantId(tenantId);

        // 저장까지데이터베이스
        boolean result = this.save(componentRobotUse);
        if (result) {
            return AppResponse.success("컴포넌트사용추가성공");
        } else {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "컴포넌트사용추가실패");
        }
    }

    @Override
    public AppResponse<String> deleteComponentUse(DelComponentUseDto delComponentUseDto) throws NoLoginException {
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

        // 가져오기봇버전
        Integer robotVersion = getRobotVersion(
                delComponentUseDto.getRobotId(),
                delComponentUseDto.getMode(),
                delComponentUseDto.getRobotVersion(),
                new BaseDto());

        // 생성삭제BO객체
        ComponentRobotUseDeleteBo deleteBo = new ComponentRobotUseDeleteBo();
        deleteBo.setRobotId(delComponentUseDto.getRobotId());
        deleteBo.setRobotVersion(robotVersion);
        deleteBo.setComponentId(delComponentUseDto.getComponentId());
        deleteBo.setTenantId(tenantId);
        deleteBo.setUpdaterId(userId);

        // 호출DAO방법법실행삭제
        int result = componentRobotUseDao.deleteComponentUse(deleteBo);

        if (result > 0) {
            return AppResponse.success("컴포넌트사용삭제성공");
        } else {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "컴포넌트사용삭제실패");
        }
    }

    @Override
    public AppResponse<String> updateComponentUse(UpdateComponentUseDto updateComponentUseDto) throws NoLoginException {
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

        // 가져오기봇버전
        Integer robotVersion = getRobotVersion(
                updateComponentUseDto.getRobotId(),
                updateComponentUseDto.getMode(),
                updateComponentUseDto.getRobotVersion(),
                new BaseDto());

        // 가져오기 있음컴포넌트사용기록
        ComponentRobotUse existingUse = getExistingComponentUse(updateComponentUseDto, robotVersion, userId);

        // 검증버전
        validateComponentVersion(updateComponentUseDto, existingUse, tenantId);

        // 실행업데이트
        return executeComponentUseUpdate(updateComponentUseDto, existingUse, robotVersion, tenantId, userId);
    }

    /**
     * 가져오기 있음컴포넌트사용기록
     */
    private ComponentRobotUse getExistingComponentUse(
            UpdateComponentUseDto updateComponentUseDto, Integer robotVersion, String userId) {
        ComponentRobotUse existingUse = componentRobotUseDao.getByRobotIdVersionAndComponentId(
                updateComponentUseDto.getRobotId(), robotVersion, updateComponentUseDto.getComponentId(), userId);

        if (existingUse == null) {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "해당봇버전아래찾을 수 없는 의컴포넌트사용");
        }
        return existingUse;
    }

    /**
     * 검증컴포넌트버전
     */
    private void validateComponentVersion(
            UpdateComponentUseDto updateComponentUseDto, ComponentRobotUse existingUse, String tenantId) {
        Integer oldVersion = existingUse.getComponentVersion();
        Integer newVersion = updateComponentUseDto.getComponentVersion();

        // 검증새버전여부대버전
        if (newVersion <= oldVersion) {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "새버전대현재버전");
        }

        // 검증새버전에서component_version테이블중여부저장에서
        ComponentVersion newComponentVersion = componentVersionDao.getVersionByComponentIdAndVersion(
                updateComponentUseDto.getComponentId(), newVersion, tenantId);
        if (newComponentVersion == null) {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "지정한 컴포넌트 버전을 찾을 수 없습니다");
        }
    }

    /**
     * 실행컴포넌트사용업데이트
     */
    private AppResponse<String> executeComponentUseUpdate(
            UpdateComponentUseDto updateComponentUseDto,
            ComponentRobotUse existingUse,
            Integer robotVersion,
            String tenantId,
            String userId) {
        // 생성업데이트BO객체
        ComponentRobotUseUpdateBo updateBo = new ComponentRobotUseUpdateBo();
        updateBo.setRobotId(updateComponentUseDto.getRobotId());
        updateBo.setRobotVersion(robotVersion);
        updateBo.setComponentId(updateComponentUseDto.getComponentId());
        updateBo.setOldComponentVersion(existingUse.getComponentVersion());
        updateBo.setNewComponentVersion(updateComponentUseDto.getComponentVersion());
        updateBo.setTenantId(tenantId);
        updateBo.setUpdaterId(userId);

        int result = componentRobotUseDao.updateComponentUse(updateBo);

        if (result > 0) {
            return AppResponse.success("컴포넌트사용버전업데이트성공");
        } else {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "컴포넌트사용버전업데이트실패");
        }
    }

    public Integer getRobotVersion(String robotId, String mode, Integer version, BaseDto baseDto) {
        baseDto.setMode(mode);
        baseDto.setRobotVersion(version);
        baseDto.setRobotId(robotId);

        // 해제관리아니요제목, 할 수 없음사용유형호출의방법법, 가능사용비고입력의방식
        self.getVersion(baseDto);

        return baseDto.getRobotVersion();
    }

    @RobotVersionAnnotation
    public void getVersion(BaseDto baseDto) {}

    @Override
    public AppResponse<String> getProcessId(String componentId, Integer componentVersion) throws NoLoginException {

        // 조회프로세스ID
        String processId = cProcessDao.getProcessIdByComp(componentId, componentVersion);

        if (processId == null) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "프로세스 ID 조회 결과가 비어 있습니다");

        return AppResponse.success(processId);
    }

    @Override
    public AppResponse<EditCompUseVo> getEditCompUse(EditCompUseDto queryDto) throws NoLoginException {
        String componentId = queryDto.getComponentId();
        String robotId = queryDto.getRobotId();
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

        ComponentRobotUse componentRobotUse =
                componentRobotUseDao.getByRobotIdVersionAndComponentId(robotId, 0, componentId, userId);
        ComponentVersion componentVersion = componentVersionDao.getVersionByComponentIdAndVersion(
                componentId, componentRobotUse.getComponentVersion(), tenantId);
        Component component = componentDao.getComponentById(componentId, userId, tenantId);

        EditCompUseVo editCompUseVo = new EditCompUseVo();
        editCompUseVo.setName(component.getName());
        editCompUseVo.setIcon(componentVersion.getIcon());
        editCompUseVo.setComponentId(componentId);
        editCompUseVo.setComponentVersion(componentVersion.getVersion());

        return AppResponse.success(editCompUseVo);
    }
}