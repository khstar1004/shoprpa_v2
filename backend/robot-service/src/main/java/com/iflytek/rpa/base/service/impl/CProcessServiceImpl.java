package com.iflytek.rpa.base.service.impl;

import static com.iflytek.rpa.base.constants.BaseConstant.PROCESS_TYPE_MODULE;
import static com.iflytek.rpa.base.constants.BaseConstant.PROCESS_TYPE_PROCESS;
import static com.iflytek.rpa.robot.constants.RobotConstant.EDITING;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.rpa.base.annotation.RobotVersionAnnotation;
import com.iflytek.rpa.base.dao.CParamDao;
import com.iflytek.rpa.base.dao.CProcessDao;
import com.iflytek.rpa.base.entity.CParam;
import com.iflytek.rpa.base.entity.CProcess;
import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.base.entity.dto.CProcessDto;
import com.iflytek.rpa.base.entity.dto.CreateProcessDto;
import com.iflytek.rpa.base.entity.dto.RenameProcessDto;
import com.iflytek.rpa.base.service.CModuleService;
import com.iflytek.rpa.base.service.CProcessService;
import com.iflytek.rpa.base.service.NextName;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.component.dao.ComponentDao;
import com.iflytek.rpa.component.entity.Component;
import com.iflytek.rpa.market.constants.AuditConstant;
import com.iflytek.rpa.market.dao.AppApplicationDao;
import com.iflytek.rpa.market.dao.AppApplicationTenantDao;
import com.iflytek.rpa.market.dao.AppMarketResourceDao;
import com.iflytek.rpa.market.entity.AppApplication;
import com.iflytek.rpa.market.entity.AppApplicationTenant;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.RobotDesign;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로세스id데이터(CProcess)테이블서비스유형
 *
 * @author mjren
 * @since 2024-10-09 17:11:14
 */
@Service("cProcessService")
public class CProcessServiceImpl extends NextName implements CProcessService {
    @Resource
    private CProcessDao cProcessDao;

    @Autowired
    private RobotDesignDao robotDesignDao;

    @Autowired
    private ComponentDao componentDao;

    @Autowired
    private RobotExecuteDao robotExecuteDao;

    @Autowired
    private RobotVersionDao robotVersionDao;

    @Autowired
    private AppMarketResourceDao appMarketResourceDao;

    @Autowired
    private CModuleService cModuleService;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private AppApplicationDao appApplicationDao;

    @Autowired
    private AppApplicationTenantDao appApplicationTenantDao;

    @Autowired
    private CParamDao cParamDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<String> getProcessNextName(String robotId) {
        BaseDto baseDto = new BaseDto();
        baseDto.setRobotId(robotId);
        baseDto.setRobotVersion(0);
        // 가져오기프로세스이름목록
        List<CProcess> processList = cProcessDao.getProcessNameList(baseDto);
        Set<String> nameSet = processList.stream().map(CProcess::getProcessName).collect(Collectors.toSet());

        // 완료아래일개가능사용의하위 프로세스이름
        int nextNumber = 1;
        while (nameSet.contains("하위 프로세스" + nextNumber)) {
            nextNumber++;
        }
        String nextName = "하위 프로세스" + nextNumber;

        // 결과가있음프로세스, 이면사용"프로세스"로이름
        if (!nameSet.contains("프로세스")) {
            nextName = "프로세스";
        }
        return AppResponse.success(nextName);
    }

    public AppResponse<Map> createNewProcess(CreateProcessDto processDto) throws NoLoginException {
        CProcess searchDto = new CProcess();
        BeanUtil.copyProperties(processDto, searchDto);
        searchDto.setRobotVersion(0);
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        searchDto.setCreatorId(userId);
        // 조회이름프로세스
        Integer count = cProcessDao.countProcessByName(searchDto);
        if (count > 0) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "완료저장된 이름의하위 프로세스이름, 요청다시 명령이름");
        }
        String processId = idWorker.nextId() + "";
        // 생성프로세스
        CProcess process = new CProcess();
        process.setRobotId(processDto.getRobotId());
        process.setRobotVersion(0);
        process.setProcessName(processDto.getProcessName());
        process.setProcessContent(processDto.getProcessContent());
        process.setProcessId(processId);
        process.setCreatorId(userId);
        process.setCreateTime(new Date());
        process.setUpdaterId(userId);
        process.setUpdateTime(new Date());
        process.setDeleted(0);
        // 저장프로세스
        cProcessDao.insert(process);

        // 반환결과
        Map<String, String> responseData = new HashMap<>();
        responseData.put("processId", processId);
        return AppResponse.success(responseData);
    }

    @Override
    public AppResponse<Boolean> renameProcess(RenameProcessDto processDto) throws NoLoginException {
        CProcess searchDto = new CProcess();
        BeanUtil.copyProperties(processDto, searchDto);
        searchDto.setRobotVersion(0);
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        searchDto.setCreatorId(userId);
        // 조회이름프로세스
        Integer count = cProcessDao.countProcessByName(searchDto);
        if (count > 0) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "완료저장된 이름의하위 프로세스이름, 요청다시 명령이름");
        }
        cProcessDao.renameProcess(searchDto);
        return AppResponse.success(true);
    }

    @Override
    public AppResponse<?> getAllProcessData(CProcess process) {
        String robotId = process.getRobotId();
        Integer version = process.getRobotVersion();
        if (null == version || StringUtils.isBlank(robotId)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE);
        }
        List<CProcess> result = cProcessDao.getAllProcessDataByRobotId(robotId, version);
        return AppResponse.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> saveProcessContent(CProcessDto process) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        process.setCreatorId(userId);
        BaseDto baseDto = new BaseDto();
        BeanUtil.copyProperties(process, baseDto);
        CProcess oldProcess = cProcessDao.getProcessById(baseDto);
        if (null == oldProcess) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "프로세스 데이터를 찾을 수 없습니다");
        }
        String oldProcessContent = oldProcess.getProcessContent();
        String newProcessContent = process.getProcessJson();
        if ((null == oldProcessContent || null == newProcessContent) || !oldProcessContent.equals(newProcessContent)) {
            // 내용발송완료변수
            // 를계획기기봇또는컴포넌트의상태로중
            robotDesignDao.updateTransformStatus(userId, process.getRobotId(), null, EDITING);
            Integer i = componentDao.updateTransformStatus(userId, process.getRobotId(), null, EDITING);
        }
        if (null != newProcessContent) {
            // 제한제어프로세스데이터의크기
            // 가져오기문자열의문자길이정도
            int byteLength = newProcessContent.getBytes().length;
            // 를문자길이정도변환로문자(MB)
            double megabytes = byteLength / (1024.0 * 1024.0);
            if (megabytes > 14) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "프로세스 데이터는 15MB를 초과할 수 없습니다");
            }
        }
        // 결과가있음변경수정, 이면아니요변경수정상태
        cProcessDao.updateProcessContent(process);
        return AppResponse.success(true);
    }

    @Override
    @RobotVersionAnnotation
    public AppResponse<?> getProcessDataByProcessId(BaseDto baseDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();

        // String tenantId = TenantUtils.getTenantId();
        // 권한조회
        //        AppResponse<?> permissionCheck = checkRobotPermission(baseDto.getRobotId(), userId, tenantId);
        //        if (!permissionCheck.ok()) {
        //            return permissionCheck;
        //        }

        baseDto.setCreatorId(userId);
        CProcess process = cProcessDao.getProcessById(baseDto);
        if (null == process || null == process.getProcessContent()) {
            return AppResponse.success("");
        }
        return AppResponse.success(process.getProcessContent());
    }

    /**
     * 조회봇권한
     */
    private AppResponse<?> checkRobotPermission(String robotId, String userId, String tenantId) {
        // 조회여부열기시작완료검토공가능
        if (!isAuditFunctionEnabled(tenantId)) {
            return AppResponse.success("검토공가능미완료열기시작");
        }

        // 가져오기봇정보
        RobotDesign robotDesign = robotDesignDao.getRobotInfoAll(robotId, tenantId);
        Component component = componentDao.getComponentById(robotId, userId, tenantId);
        if (robotDesign == null && component == null) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "수정봇또는찾을 수 없습니다");
        }

        // 결과가예생성의봇, 직선연결통신경과
        if (robotDesign != null && "create".equals(robotDesign.getDataSource())) {
            return AppResponse.success("생성의봇");
        }

        // 결과가예생성의컴포넌트, 직선연결통신경과
        if (component != null && "create".equals(component.getDataSource())) {
            return AppResponse.success("생성의컴포넌트");
        }

        // 결과가예모듈의또는가져오기의봇, 필요조회권한
        if ("market".equals(robotDesign.getDataSource())) {
            return checkMarketRobotPermission(robotId, userId, tenantId);
        }

        return AppResponse.success("권한조회통신경과");
    }

    /**
     * 조회마켓봇의권한
     */
    private AppResponse<?> checkMarketRobotPermission(String robotId, String userId, String tenantId) {
        // 조회여부있음대기검토의위신청
        AppApplication pendingApplication = appApplicationDao.selectOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, robotId)
                .eq(AppApplication::getCreatorId, userId)
                .eq(AppApplication::getApplicationType, "release")
                .eq(AppApplication::getStatus, AuditConstant.AUDIT_STATUS_PENDING)
                .eq(AppApplication::getDeleted, 0));

        if (pendingApplication != null) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "봇에서위검토중, 시불가사용");
        }

        // 가져오기완료검토통신경과의위신청
        AppApplication approvedReleaseApplication = appApplicationDao.selectOne(new LambdaQueryWrapper<AppApplication>()
                .eq(AppApplication::getRobotId, robotId)
                .eq(AppApplication::getCreatorId, userId)
                .eq(AppApplication::getApplicationType, "release")
                .eq(AppApplication::getStatus, AuditConstant.AUDIT_STATUS_APPROVED)
                .eq(AppApplication::getDeleted, 0)
                .orderByDesc(AppApplication::getCreateTime)
                .last("LIMIT 1"));

        if (approvedReleaseApplication == null) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "봇통과하지 못했습니다위검토, 불가사용");
        }

        String securityLevel = approvedReleaseApplication.getSecurityLevel();
        if (StringUtils.isBlank(securityLevel)) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "봇비밀단계정보 실패, 불가사용");
        }

        // 조회색상비밀단계여부경과
        if ("red".equals(securityLevel)) {
            if (approvedReleaseApplication.getExpireTime() != null
                    && approvedReleaseApplication.getExpireTime().before(new Date())) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "봇비밀단계완료경과, 불가사용");
            }
        }

        // 조회색상비밀단계권한
        if ("yellow".equals(securityLevel)) {
            String allowedDept = approvedReleaseApplication.getAllowedDept();
            if (StringUtils.isNotBlank(allowedDept)) {
                // 조회사용자여부에서허용의모듈내부
                AppResponse<String> deptIdRes = rpaAuthFeign.getDeptIdByUserId(userId, tenantId);
                if (!deptIdRes.ok()) throw new ServiceException("rpa-auth 서비스가 준비되지 않았습니다");
                String userDeptId = deptIdRes.getData();
                if (!allowedDept.contains(userDeptId)) {
                    // 조회여부있음통신경과의사용신청
                    AppApplication useApplication = appApplicationDao.selectOne(new LambdaQueryWrapper<AppApplication>()
                            .eq(AppApplication::getRobotId, robotId)
                            .eq(AppApplication::getCreatorId, userId)
                            .eq(AppApplication::getApplicationType, "use")
                            .eq(AppApplication::getStatus, AuditConstant.AUDIT_STATUS_APPROVED)
                            .eq(AppApplication::getDeleted, 0)
                            .last("LIMIT 1"));

                    if (useApplication == null) {
                        return AppResponse.error(ErrorCodeEnum.E_SERVICE, "허용된 모듈이 아닙니다. 사용 신청이 승인되어야 해당 봇을 사용할 수 있습니다");
                    }
                }
            }
        }

        return AppResponse.success("권한조회통신경과");
    }

    /**
     * 조회여부열기시작완료위검토공가능
     */
    private boolean isAuditFunctionEnabled(String tenantId) {
        AppApplicationTenant currentConfig = appApplicationTenantDao.getByTenantId(tenantId);
        if (currentConfig == null) {
            return false; // 닫기, 있음공가능
        }
        return currentConfig.getAuditEnable() == 1;
    }

    @Override
    @RobotVersionAnnotation
    public AppResponse<?> getProcessNameList(BaseDto baseDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        baseDto.setCreatorId(userId);
        List<CProcess> processNameList = cProcessDao.getProcessNameList(baseDto);
        // 를프로세스정렬에서일개
        processNameList.sort((p1, p2) -> {
            if ("프로세스".equals(p1.getProcessName())) {
                // p1 예 "프로세스", 에서전
                return -1;
            } else if ("프로세스".equals(p2.getProcessName())) {
                // p2 예 "프로세스", 에서전
                return 1;
            } else {
                // 요소 name 정렬
                return p1.getProcessName().compareTo(p2.getProcessName());
            }
        });
        return AppResponse.success(processNameList);
    }

    @Override
    public AppResponse<?> copySubProcess(String robotId, String processId, String type) {
        Map<String, String> result = new HashMap<>();
        if (PROCESS_TYPE_PROCESS.equals(type)) {
            // 조회기존프로세스데이터
            BaseDto baseDto = new BaseDto();
            baseDto.setRobotId(robotId);
            baseDto.setRobotVersion(0);
            baseDto.setProcessId(processId);
            CProcess process = cProcessDao.getProcessById(baseDto);
            if (null == process) {
                throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "프로세스 데이터를 찾을 수 없습니다");
            }
            String processName = process.getProcessName();
            baseDto.setName(processName);
            // 제품본이름
            String nextName = createNextName(baseDto, processName + "본");
            // 복사프로세스
            process.setProcessId(idWorker.nextId() + "");
            process.setProcessName(nextName);
            process.setCreateTime(new Date());
            process.setUpdateTime(new Date());
            cProcessDao.insert(process);

            String newProcessId = process.getProcessId();
            copyCParam(newProcessId, processId, process.getRobotId(), process.getRobotVersion());

            result.put("id", process.getProcessId());
            result.put("name", process.getProcessName());
        } else if (PROCESS_TYPE_MODULE.equals(type)) {
            return AppResponse.success(cModuleService.copyCodeModule(robotId, processId));
        }

        return AppResponse.success(result);
    }

    private void copyCParam(String newProcessId, String oldProcessId, String robotId, Integer version) {
        List<CParam> params = cParamDao.getAllParams(oldProcessId, robotId, version);
        for (CParam cParam : params) {
            cParam.setId(idWorker.nextId() + "");
            cParam.setProcessId(newProcessId);
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
        return cProcessDao.getProcessNameListByPrefix(baseDto);
    }

    @Override
    public AppResponse<Boolean> deleteProcess(CProcessDto processDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        processDto.setCreatorId(userId);
        boolean result = cProcessDao.deleteProcessByProcessId(processDto);
        return AppResponse.success(result);
    }
}