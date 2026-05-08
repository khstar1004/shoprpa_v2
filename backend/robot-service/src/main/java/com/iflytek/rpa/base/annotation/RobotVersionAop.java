package com.iflytek.rpa.base.annotation;

import static com.iflytek.rpa.robot.constants.RobotConstant.*;

import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.market.dao.AppMarketResourceDao;
import com.iflytek.rpa.market.entity.AppMarketResource;
import com.iflytek.rpa.market.entity.MarketDto;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.utils.exception.NoDataException;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RobotVersionAop {
    private static final Logger log = LoggerFactory.getLogger(RobotVersionAop.class);

    @Autowired
    private RobotExecuteDao robotExecuteDao;

    @Autowired
    private RobotVersionDao robotVersionDao;

    @Autowired
    private AppMarketResourceDao appMarketResourceDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Around("@annotation(robotVersionAnnotation)")
    public Object process(ProceedingJoinPoint joinPoint, RobotVersionAnnotation robotVersionAnnotation)
            throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        BaseDto baseDto = new BaseDto();
        String robotId = null;
        String mode = null;
        Integer robotVersion = null;

        // 가져오기또는수정 robotId 및 version
        for (int i = 0; i < parameters.length; i++) {
            Object argValue = args[i];
            if (argValue.getClass().equals(robotVersionAnnotation.clazz())) {
                try {
                    robotId = argValue.getClass()
                            .getMethod("getRobotId")
                            .invoke(argValue)
                            .toString();
                    mode = argValue.getClass()
                            .getMethod("getMode")
                            .invoke(argValue)
                            .toString();
                    robotVersion = (Integer)
                            argValue.getClass().getMethod("getRobotVersion").invoke(argValue);
                    try {
                        getRobotIdAndVersion(robotId, robotVersion, mode, baseDto);
                    } catch (NoDataException e) {
                        return AppResponse.error(ErrorCodeEnum.E_SQL, e.getMessage());
                    }
                    // 수정 robotId 의값
                    argValue.getClass().getMethod("setRobotId", String.class).invoke(argValue, baseDto.getRobotId());
                    // 수정 version 의값
                    argValue.getClass()
                            .getMethod("setRobotVersion", Integer.class)
                            .invoke(argValue, baseDto.getRobotVersion());

                } catch (Exception e) {
                    log.error("가져오기또는수정 robotId 또는 version 실패, message:{}", e.getMessage(), e);
                    return AppResponse.error(ErrorCodeEnum.E_SERVICE, "가져오기또는수정봇정보실패");
                }
            }
        }

        // 호출목록 방법법, 수정후의매개변수
        return joinPoint.proceed(args);
    }

    /**
     * 봇프로세스대기데이터에서가져오기: 1, 계획기기및, 에서버전0가져오기, 2, 실행기기생성의, 에서사용버전가져오기 3, 실행기기에서마켓가져오기의봇, 가져오기appVersion및의robotId
     *
     * @param robotId
     * @param mode
     * @param baseDto
     * @throws NoDataException
     * @throws NoLoginException
     */
    private void getRobotIdAndVersion(String robotId, Integer robotVersion, String mode, BaseDto baseDto)
            throws NoDataException, NoLoginException {
        if (null != robotVersion) {
            baseDto.setRobotVersion(robotVersion);
            baseDto.setRobotId(robotId);
            return;
        }

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
        if (PROJECT_LIST.equals(mode) || EDIT_PAGE.equals(mode)) {
            baseDto.setRobotId(robotId);
            baseDto.setRobotVersion(0);
        } else if (DISPATCH.equals(mode)) {
            // 스케줄링방식업로드봇버전
            if (null == robotVersion) {
                throw new NoDataException("스케줄링 작업의 봇 버전을 찾을 수 없습니다");
            }
            baseDto.setRobotId(robotId);
            baseDto.setRobotVersion(robotVersion);
        } else {
            // 결과가예실행기기또는예약 작업, 분로생성의(조회사용버전), 가져오기의(조회appVersion)
            RobotExecute robotExecute = robotExecuteDao.queryByRobotId(robotId, userId, tenantId);
            if (null == robotExecute) {
                throw new NoDataException("실행할 봇을 찾을 수 없습니다");
            }
            String dataSource = robotExecute.getDataSource();
            if (CREATE.equals(dataSource)) {
                // 생성의, 조회사용버전
                RobotVersion version = robotVersionDao.getOriEnableVersion(robotId, userId, tenantId);
                if (null == version || null == version.getVersion()) {
                    throw new NoDataException("사용 가능한 봇 버전 정보가 없습니다");
                }
                baseDto.setRobotId(robotId);
                baseDto.setRobotVersion(version.getVersion());

            } else if (MARKET.equals(dataSource)) {
                // 마켓가져오기의, 조회appVersion및의robotId
                String marketId = robotExecute.getMarketId();
                String appId = robotExecute.getAppId();
                AppMarketResource appMarketResource =
                        appMarketResourceDao.getAppInfoByAppId(new MarketDto(tenantId, marketId, appId));
                if (null == appMarketResource || null == appMarketResource.getRobotId()) {
                    throw new NoDataException("해당봇닫기 의마켓사용정보 실패");
                }
                baseDto.setRobotId(appMarketResource.getRobotId());
                if (null == robotExecute.getAppVersion()) {
                    throw new NoDataException("해당봇닫기 의사용버전정보 실패");
                }
                baseDto.setRobotVersion(robotExecute.getAppVersion());
            } else if (DEPLOY.equals(dataSource)) {
                // 모듈의, appIdrobotId,appVersionrobotVersion
                baseDto.setRobotId(robotExecute.getAppId());
                baseDto.setRobotVersion(robotExecute.getAppVersion());
            }
        }
    }
}
