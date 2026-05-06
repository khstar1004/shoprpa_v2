package com.iflytek.rpa.base.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.EDITING;

import com.iflytek.rpa.base.annotation.RobotVersionAnnotation;
import com.iflytek.rpa.base.dao.CModuleDao;
import com.iflytek.rpa.base.dao.CParamDao;
import com.iflytek.rpa.base.entity.CModule;
import com.iflytek.rpa.base.entity.CParam;
import com.iflytek.rpa.base.entity.CProcess;
import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.base.entity.dto.CreateModuleDto;
import com.iflytek.rpa.base.entity.dto.ProcessModuleListDto;
import com.iflytek.rpa.base.entity.dto.RenameModuleDto;
import com.iflytek.rpa.base.entity.vo.ModuleListVo;
import com.iflytek.rpa.base.entity.vo.OpenModuleVo;
import com.iflytek.rpa.base.entity.vo.ProcessModuleListVo;
import com.iflytek.rpa.base.service.CModuleService;
import com.iflytek.rpa.base.service.NextName;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import com.iflytek.rpa.robot.entity.dto.SaveModuleDto;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service("CModuleService")
public class CModuleServiceImpl extends NextName implements CModuleService {

    @Resource
    private CModuleDao cModuleDao;

    @Autowired
    private CModuleServiceImpl self;

    @Resource
    private RobotDesignDao robotDesignDao;

    @Autowired
    private CParamDao cParamDao;

    @Resource
    private IdWorker idWorker;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    public final int CONTENT_MAX_LENGTH = 10000000; // 데이터베이스중medium text 지원의대길이정도예 16777215

    // 새완료의코드
    public final String initContent = "from typing import Any\n" + "from astronverse.workflowlib import print, logger\n"
            + "from .package import element, element_vision, gv\n"
            + "\n"
            + "\n"
            + "def main(args) -> Any:\n"
            + "    return True";

    @Override
    public AppResponse<List<ProcessModuleListVo>> processModuleList(ProcessModuleListDto queryDto)
            throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();

        // 가져오기 코드모듈목록
        List<ModuleListVo> moduleList = getModuleList(queryDto, userId);

        List<ProcessModuleListVo> resVoList = getResVoList(moduleList, userId, queryDto);

        return AppResponse.success(resVoList);
    }

    @Override
    public AppResponse<List<ModuleListVo>> moduleList(ProcessModuleListDto queryDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();

        List<ModuleListVo> moduleList = getModuleList(queryDto, userId);

        return AppResponse.success(moduleList);
    }

    @Override
    public AppResponse<OpenModuleVo> create(CreateModuleDto queryDto) throws NoLoginException {

        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        String robotId = queryDto.getRobotId();

        if (StringUtils.isBlank(robotId)) throw new IllegalArgumentException();

        String newModuleId = String.valueOf(idWorker.nextId());

        // 검증이름
        if (checkNewNameDup(queryDto.getModuleName(), robotId, userId)) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "이름재복사, 요청다시 명령이름");
        }

        CModule cModule = new CModule();
        cModule.setModuleId(newModuleId);
        cModule.setCreatorId(userId);
        cModule.setModuleName(queryDto.getModuleName());
        cModule.setModuleContent(initContent);
        cModule.setRobotVersion(0);
        cModule.setRobotId(robotId);
        cModule.setCreatorId(userId);
        cModule.setUpdaterId(userId);

        cModuleDao.insert(cModule);

        OpenModuleVo resVo = new OpenModuleVo();
        resVo.setModuleContent(cModule.getModuleContent());
        resVo.setModuleId(newModuleId);
        resVo.setRobotId(robotId);
        resVo.setRobotVersion(0);

        return AppResponse.success(resVo);
    }

    @Override
    public AppResponse<String> newModuleName(String robotId) throws NoLoginException {
        if (StringUtils.isBlank(robotId)) throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "매개변수 실패");
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        return AppResponse.success(getModuleInitName(robotId, 0, userId));
    }

    @Override
    @RobotVersionAnnotation
    public AppResponse<OpenModuleVo> open(BaseDto baseDto, String moduleId) throws NoLoginException {
        Integer robotVersion = 0;
        if (baseDto.getRobotVersion() != null) {
            robotVersion = baseDto.getRobotVersion();
        }
        String robotId = baseDto.getRobotId();
        CModule module = cModuleDao.getModule(moduleId, robotId, robotVersion);
        if (module == null) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "해당 모듈을 찾을 수 없습니다");
        OpenModuleVo resVo = new OpenModuleVo();
        resVo.setModuleId(moduleId);
        resVo.setModuleContent(module.getModuleContent());
        resVo.setRobotId(robotId);
        resVo.setRobotVersion(robotVersion);
        resVo.setBreakpoint(module.getBreakpoint());
        return AppResponse.success(resVo);
    }

    @Override
    public AppResponse<Boolean> delete(String moduleId) throws NoLoginException, SQLException {

        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();

        boolean b = cModuleDao.deleteOneModule(moduleId, userId);

        if (b) return AppResponse.success(true);
        else throw new SQLException();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> save(SaveModuleDto queryDto) throws NoLoginException, SQLException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        String moduleId = queryDto.getModuleId();
        String robotId = queryDto.getRobotId();
        String newModuleContent = queryDto.getModuleContent();
        String breakpoint = queryDto.getBreakpoint();
        if (StringUtils.length(newModuleContent) > CONTENT_MAX_LENGTH) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "모듈코드길이정도초과길이");
        }

        CModule module = cModuleDao.getOneModule(moduleId, userId, robotId);
        if (module == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "해당 모듈을 찾을 수 없어 업데이트할 수 없습니다");
        }
        String oldModuleContent = module.getModuleContent();
        // 업데이트 robotDesign상태로중상태
        if (oldModuleContent != null && newModuleContent != null && !oldModuleContent.equals(newModuleContent)) {
            robotDesignDao.updateTransformStatus(userId, robotId, null, EDITING);
        }
        Long id = cModuleDao.getIdByModuleId(moduleId, userId, robotId);
        if (id == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "해당 모듈을 찾을 수 없어 업데이트할 수 없습니다");
        }
        // 근거 id 행업데이트
        boolean b = cModuleDao.updateModuleContentById(id, newModuleContent, breakpoint);

        if (b) return AppResponse.success(true);
        else throw new SQLException();
    }

    @Override
    public AppResponse<Boolean> rename(RenameModuleDto queryDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        String moduleId = queryDto.getModuleId();
        Integer robotVersion = 0;
        String robotId = queryDto.getRobotId();
        String newName = queryDto.getModuleName();

        List<String> allModuleName = cModuleDao.getAllModuleNameExpSelf(robotId, robotVersion, userId, moduleId);

        if (allModuleName.contains(newName)) throw new ServiceException("이름재복사, 요청다시 입력");
        else return AppResponse.success(cModuleDao.updateModuleName(userId, moduleId, newName));
    }

    public String getModuleInitName(String robotId, Integer robotVersion, String userId) {
        String newModuleName = "코드모듈1";
        List<CModule> allModuleList = cModuleDao.getAllModuleList(robotId, robotVersion, userId);

        if (!CollectionUtils.isEmpty(allModuleList)) {
            List<String> moduleNameList =
                    allModuleList.stream().map(CModule::getModuleName).collect(Collectors.toList());
            List<String> nameAfterFilter = moduleNameList.stream()
                    .filter(name -> name.contains("코드모듈"))
                    .collect(Collectors.toList());
            newModuleName = newModuleName(nameAfterFilter);
        }

        return newModuleName;
    }

    public String newModuleName(List<String> nameAfterFilter) {
        Integer index = 1;
        while (true) {
            String newName = "코드모듈" + index;
            if (nameAfterFilter.contains(newName)) index++;
            else return newName;
        }
    }

    private boolean checkNewNameDup(String newName, String robotId, String userId) {
        List<String> allModuleName = cModuleDao.getAllModuleName(robotId, 0, userId);
        if (allModuleName.contains(newName)) return true;
        else return false;
    }

    private List<ProcessModuleListVo> getResVoList(
            List<ModuleListVo> moduleList, String userId, ProcessModuleListDto queryDto) {
        BaseDto baseDto = new BaseDto();
        BeanUtils.copyProperties(queryDto, baseDto);
        self.getVersion(baseDto);
        String robotId = baseDto.getRobotId();
        Integer robotVersion = 0;

        List<ProcessModuleListVo> resVoList = new ArrayList<>();

        List<CProcess> processList = cModuleDao.getAllProcessListWithOutUserId(robotId, robotVersion);

        // 프로세스데이터
        for (CProcess process : processList) {
            ProcessModuleListVo resVo = new ProcessModuleListVo();

            resVo.setName(process.getProcessName());
            resVo.setResourceId(process.getProcessId());
            resVo.setResourceCategory("process");

            resVoList.add(resVo);
        }

        // 모듈데이터
        for (ModuleListVo moduleVo : moduleList) {
            ProcessModuleListVo resVo = new ProcessModuleListVo();

            resVo.setName(moduleVo.getName());
            resVo.setResourceId(moduleVo.getModuleId());
            resVo.setResourceCategory("module");

            resVoList.add(resVo);
        }

        return resVoList;
    }

    @RobotVersionAnnotation
    public void getVersion(BaseDto dto) {}

    private List<ModuleListVo> getModuleList(ProcessModuleListDto queryDto, String userId) {
        BaseDto baseDto = new BaseDto();
        BeanUtils.copyProperties(queryDto, baseDto);
        self.getVersion(baseDto);

        String robotId = baseDto.getRobotId();
        Integer robotVersion = 0;

        List<CModule> allModuleList = cModuleDao.getAllModuleListWithoutUserId(robotId, robotVersion);

        List<ModuleListVo> res = new ArrayList<>();

        for (CModule cModule : allModuleList) {

            ModuleListVo moduleListVo = new ModuleListVo();
            moduleListVo.setModuleId(cModule.getModuleId());
            moduleListVo.setName(cModule.getModuleName());

            res.add(moduleListVo);
        }

        return res;
    }

    public Map<String, String> copyCodeModule(String robotId, String processOrModuleId) {
        // 조회기존코드모듈데이터
        BaseDto baseDto = new BaseDto();
        baseDto.setRobotId(robotId);
        baseDto.setRobotVersion(0);
        baseDto.setId(processOrModuleId);
        CModule codeModule = cModuleDao.getCodeModeById(baseDto);
        if (null == codeModule) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "프로세스 데이터를 찾을 수 없습니다");
        }

        String newModuleId = idWorker.nextId() + "";
        String moduleName = codeModule.getModuleName();
        baseDto.setName(moduleName);
        // 제품본이름
        String nextName = createNextName(baseDto, moduleName + "본");
        // 복사프로세스
        codeModule.setModuleId(newModuleId);
        codeModule.setModuleName(nextName);
        codeModule.setCreateTime(new Date());
        codeModule.setUpdateTime(new Date());
        cModuleDao.insert(codeModule);
        // 복사구성 매개변수
        copyCParam(newModuleId, processOrModuleId, robotId, 0);

        Map<String, String> result = new HashMap<>();
        result.put("id", codeModule.getModuleId());
        result.put("name", codeModule.getModuleName());
        return result;
    }

    private void copyCParam(String newProcessId, String oldProcessId, String robotId, Integer version) {
        List<CParam> params = cParamDao.getAllParamsByModuleId(oldProcessId, robotId, version);
        if (params.isEmpty()) return;
        for (CParam cParam : params) {
            cParam.setId(idWorker.nextId() + "");
            cParam.setModuleId(newProcessId);
            cParam.setCreateTime(new Date());
            cParam.setUpdateTime(new Date());
            cParam.setDeleted(0);
        }
        if (!params.isEmpty()) {
            cParamDao.insertParamBatch(params);
        }
    }

    @Override
    public List<String> getNameList(BaseDto baseDto) {
        return cModuleDao.getModuleNameListByPrefix(baseDto);
    }
}