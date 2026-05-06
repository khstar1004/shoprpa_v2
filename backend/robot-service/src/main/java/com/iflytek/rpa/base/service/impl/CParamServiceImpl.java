package com.iflytek.rpa.base.service.impl;

import static com.iflytek.rpa.robot.constants.RobotConstant.DISPATCH;
import static com.iflytek.rpa.robot.constants.RobotConstant.EDITING;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.base.dao.CParamDao;
import com.iflytek.rpa.base.entity.CParam;
import com.iflytek.rpa.base.entity.dto.CParamDto;
import com.iflytek.rpa.base.entity.dto.CParamListDto;
import com.iflytek.rpa.base.entity.dto.ParamDto;
import com.iflytek.rpa.base.entity.dto.QueryParamDto;
import com.iflytek.rpa.base.service.CParamService;
import com.iflytek.rpa.base.service.handler.ParamHandlerFactory;
import com.iflytek.rpa.base.service.handler.ParamModeHandler;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.dto.RobotVersionDto;
import com.iflytek.rpa.task.service.ScheduleTaskRobotService;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author tzzhang
 * @date 2025/3/13 15:11
 */
@Service("CParamService")
@RequiredArgsConstructor
public class CParamServiceImpl implements CParamService {
    @Resource
    private final CParamDao cParamDao;

    @Resource
    private final IdWorker idWorker;

    @Resource
    private final RobotExecuteDao robotExecuteDao;

    @Autowired
    private final RobotDesignDao robotDesignDao;

    @Autowired
    private ScheduleTaskRobotService scheduleTaskRobotService;

    private final ParamHandlerFactory paramHandlerFactory;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<List<ParamDto>> getAllParams(QueryParamDto queryParamDto)
            throws JsonProcessingException, NoLoginException {
        validateBaseParams(queryParamDto);

        String mode = queryParamDto.getMode();
        ParamModeHandler handler = paramHandlerFactory.getHandler(mode);
        return handler.handle(queryParamDto);
    }

    private void validateBaseParams(QueryParamDto dto) {
        if (StringUtils.isBlank(dto.getRobotId())) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode(), "robotId는 비워 둘 수 없습니다");
        }

        if (dto.getMode() == null) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode(), "mode 매개변수를 지정해야 합니다");
        }

        if (DISPATCH.equals(dto.getMode())) {
            if (null == dto.getRobotVersion()) {
                throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode(), "dispatch 모드에서는 robotVersion을 비워 둘 수 없습니다");
            }
        }
    }
    //        List<CParam> cParamList = new ArrayList<CParam>();
    //        String mode = queryParamDto.getMode();
    //        //프론트엔드예아니요robot_version백엔드
    //        Integer robotVersion = queryParamDto.getRobotVersion();
    //        String processId = queryParamDto.getProcessId();
    //        //페이지조회, 조회새버전(0버전)구성 매개변수, 분프로세스조회, 다중호출, 직선까지모든프로세스의매개변수조회출력.
    //        if(EDIT_PAGE.equals(mode) || PROJECT_LIST.equals(mode)){
    //            //결과가있음버전조회새버전의데이터
    //            robotVersion = (null == robotVersion)?0:robotVersion;
    //            //상태버전
    //            cParamList = cParamDao.getAllParams(processId,robotId,robotVersion);
    //            return AppResponse.success(cParamToParamDto(cParamList));
    //        }
    //        if(EXECUTOR.equals(mode)){
    //            //에서실행기기실행;있음지정데이터(에서robot_execute테이블), 사용버전데이터, 마켓데이터;지정데이터단계높이
    //            RobotExecute robotExecute = robotExecuteDao.getRobotInfoByRobotId(robotId,userId,tenantId);
    //            if(null == robotExecute){
    //                throw new ServiceException((ErrorCodeEnum.E_SQL.getCode()),"실행할 로봇 정보를 찾을 수 없습니다");
    //            }
    //            String dataSource = robotExecute.getDataSource();
    //            String paramDetail = robotExecute.getParamDetail();
    //            //paramDetail결과가예null설명필요조회테이블
    //            if(null == paramDetail){
    //                if(dataSource.equals("market")){
    //                    //봇에서마켓중가져오기
    //                    if (StringUtils.isBlank(robotExecute.getMarketId()) ||
    // StringUtils.isBlank(robotExecute.getAppId()) || null == robotExecute.getAppVersion()){
    //                        throw new ServiceException((ErrorCodeEnum.E_SQL.getCode()),"로봇 마켓 정보가 올바르지 않습니다");
    //                    }else{
    //                        //에서마켓중가져오기봇매개변수
    //                        //봇공유까지마켓robot_id아니요해당수정변수, 예사용자가져오기봇robot_id발송수정변수
    //                        //가져오기기존봇robot_id
    //                        String robotIdMarket = cParamDao.getMarketRobotId(robotExecute);
    //                        //조회기존봇프로세스매개변수
    //                        processId = cParamDao.getMianProcessId(robotIdMarket,robotExecute.getAppVersion());
    //                        cParamList = cParamDao.getAllParams(processId,robotIdMarket,robotExecute.getAppVersion());
    //                        return AppResponse.success(cParamToParamDto(cParamList));
    //                    }
    //                }else if(dataSource.equals("create")){
    //                    //생성의봇
    //                    //조회봇사용의버전
    //                    robotVersion = cParamDao.getRobotVersion(robotId);
    //                    if(StringUtils.isBlank(processId)){
    //                        //실행기기매개변수조회아니요processId
    //                        //조회프로세스매개변수
    //                        //근거robotId및robotVersion조회프로세스id
    //                        processId = cParamDao.getMianProcessId(robotId,robotVersion);
    //                    }
    //                    //근거robotId및processId및robotVersion조회프로세스매개변수
    //                    cParamList = cParamDao.getSelfRobotParam(robotId,processId,robotVersion);
    //                    return AppResponse.success(cParamToParamDto(cParamList));
    //                }
    //            }
    //            //paramDetail아니요비어 있습니다, 있음사용자지정구성 매개변수
    //            // 사용 Jackson 를 JSON 문자열반대순서열로 List<CParam>
    //            ObjectMapper objectMapper = new ObjectMapper();
    //            cParamList = objectMapper.readValue(paramDetail, new TypeReference<List<CParam>>(){});
    //
    //            return AppResponse.success(cParamToParamDto(cParamList));
    //        }
    //
    //        if(TRIGGER.equals(mode)){
    //            //에서본예약 작업(트리거기기)실행, 있음지정데이터(에서task_robot테이블), 사용버전데이터, 마켓데이터;지정데이터단계높이
    //            //가져오기 일id
    //            Long taskRobotUniqueId = queryParamDto.getTaskRobotUniqueId();
    //            if(null == taskRobotUniqueId){
    //                throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode(),"적음예약 작업봇일id");
    //            }
    //            ScheduleTaskRobot taskRobot = scheduleTaskRobotService.queryById(taskRobotUniqueId);
    //            if (null == taskRobot){
    //                return AppResponse.success(new ArrayList<>());
    //            }
    //            String paramJson = taskRobot.getParamJson();
    //            if(StringUtils.isNotBlank(paramJson)){
    //                ObjectMapper objectMapper = new ObjectMapper();
    //                cParamList = objectMapper.readValue(paramJson, new TypeReference<List<CParam>>(){});
    //                return AppResponse.success(cParamToParamDto(cParamList));
    //            }
    //            //지정매개변수가 비어 있습니다, 조회데이터
    //            // 근거봇id조회봇
    //            RobotExecute robotExecute = robotExecuteDao.getRobotInfoByRobotId(robotId,userId,tenantId);
    //            if(null == robotExecute){
    //                throw new ServiceException((ErrorCodeEnum.E_SQL.getCode()),"실행할 로봇 정보를 찾을 수 없습니다");
    //            }
    //            String dataSource = robotExecute.getDataSource();
    //            if(dataSource.equals("market")){
    //                //봇에서마켓중가져오기
    //                if (StringUtils.isBlank(robotExecute.getMarketId()) ||
    // StringUtils.isBlank(robotExecute.getAppId()) || null == robotExecute.getAppVersion()){
    //                    throw new ServiceException((ErrorCodeEnum.E_SQL.getCode()),"로봇 마켓 정보가 올바르지 않습니다");
    //                }else{
    //                    //에서마켓중가져오기봇매개변수
    //                    //봇공유까지마켓robot_id아니요해당수정변수, 예사용자가져오기봇robot_id발송수정변수
    //                    //가져오기기존봇robot_id
    //                    String robotIdMarket = cParamDao.getMarketRobotId(robotExecute);
    //                    //조회기존봇프로세스매개변수
    //                    processId = cParamDao.getMianProcessId(robotIdMarket,robotExecute.getAppVersion());
    //                    cParamList = cParamDao.getAllParams(processId,robotIdMarket,robotExecute.getAppVersion());
    //                    return AppResponse.success(cParamToParamDto(cParamList));
    //                }
    //            }else if(dataSource.equals("create")){
    //                //생성의봇
    //                //조회봇사용의버전
    //                robotVersion = cParamDao.getRobotVersion(robotId);
    //                if(StringUtils.isBlank(processId)){
    //                    //실행기기매개변수조회아니요processId
    //                    //조회프로세스매개변수
    //                    //근거robotId및robotVersion조회프로세스id
    //                    processId = cParamDao.getMianProcessId(robotId,robotVersion);
    //                }
    //                //근거robotId및processId및robotVersion조회프로세스매개변수
    //                cParamList = cParamDao.getSelfRobotParam(robotId,processId,robotVersion);
    //                return AppResponse.success(cParamToParamDto(cParamList));
    //            }
    //        }
    //        return AppResponse.success(new ArrayList<>());
    //    }
    //
    //
    //    private List<ParamDto> cParamToParamDto(List<CParam> cParamList){
    //        List<ParamDto> result = new ArrayList<>();
    //        if(!CollectionUtil.isEmpty(cParamList)){
    //            for(CParam param:cParamList){
    //                ParamDto paramDto = new ParamDto();
    //                BeanUtils.copyProperties(param, paramDto);
    //                result.add(paramDto);
    //            }
    //        }
    //        return result;
    //    }

    @Override
    public AppResponse<String> addParam(CParamDto cParamDto) throws NoLoginException {
        // 추가매개변수 에서시있음, 으로가능으로매개변수버전예0버전
        CParam cParam = new CParam();
        BeanUtils.copyProperties(cParamDto, cParam);
        if (StringUtils.isEmpty(cParam.getProcessId())) {
            cParam.setProcessId(null);
        }
        if (StringUtils.isEmpty(cParam.getModuleId())) {
            cParam.setModuleId(null);
        }
        // 사용법완료id
        String cParamId = idWorker.nextId() + "";
        cParam.setId(cParamId);
        // 가져오기사용자id
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        cParam.setCreatorId(userId);
        cParam.setUpdaterId(userId);
        cParam.setCreateTime(new Date());
        cParam.setUpdateTime(new Date());
        cParam.setDeleted(0);
        // 결과가버전예버전0
        if (null == cParam.getRobotVersion()) {
            cParam.setRobotVersion(0);
        }
        checkSameName(cParam);
        cParamDao.addParam(cParam);

        AppResponse<User> resp = rpaAuthFeign.getLoginUser();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User user = resp.getData();
        String nowUserId = user.getId();
        robotDesignDao.updateTransformStatus(nowUserId, cParam.getRobotId(), null, EDITING);
        return AppResponse.success(cParamId);
    }

    @Override
    public AppResponse<Boolean> deleteParam(String id) throws NoLoginException {
        CParam paramInfoById = cParamDao.getParamInfoById(id);
        AppResponse<User> resp = rpaAuthFeign.getLoginUser();
        if (resp == null || !resp.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User user = resp.getData();
        String nowUserId = user.getId();
        robotDesignDao.updateTransformStatus(nowUserId, paramInfoById.getRobotId(), null, EDITING);
        cParamDao.deleteParam(id);
        return AppResponse.success(true);
    }

    @Override
    public AppResponse<Boolean> updateParam(CParamDto cParamDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        CParam cParam = new CParam();
        BeanUtils.copyProperties(cParamDto, cParam);
        if (StringUtils.isEmpty(cParam.getProcessId())) {
            cParam.setProcessId(null);
        }
        if (StringUtils.isEmpty(cParam.getModuleId())) {
            cParam.setModuleId(null);
        }

        checkSameName(cParam);

        // 조회새매개변수정보여부일
        CParam oldParamInfo = cParamDao.getParamInfoById(cParamDto.getId());

        // 결과가새매개변수정보아니요일, 를계획기기봇또는컴포넌트의상태로중
        if (!StringUtils.equals(oldParamInfo.getVarName(), cParam.getVarName())
                || !StringUtils.equals(oldParamInfo.getVarDescribe(), cParam.getVarDescribe())
                || !StringUtils.equals(oldParamInfo.getVarType(), cParam.getVarType())
                || !StringUtils.equals(oldParamInfo.getVarValue(), cParam.getVarValue())
                || oldParamInfo.getVarDirection() != cParam.getVarDirection()) {
            cParam.setUpdaterId(userId);
            cParam.setUpdateTime(new Date());
            cParamDao.updateParam(cParam);
            robotDesignDao.updateTransformStatus(userId, cParamDto.getRobotId(), null, EDITING);
        }

        return AppResponse.success(true);
    }

    private void checkSameName(CParam cParam) throws NoLoginException {
        String varName = cParam.getVarName();
        varName = varName.trim();
        if (StringUtils.isBlank(varName)) {
            throw new ServiceException("매개변수 이름은 비워 둘 수 없습니다");
        }
        cParam.setVarName(varName);
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        cParam.setCreatorId(userId);
        Long countRobot = cParamDao.countParamByName(cParam);
        if (countRobot > 0) {
            throw new ServiceException("이미 같은 이름의 매개변수가 있습니다. 다른 이름을 입력해 주세요");
        }
    }

    public AppResponse<Boolean> saveUserParam(CParamListDto cParamListDto)
            throws NoLoginException, JsonProcessingException {
        List<CParam> cParamList = cParamListDto.getParamList();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        RobotExecute robotExecuteTmp = robotExecuteDao.getRobotExecuteByTenantId(cParamListDto.getRobotId(), tenantId);
        if (robotExecuteTmp != null) // 수정 실행기기의매개변수
        return updateExecutorParamDetail(robotExecuteTmp, cParamListDto.getParamList());

        RobotExecute robotExecute = new RobotExecute();
        if (CollectionUtil.isEmpty(cParamList)) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode(), "매개변수 정보는 비워 둘 수 없습니다");
        }
        // 근거매개변수id조회봇id
        CParam paramInfo = cParamDao.getParamInfoById(cParamList.get(0).getId());
        String robotId = paramInfo.getRobotId();

        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();

        for (CParam cParam : cParamList) {
            cParam.setCreateTime(new Date());
            cParam.setUpdateTime(new Date());
            cParam.setCreatorId(userId);
        }
        robotExecute.setRobotId(robotId);
        robotExecute.setCreatorId(userId);
        AppResponse<String> res = rpaAuthFeign.getTenantId();
        if (res == null || res.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String nowTenantId = res.getData();
        robotExecute.setTenantId(nowTenantId);
        ObjectMapper mapper = new ObjectMapper();
        // 입력cParamList행순서열
        String cParamListJson = mapper.writeValueAsString(cParamList);
        robotExecute.setParamDetail(cParamListJson);
        robotExecute.setUpdateTime(new Date());
        robotExecuteDao.saveParamInfo(robotExecute);
        return AppResponse.success(true);
    }

    private AppResponse<Boolean> updateExecutorParamDetail(RobotExecute robotExecute, List<CParam> cParamList)
            throws NoLoginException, JsonProcessingException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        for (CParam cParam : cParamList) {
            cParam.setCreateTime(new Date());
            cParam.setUpdateTime(new Date());
            cParam.setCreatorId(userId);
        }
        ObjectMapper mapper = new ObjectMapper();
        String cParamListJson = mapper.writeValueAsString(cParamList);
        robotExecute.setParamDetail(cParamListJson);
        robotExecute.setUpdateTime(new Date());

        robotExecuteDao.saveParamInfo(robotExecute);

        return AppResponse.success(true);
    }

    public void createParamForCurrentVersion(String processId, RobotVersionDto robotVersionDto, Integer version) {
        // 조회0버전봇모든매개변수
        List<CParam> cParamList = cParamDao.getAllParams(processId, robotVersionDto.getRobotId(), version);
        for (CParam cParam : cParamList) {
            cParam.setId(idWorker.nextId() + "");
            // 업데이트버전
            cParam.setRobotVersion(robotVersionDto.getVersion());
        }
        if (!cParamList.isEmpty()) {
            cParamDao.createParamForCurrentVersion(cParamList);
        }
    }
}
