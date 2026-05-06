package com.iflytek.rpa.component.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.base.annotation.RobotVersionAnnotation;
import com.iflytek.rpa.base.dao.CProcessDao;
import com.iflytek.rpa.base.entity.CProcess;
import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.component.dao.ComponentDao;
import com.iflytek.rpa.component.dao.ComponentRobotBlockDao;
import com.iflytek.rpa.component.dao.ComponentRobotUseDao;
import com.iflytek.rpa.component.dao.ComponentVersionDao;
import com.iflytek.rpa.component.entity.Component;
import com.iflytek.rpa.component.entity.ComponentRobotUse;
import com.iflytek.rpa.component.entity.ComponentVersion;
import com.iflytek.rpa.component.entity.dto.CheckNameDto;
import com.iflytek.rpa.component.entity.dto.ComponentListDto;
import com.iflytek.rpa.component.entity.dto.EditPageCompInfoDto;
import com.iflytek.rpa.component.entity.dto.GetComponentUseDto;
import com.iflytek.rpa.component.entity.vo.*;
import com.iflytek.rpa.component.service.ComponentService;
import com.iflytek.rpa.robot.constants.RobotConstant;
import com.iflytek.rpa.robot.service.RobotDesignService;
import com.iflytek.rpa.robot.service.impl.RobotDesignServiceImpl;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 컴포넌트테이블(Component)테이블서비스유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Service("componentService")
public class ComponentServiceImpl extends ServiceImpl<ComponentDao, Component> implements ComponentService {

    @Autowired
    private ComponentDao componentDao;

    @Autowired
    private CProcessDao cProcessDao;

    @Autowired
    private ComponentVersionDao componentVersionDao;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RobotDesignService robotDesignService;

    @Autowired
    private ComponentServiceImpl self;

    @Autowired
    private ComponentRobotBlockDao componentRobotBlockDao;

    @Autowired
    private ComponentRobotUseDao componentRobotUseDao;

    @Autowired
    private RobotDesignServiceImpl robotDesignServiceImpl;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<CProcess> createComponent(String componentName) throws NoLoginException {
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

        // 조회이름여부재복사
        Long count = componentDao.countByName(componentName, tenantId, userId, null);
        if (count > 0) throw new ServiceException("컴포넌트이름완료저장에서");

        // 조회컴포넌트이름길이정도
        if (componentName.length() > 50) {
            throw new ServiceException("컴포넌트 이름은 50자를 초과할 수 없습니다");
        }

        // 컴포넌트정보
        String componentId = String.valueOf(idWorker.nextId());
        Component component = new Component();
        component.setName(componentName);
        component.setComponentId(componentId);
        component.setCreatorId(userId);
        component.setUpdaterId(userId);
        component.setTenantId(tenantId);
        component.setCreateTime(new Date());
        component.setUpdateTime(new Date());
        component.setDeleted(0);
        component.setIsShown(1);
        component.setTransformStatus("editing");
        component.setDataSource("create");
        int insert = baseMapper.insert(component);
        if (insert < 1) throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "컴포넌트생성실패");

        // 새생성프로세스,봇버전예0
        CProcess cProcess = new CProcess();
        cProcess.setRobotId(componentId);
        cProcess.setProcessId(idWorker.nextId() + "");
        cProcess.setProcessName("프로세스");
        cProcess.setProcessContent("[]");
        cProcess.setCreatorId(userId);
        cProcess.setUpdaterId(userId);
        cProcess.setRobotVersion(0);
        cProcessDao.createProcess(cProcess);

        CProcess cProcess1 = new CProcess();
        cProcess1.setRobotId(componentId);
        cProcess1.setProcessId(cProcess.getProcessId());
        return AppResponse.success(cProcess1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> deleteComponent(String componentId) throws NoLoginException {
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

        // 조회컴포넌트여부저장에서
        Component shownComponent = componentDao.getShownComponentById(componentId, userId, tenantId);
        if (shownComponent == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "컴포넌트를 찾을 수 없습니다");
        }

        // 삭제컴포넌트
        Integer result = componentDao.deleteComponent(componentId, userId, tenantId);
        if (result > 0) {
            return AppResponse.success(true);
        } else {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "삭제컴포넌트실패");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> renameComponent(String componentId, String newName) throws NoLoginException {

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

        // 조회컴포넌트여부저장에서
        Component existingComponent = componentDao.getComponentById(componentId, userId, tenantId);
        if (existingComponent == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "컴포넌트를 찾을 수 없습니다");
        }

        // 조회이름여부재복사, 정렬제거
        Long count = componentDao.countByName(newName, tenantId, userId, existingComponent.getId());
        if (count > 0) throw new ServiceException(ErrorCodeEnum.E_SQL_REPEAT.getCode(), "컴포넌트이름완료저장에서");

        // 업데이트컴포넌트이름
        Component updateComponent = new Component();
        updateComponent.setId(existingComponent.getId());
        updateComponent.setName(newName);
        updateComponent.setUpdaterId(userId);
        updateComponent.setUpdateTime(new Date());

        boolean result = updateById(updateComponent);
        if (result) return AppResponse.success(true);
        else throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "이름 변경실패");
    }

    @Override
    public AppResponse<Boolean> checkNameDuplicate(CheckNameDto checkNameDto) throws NoLoginException {

        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        String componentId = checkNameDto.getComponentId();
        String name = checkNameDto.getName();
        Long excludeId = null;

        if (StringUtils.isNotBlank(componentId)) {
            Component existingComponent = componentDao.getComponentById(componentId, userId, tenantId);
            if (existingComponent != null) {
                excludeId = existingComponent.getId();
            }
        }

        Long count = componentDao.countByName(name, tenantId, userId, excludeId);
        boolean isDuplicate = count > 0;

        return AppResponse.success(isDuplicate);
    }

    @Override
    public AppResponse<String> createComponentName() throws NoLoginException {
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
        String componentNameBase = "컴포넌트";
        List<String> componentNameList = componentDao.getComponentNameList(tenantId, userId, componentNameBase);
        int componetNameIndex = 1;
        List<Integer> componentNameIndexList = new ArrayList<>();
        for (String componentName : componentNameList) {
            String[] componentNameSplit = componentName.split(componentNameBase);
            if (componentNameSplit.length == 2 && componentNameSplit[1].matches("^[1-9]\\d*$")) {
                int componentNameNum = Integer.parseInt(componentNameSplit[1]);
                componentNameIndexList.add(componentNameNum);
            }
        }
        Collections.sort(componentNameIndexList);
        for (int i = 0; i < componentNameIndexList.size(); i++) {
            if (componentNameIndexList.get(i) != i + 1) {
                componetNameIndex = i + 1;
                break;
            } else {
                componetNameIndex += 1;
            }
        }
        return AppResponse.success(componentNameBase + componetNameIndex);
    }

    @Override
    public AppResponse<ComponentInfoVo> getComponentInfo(String componentId) throws NoLoginException {
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

        // 가져오기컴포넌트본정보
        Component component = componentDao.getComponentById(componentId, userId, tenantId);
        if (component == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "컴포넌트를 찾을 수 없습니다");
        }

        // 가져오기컴포넌트버전목록
        List<ComponentVersion> componentVersionList =
                componentVersionDao.getVersionsByComponentId(componentId, tenantId);

        // 가져오기 새버전
        Integer latestVersion = componentVersionDao.getLatestVersion(componentId, tenantId);
        // 가져오기생성자이름
        AppResponse<String> realNameResp = rpaAuthFeign.getNameById(component.getCreatorId());
        if (realNameResp == null || realNameResp.getData() == null) {
            throw new ServiceException("사용자명가져오기실패");
        }
        String creatorName = realNameResp.getData();

        // 가져오기 새버전의및아이콘
        String introduction = "";
        String icon = "";
        if (latestVersion != null && !componentVersionList.isEmpty()) {
            ComponentVersion latestVersionInfo = componentVersionList.stream()
                    .filter(v -> v.getVersion().equals(latestVersion))
                    .findFirst()
                    .orElse(null);
            if (latestVersionInfo != null) {
                introduction = latestVersionInfo.getIntroduction();
                icon = latestVersionInfo.getIcon();
            }
        }

        // 생성버전정보목록
        List<VersionInfo> versionInfoList = new ArrayList<>();
        for (ComponentVersion version : componentVersionList) {
            VersionInfo versionInfo = new VersionInfo();
            versionInfo.setVersion(version.getVersion());
            versionInfo.setCreateTime(version.getCreateTime());
            versionInfo.setUpdateLog(version.getUpdateLog());
            versionInfoList.add(versionInfo);
        }

        // 생성반환객체
        ComponentInfoVo componentInfoVo = new ComponentInfoVo();
        componentInfoVo.setName(component.getName());
        componentInfoVo.setIcon(icon);
        componentInfoVo.setLatestVersion(latestVersion);
        componentInfoVo.setCreatorName(creatorName);
        componentInfoVo.setIntroduction(introduction);
        componentInfoVo.setVersionInfoList(versionInfoList);

        return AppResponse.success(componentInfoVo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> copyComponent(String componentId, String name) throws Exception {
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

        // 가져오기기존컴포넌트정보
        Component originalComponent = componentDao.getComponentById(componentId, userId, tenantId);
        if (originalComponent == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "기존컴포넌트를 찾을 수 없습니다");
        }

        // 조회새이름여부재복사
        Long count = componentDao.countByName(name, tenantId, userId, null);
        if (count > 0) {
            throw new ServiceException("컴포넌트이름완료저장에서");
        }

        // 생성새컴포넌트
        String newComponentId = String.valueOf(idWorker.nextId());
        Component newComponent = new Component();
        newComponent.setName(name);
        newComponent.setComponentId(newComponentId);
        newComponent.setCreatorId(userId);
        newComponent.setUpdaterId(userId);
        newComponent.setTenantId(tenantId);
        newComponent.setDataSource(RobotConstant.CREATE);
        newComponent.setTransformStatus(RobotConstant.EDITING);
        newComponent.setIsShown(1);
        newComponent.setCreateTime(new Date());
        newComponent.setUpdateTime(new Date());

        // 저장새컴포넌트
        boolean saveResult = save(newComponent);
        if (!saveResult) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode(), "생성본컴포넌트실패");
        }

        // TODO: 복사컴포넌트버전정보대기닫기데이터
        copyEditBase4Comp(originalComponent.getComponentId(), newComponentId, userId);

        return AppResponse.success(true);
    }

    @Override
    public AppResponse<String> copyCreateName(String componentId) throws Exception {
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

        // 가져오기기존컴포넌트정보
        Component originalComponent = componentDao.getComponentById(componentId, userId, tenantId);
        if (originalComponent == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "기존컴포넌트를 찾을 수 없습니다");
        }

        // 완료새이름
        String newName = generateCopyComponentName(originalComponent.getName(), tenantId, userId);

        return AppResponse.success(newName);
    }

    /**
     * 컴포넌트닫기의데이터
     * @param oldComponentId
     * @param newComponentId
     * @param userId
     * @throws Exception
     */
    public void copyEditBase4Comp(String oldComponentId, String newComponentId, String userId) throws Exception {
        // 분그룹
        robotDesignServiceImpl.groupCopy(oldComponentId, newComponentId, userId);
        // 요소
        robotDesignServiceImpl.elementCopy(oldComponentId, newComponentId, userId);
        // 전역 변수
        robotDesignServiceImpl.globalValCopy(oldComponentId, newComponentId, userId);
        // 프로세스
        robotDesignServiceImpl.processCopy(oldComponentId, newComponentId, userId);
        // python
        robotDesignServiceImpl.requireCopy(oldComponentId, newComponentId, userId);
        // python모듈
        robotDesignServiceImpl.moduleCopy(oldComponentId, newComponentId, userId);
        // 구성 매개변수
        robotDesignServiceImpl.paramCopy(oldComponentId, newComponentId, userId);
        // 가능컴포넌트
        robotDesignServiceImpl.smartComponentCopy(oldComponentId, newComponentId, userId);
    }

    /**
     * 완료본컴포넌트이름
     * @param originalName 기존컴포넌트이름
     * @param tenantId 테넌트ID
     * @return 새의컴포넌트이름
     */
    private String generateCopyComponentName(String originalName, String tenantId, String userId) {
        String baseName = originalName + "-본";
        String newName = baseName;
        int suffix = 1;

        // 조회이름여부재복사, 결과가재복사이면증가추가숫자후
        while (isComponentNameExists(newName, tenantId, userId)) {
            newName = baseName + suffix;
            suffix++;
        }

        return newName;
    }

    /**
     * 조회컴포넌트이름여부저장에서
     * @param name 컴포넌트이름
     * @param tenantId 테넌트ID
     * @return 여부저장에서
     */
    private boolean isComponentNameExists(String name, String tenantId, String userId) {
        Long count = componentDao.countByName(name, tenantId, userId, null);
        return count > 0;
    }

    @Override
    public AppResponse<IPage<ComponentVo>> getComponentPageList(ComponentListDto componentListDto) throws Exception {
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

        // 생성분객체
        Page<ComponentVo> page = new Page<>(componentListDto.getPageNum(), componentListDto.getPageSize());

        // 호출 DAO 행분조회
        IPage<ComponentVo> result = componentDao.getComponentPageList(
                page,
                componentListDto.getName(),
                componentListDto.getDataSource(),
                componentListDto.getSortType(),
                tenantId,
                userId);

        return AppResponse.success(result);
    }

    @Override
    public AppResponse<List<EditingPageCompVo>> getEditingPageCompList(GetComponentUseDto queryDto) throws Exception {

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

        Integer robotVersion =
                getRobotVersion(queryDto.getRobotId(), queryDto.getMode(), queryDto.getVersion(), new BaseDto());
        queryDto.setVersion(robotVersion);

        // 1. 가져오기사용자권한내부가능가져오기의컴포넌트(shown = 1)
        List<Component> availableComponents = componentDao.getAvailableComponentsByUser(tenantId, userId);
        if (CollectionUtils.isEmpty(availableComponents)) {
            return AppResponse.success(Collections.emptyList());
        }

        // 2. 통신경과componentVersion테이블, 필터링까지완료발송경과버전의componentList
        List<String> publishedComponentIds = componentVersionDao.getPublishedComponentIds(tenantId);
        if (CollectionUtils.isEmpty(publishedComponentIds)) {
            return AppResponse.success(Collections.emptyList());
        }

        // 필터링출력완료발송경과버전의컴포넌트
        List<Component> publishedComponents = availableComponents.stream()
                .filter(component -> publishedComponentIds.contains(component.getComponentId()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(publishedComponents)) {
            return AppResponse.success(Collections.emptyList());
        }

        // 3. 근거robotId및version가져오기 의blockComponentIdList
        List<String> blockedComponentIds =
                getBlockedComponentIds(queryDto.getRobotId(), queryDto.getVersion(), tenantId);

        // 4. 근거robotId및version가져오기 사용의useComponentIdList
        List<String> usedComponentIds = getUsedComponentIds(queryDto.getRobotId(), queryDto.getVersion(), tenantId);

        // 5. componentList - blockComponentIdList + useComponentIdList 까지종료의목록
        List<String> finalComponentIds = getFinalComponentIds(
                publishedComponents.stream().map(Component::getComponentId).collect(Collectors.toList()),
                blockedComponentIds,
                usedComponentIds);

        // 6. 그룹설치성공 List<EditingPageCompVo> 반환
        List<EditingPageCompVo> result = buildEditingPageCompVoList(finalComponentIds, tenantId);

        // 7. icon및isLatest필드 ---- 시추가필요
        setIconAndIsLatest(result, queryDto.getRobotId(), queryDto.getVersion(), tenantId);

        return AppResponse.success(result);
    }

    /**
     * 가져오기 의컴포넌트ID목록
     */
    private List<String> getBlockedComponentIds(String robotId, Integer version, String tenantId) {
        if (StringUtils.isBlank(robotId) || version == null) {
            return Collections.emptyList();
        }
        return componentRobotBlockDao.getBlockedComponentIds(robotId, version, tenantId);
    }

    /**
     * 가져오기 사용의컴포넌트ID목록
     */
    private List<String> getUsedComponentIds(String robotId, Integer version, String tenantId) {
        if (StringUtils.isBlank(robotId) || version == null) {
            return Collections.emptyList();
        }
        List<ComponentRobotUse> usedComponents =
                componentRobotUseDao.getByRobotIdAndVersion(robotId, version, tenantId);
        if (CollectionUtils.isEmpty(usedComponents)) {
            return Collections.emptyList();
        }
        return usedComponents.stream().map(ComponentRobotUse::getComponentId).collect(Collectors.toList());
    }

    /**
     * 계획종료의컴포넌트ID목록
     */
    private List<String> getFinalComponentIds(
            List<String> publishedComponentIds, List<String> blockedComponentIds, List<String> usedComponentIds) {
        // 제거의컴포넌트
        List<String> result = publishedComponentIds.stream()
                .filter(id -> !blockedComponentIds.contains(id))
                .collect(Collectors.toList());

        // 추가사용의컴포넌트(재)
        for (String usedId : usedComponentIds) {
            if (!result.contains(usedId)) {
                result.add(usedId);
            }
        }

        return result;
    }

    /**
     * 생성컴포넌트VO목록
     */
    private List<EditingPageCompVo> buildEditingPageCompVoList(List<String> componentIds, String tenantId) {
        if (CollectionUtils.isEmpty(componentIds)) {
            return Collections.emptyList();
        }

        List<Component> components = componentDao.getComponentsByIds(componentIds, tenantId);
        if (CollectionUtils.isEmpty(components)) {
            return Collections.emptyList();
        }

        return components.stream().map(this::convertToEditingPageCompVo).collect(Collectors.toList());
    }

    /**
     * 변환로컴포넌트VO
     */
    private EditingPageCompVo convertToEditingPageCompVo(Component component) {
        EditingPageCompVo vo = new EditingPageCompVo();
        vo.setComponentId(component.getComponentId());
        vo.setName(component.getName());
        return vo;
    }

    /**
     * 컴포넌트의icon및isLatest필드
     */
    private void setIconAndIsLatest(
            List<EditingPageCompVo> componentVoList, String robotId, Integer robotVersion, String tenantId) {
        if (CollectionUtils.isEmpty(componentVoList)) {
            return;
        }

        // 가져오기 compUseInfoMap
        List<CompUseInfo> compUseInfoList = componentRobotUseDao.getCompUseInfoList(robotId, robotVersion, tenantId);
        Map<String, CompUseInfo> compUseInfoMap =
                compUseInfoList.stream().collect(Collectors.toMap(CompUseInfo::getComponentId, info -> info));

        // 가져오기모든컴포넌트의ID목록
        List<String> componentIds =
                componentVoList.stream().map(EditingPageCompVo::getComponentId).collect(Collectors.toList());

        // 량가져오기컴포넌트의새버전정보(패키지icon)
        List<ComponentVersion> latestVersionInfoList =
                componentVersionDao.getLatestVersionInfoBatch(componentIds, tenantId);
        if (CollectionUtils.isEmpty(latestVersionInfoList)) {
            return;
        }

        // 를버전정보변환로Map, 방법조회
        Map<String, ComponentVersion> versionInfoMap = latestVersionInfoList.stream()
                .collect(Collectors.toMap(ComponentVersion::getComponentId, version -> version));

        // 가져오기봇에서지정버전아래사용의컴포넌트버전정보
        List<ComponentRobotUse> usedComponents =
                componentRobotUseDao.getByRobotIdAndVersion(robotId, robotVersion, tenantId);
        Map<String, ComponentRobotUse> usedComponentMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(usedComponents)) {
            usedComponentMap = usedComponents.stream()
                    .collect(Collectors.toMap(
                            ComponentRobotUse::getComponentId,
                            usedComponent -> usedComponent,
                            (existing, replacement) -> existing));
        }

        // icon및isLatest필드
        for (EditingPageCompVo vo : componentVoList) {
            String componentId = vo.getComponentId();
            ComponentVersion latestVersionInfo = versionInfoMap.get(componentId);

            // icon필드, 결과가사용경과, 사용현재사용의버전의icon
            if (compUseInfoMap.containsKey(componentId)) {
                CompUseInfo compUseInfo = compUseInfoMap.get(componentId);
                vo.setIcon(compUseInfo.getIcon());
            } else { // 결과가있음사용경과, 사용새버전의icon
                vo.setIcon(latestVersionInfo.getIcon());
            }

            // isLatest필드
            ComponentRobotUse usedComponent = usedComponentMap.get(componentId);
            if (usedComponent != null) {
                // 사용의버전및새버전
                Integer usedVersion = usedComponent.getComponentVersion();
                Integer latestVersion = latestVersionInfo.getVersion();
                vo.setIsLatest(usedVersion.equals(latestVersion) ? 1 : 0);
            } else {
                // 있음사용기록, 로새버전
                vo.setIsLatest(1);
            }
        }
    }

    @Override
    public AppResponse<EditingPageCompInfoVo> getEditingPageCompInfo(EditPageCompInfoDto queryDto) throws Exception {
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
        Integer robotVersion =
                getRobotVersion(queryDto.getRobotId(), queryDto.getMode(), queryDto.getRobotVersion(), new BaseDto());

        // 조회컴포넌트사용기록
        ComponentRobotUse componentRobotUse = componentRobotUseDao.getByRobotIdVersionAndComponentId(
                queryDto.getRobotId(), robotVersion, queryDto.getComponentId(), userId);

        // 생성컴포넌트VO
        EditingPageCompInfoVo result =
                buildEditingPageCompInfoVo(queryDto.getComponentId(), componentRobotUse, tenantId);

        return AppResponse.success(result);
    }

    /**
     * 생성컴포넌트VO
     */
    private EditingPageCompInfoVo buildEditingPageCompInfoVo(
            String componentId, ComponentRobotUse componentRobotUse, String tenantId) throws NoLoginException {
        EditingPageCompInfoVo vo = new EditingPageCompInfoVo();
        vo.setComponentId(componentId);
        AppResponse<User> res = rpaAuthFeign.getLoginUser();
        if (res == null || !res.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = res.getData();
        String userId = loginUser.getId();

        // 가져오기컴포넌트본정보
        Component component = componentDao.getComponentById(componentId, userId, tenantId);
        if (component == null) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "가져오기컴포넌트실패, 데이터예외");
        vo.setName(component.getName());

        // 가져오기컴포넌트새버전정보(패키지버전및)
        ComponentVersion latestVersionInfo = componentVersionDao.getLatestVersionInfo(componentId, tenantId);
        if (latestVersionInfo == null) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "컴포넌트전송되지 않았습니다경과버전, 데이터예외");

        Integer latestVersion = latestVersionInfo.getVersion();
        vo.setLatestVersion(latestVersion);
        vo.setIntroduction(latestVersionInfo.getIntroduction());

        // 결과가있음사용기록, 사용사용테이블중의버전
        if (componentRobotUse != null) {
            Integer usedVersion = componentRobotUse.getComponentVersion();
            if (usedVersion != null) {
                vo.setVersion(usedVersion);
                // 여부로새버전
                vo.setIsLatest(usedVersion.equals(latestVersion) ? 1 : 0);
            }
        } else {
            // 있음사용기록, 직선연결사용새버전
            vo.setVersion(latestVersion);
            vo.setIsLatest(1);
        }

        return vo;
    }

    /**
     * 가져오기봇버전
     */
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
    public AppResponse<List<CompManageVo>> getCompManageList(GetComponentUseDto queryDto) throws Exception {
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
        Integer robotVersion =
                getRobotVersion(queryDto.getRobotId(), queryDto.getMode(), queryDto.getVersion(), new BaseDto());

        // 1. 근거robotId및robotVersion조회모든의shown = 1의component, componentVersion테이블
        List<CompManageVo> resVoList = getComponentInfoList(tenantId, userId);
        if (CollectionUtils.isEmpty(resVoList)) {
            return AppResponse.success(Collections.emptyList());
        }

        // 2. 근거robotId및robotVersion조회componentRobotBlock테이블중모든의componentId, blocked필드
        setBlockedStatus(resVoList, queryDto.getRobotId(), robotVersion, tenantId);

        // 3. 근거robotId및robotVersion조회componentRobotUse테이블중모든의componentId및componentVersion, isLatest및version
        setUsageInfo(resVoList, queryDto.getRobotId(), robotVersion, tenantId);

        return AppResponse.success(resVoList);
    }

    /**
     * 가져오기컴포넌트정보목록
     */
    private List<CompManageVo> getComponentInfoList(String tenantId, String userId) {
        // 가져오기사용자권한내부가능가져오기의컴포넌트(shown = 1)
        List<Component> availableComponents = componentDao.getAvailableComponentsByUser(tenantId, userId);
        if (CollectionUtils.isEmpty(availableComponents)) {
            return Collections.emptyList();
        }

        // 통신경과componentVersion테이블, 필터링까지완료발송경과버전의componentList
        List<String> publishedComponentIds = componentVersionDao.getPublishedComponentIds(tenantId);
        if (CollectionUtils.isEmpty(publishedComponentIds)) {
            return Collections.emptyList();
        }

        // 필터링출력완료발송경과버전의컴포넌트
        List<Component> publishedComponents = availableComponents.stream()
                .filter(component -> publishedComponentIds.contains(component.getComponentId()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(publishedComponents)) {
            return Collections.emptyList();
        }

        // 량가져오기모든컴포넌트의새버전정보, 재복사IO
        List<String> componentIds =
                publishedComponents.stream().map(Component::getComponentId).collect(Collectors.toList());
        List<ComponentVersion> latestVersionInfoList =
                componentVersionDao.getLatestVersionInfoBatch(componentIds, tenantId);

        if (CollectionUtils.isEmpty(latestVersionInfoList)) {
            return Collections.emptyList();
        }

        // 를버전정보변환로Map, 방법조회
        Map<String, ComponentVersion> versionInfoMap = latestVersionInfoList.stream()
                .collect(Collectors.toMap(ComponentVersion::getComponentId, version -> version));

        // 그룹설치CompManageVo목록
        List<CompManageVo> result = new ArrayList<>();
        for (Component component : publishedComponents) {
            ComponentVersion latestVersionInfo = versionInfoMap.get(component.getComponentId());
            if (latestVersionInfo == null) {
                continue; // 결과가있음버전정보, 건너뛰기컴포넌트
            }

            CompManageVo vo = new CompManageVo();
            vo.setComponentId(component.getComponentId());
            vo.setName(component.getName());
            vo.setIcon(latestVersionInfo.getIcon());
            vo.setIntroduction(latestVersionInfo.getIntroduction());
            vo.setVersion(latestVersionInfo.getVersion());
            vo.setBlocked(0); // 미완료
            vo.setIsLatest(1); // 예새버전
            result.add(vo);
        }

        return result;
    }

    /**
     * 상태
     */
    private void setBlockedStatus(
            List<CompManageVo> componentInfoList, String robotId, Integer robotVersion, String tenantId) {
        List<String> blockedComponentIds =
                componentRobotBlockDao.getBlockedComponentIds(robotId, robotVersion, tenantId);
        if (CollectionUtils.isEmpty(blockedComponentIds)) {
            return;
        }

        for (CompManageVo vo : componentInfoList) {
            if (blockedComponentIds.contains(vo.getComponentId())) {
                vo.setBlocked(1);
            }
        }
    }

    /**
     * 사용정보
     */
    private void setUsageInfo(
            List<CompManageVo> componentInfoList, String robotId, Integer robotVersion, String tenantId) {
        List<ComponentRobotUse> usedComponents =
                componentRobotUseDao.getByRobotIdAndVersion(robotId, robotVersion, tenantId);
        if (CollectionUtils.isEmpty(usedComponents)) {
            return;
        }

        // 를사용정보변환로Map, , 높이가능
        Map<String, ComponentRobotUse> usedComponentMap = usedComponents.stream()
                .collect(Collectors.toMap(
                        ComponentRobotUse::getComponentId,
                        usedComponent -> usedComponent,
                        // 결과가있음재복사의componentId, 보관일개(관리위아니요해당있음재복사)
                        (existing, replacement) -> existing));

        // 관리사용완료의component
        for (CompManageVo vo : componentInfoList) {
            ComponentRobotUse usedComponent = usedComponentMap.get(vo.getComponentId());

            // 사용완료
            if (usedComponent != null) {
                Integer componentUseVersion = usedComponent.getComponentVersion();

                // 버전, isLatest및version
                if (componentUseVersion.equals(vo.getVersion())) vo.setIsLatest(1);
                else {
                    vo.setIsLatest(0);
                    vo.setLatestVersion(vo.getVersion()); // 새버전
                    vo.setVersion(componentUseVersion); // 사용버전
                }
            }
        }
    }
}