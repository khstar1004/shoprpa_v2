package com.iflytek.rpa.market.annotation;

import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.market.dao.AppMarketDictDao;
import com.iflytek.rpa.market.dao.AppMarketUserDao;
import com.iflytek.rpa.market.entity.AppMarketDict;
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
public class RightCheckAop {
    private static final Logger log = LoggerFactory.getLogger(RightCheckAop.class);

    @Autowired
    private AppMarketUserDao appMarketUserDao;

    @Autowired
    private AppMarketDictDao appMarketDictDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Around("@annotation(rightCheck)")
    public Object process(ProceedingJoinPoint joinPoint, RightCheck rightCheck) throws Throwable {
        // 결과가공가능코드에서DB중찾을 수 없습니다, 설명코드아니요
        String dictCode = appMarketDictDao.getCodeInfo(rightCheck.dictCode());
        if (null == dictCode) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "공유 코드가 등록되지 않았습니다");
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        String marketId = null;
        // 가져오기marketId
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object argValue = args[i];
            if (argValue instanceof String && "marketId".equals(parameter.getName())) {
                marketId = argValue.toString();
            } else if (argValue.getClass().equals(rightCheck.clazz())) {
                try {
                    marketId = argValue.getClass()
                            .getMethod("getMarketId")
                            .invoke(argValue)
                            .toString();
                } catch (Exception e) {
                    log.error("가져오기marketId실패,message:{}", e.getMessage());
                    return AppResponse.error(ErrorCodeEnum.E_PARAM);
                }
            } else {
                try {
                    marketId = argValue.getClass()
                            .getMethod("getMarketId")
                            .invoke(argValue)
                            .toString();
                } catch (Exception e) {
                    log.error("가져오기marketId실패,message:{}", e.getMessage());
                    return AppResponse.error(ErrorCodeEnum.E_PARAM);
                }
            }
        }

        if (null == marketId) {
            log.info("가져오기marketId실패");
            return AppResponse.error(ErrorCodeEnum.E_PARAM);
        }
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        // 조회역할
        String userType = appMarketUserDao.getUserTypeForCheck(userId, marketId);
        if (null == userType) {
            return AppResponse.error(ErrorCodeEnum.E_SQL, "마켓에서 해당 구성원을 찾을 수 없습니다");
        }
        // 조회권한
        AppMarketDict appMarketDict = appMarketDictDao.getDictValueByCodeAndType(rightCheck.dictCode(), userType);
        if (null != appMarketDict && "F".equals(appMarketDict.getDictValue())) {
            log.warn(
                    "사용자권한이 없습니다,userId:{},marketId:{},dictCode:{},dictValue:{}",
                    userId,
                    marketId,
                    rightCheck.dictCode(),
                    appMarketDict.getDictValue());
            return AppResponse.error(ErrorCodeEnum.E_SERVICE_POWER_LIMIT, "현재역할없음 " + appMarketDict.getName() + " 권한");
        }
        // 있음권한, 계속
        return joinPoint.proceed();
    }
}