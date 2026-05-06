package com.iflytek.rpa.base.annotation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * API로그
 * 전체영역의방법법호출로그기록공가능
 */
@Aspect
@Component
@Slf4j
public class ApiLogAspect {

    /**
     * 시간형식기기, 사용시스템일로그시간형식
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 지정: 모든Controller의방법법
     */
    @Pointcut("execution(* com.iflytek.rpa..*.controller..*(..))")
    public void controllerPointcut() {}

    /**
     * 지정: 모든비고@RestController또는@Controller의유형중의방법법
     */
    @Pointcut(
            "within(@org.springframework.web.bind.annotation.RestController *) || within(@org.springframework.stereotype.Controller *)")
    public void controllerClassPointcut() {}

    /**
     * 지정: 모든Service의방법법
     */
    @Pointcut("execution(* com.iflytek.rpa.*.service.*(..))")
    public void servicePointcut() {}

    /**
     * 지정: 비고완료@ApiLog비고해제의방법법
     */
    @Pointcut("@annotation(com.iflytek.rpa.base.annotation.ApiLog)")
    public void apiLogPointcut() {}

    /**
     * 지정: 유형위비고완료@ApiLog비고해제의모든방법법
     */
    @Pointcut("@within(com.iflytek.rpa.base.annotation.ApiLog)")
    public void apiLogClassPointcut() {}

    /**
     * 알림: 기록방법법실행의정보
     */
    @Around("controllerPointcut() || controllerClassPointcut() || apiLogPointcut() || apiLogClassPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 조회여부@NoApiLog비고해제정렬제거
        if (shouldExcludeLogging(joinPoint)) {
            return joinPoint.proceed();
        }
        // 완료요청 ID
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 가져오기방법법정보
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        // 가져오기비고해제매칭
        ApiLog apiLog = getApiLogAnnotation(method, joinPoint.getTarget().getClass());

        // 기록시작 시간
        long startTime = System.currentTimeMillis();

        try {
            // 기록방법법호출열기 정보
            logMethodStart(requestId, className, methodName, joinPoint.getArgs(), apiLog);

            // 실행목록 방법법
            Object result = joinPoint.proceed();

            // 계획실행시간
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // 기록방법법호출성공정보
            logMethodSuccess(requestId, className, methodName, result, executionTime, apiLog);

            return result;

        } catch (Throwable throwable) {
            // 계획실행시간
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // 기록방법법호출예외정보
            logMethodException(requestId, className, methodName, throwable, executionTime, apiLog);

            // 다시 출력예외
            throw throwable;
        }
    }

    /**
     * 조회여부해당정렬제거로그기록
     */
    private boolean shouldExcludeLogging(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // 조회방법법위여부있음@NoApiLog비고해제
        if (method.isAnnotationPresent(NoApiLog.class)) {
            return true;
        }

        // 조회유형위여부있음@NoApiLog비고해제
        if (targetClass.isAnnotationPresent(NoApiLog.class)) {
            return true;
        }

        return false;
    }

    /**
     * 가져오기ApiLog비고해제
     */
    private ApiLog getApiLogAnnotation(Method method, Class<?> targetClass) {
        // 가져오기방법법위의비고해제
        ApiLog apiLog = method.getAnnotation(ApiLog.class);
        if (apiLog != null) {
            return apiLog;
        }

        // 가져오기유형위의비고해제
        return targetClass.getAnnotation(ApiLog.class);
    }

    /**
     * 기록방법법호출열기 정보
     */
    private void logMethodStart(String requestId, String className, String methodName, Object[] args, ApiLog apiLog) {
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            StringBuilder logMsg = new StringBuilder();

            // 로그형식: [시간] [상태] 값형식
            logMsg.append("[").append(timestamp).append("] ");
            logMsg.append("[API열기 ] ");
            logMsg.append("requestId=").append(requestId);
            logMsg.append(" className=").append(className);
            logMsg.append(" methodName=").append(methodName);

            // 가져오기HTTP요청 정보
            HttpServletRequest request = getHttpServletRequest();
            if (request != null) {
                logMsg.append(" httpMethod=").append(request.getMethod());
                logMsg.append(" requestUrl=").append(request.getRequestURL());
            }

            // 기록설명
            if (apiLog != null && !apiLog.value().isEmpty()) {
                logMsg.append(" operation=").append(apiLog.value().replaceAll("\\s+", "_"));
            }

            // 기록요청 매개변수
            if (apiLog == null || apiLog.logParams()) {
                String params = formatParameters(args, apiLog);
                logMsg.append(" params=").append(params.replaceAll("\\s+", " "));
            }

            log.info(logMsg.toString());

        } catch (Exception e) {
            log.warn(
                    "[{}] [API로그예외] requestId={} message={}",
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                    requestId,
                    e.getMessage());
        }
    }

    /**
     * 기록방법법호출성공정보
     */
    private void logMethodSuccess(
            String requestId, String className, String methodName, Object result, long executionTime, ApiLog apiLog) {
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            StringBuilder logMsg = new StringBuilder();

            // 로그형식
            logMsg.append("[").append(timestamp).append("] ");
            logMsg.append("[API성공] ");
            logMsg.append("requestId=").append(requestId);
            logMsg.append(" className=").append(className);
            logMsg.append(" methodName=").append(methodName);

            // 기록실행시간
            if (apiLog == null || apiLog.logTime()) {
                logMsg.append(" executionTime=").append(executionTime).append("ms");

                // 가능식별자, grep필터링
                if (executionTime > 5000) {
                    logMsg.append(" performanceLevel=SLOW");
                } else if (executionTime > 1000) {
                    logMsg.append(" performanceLevel=ATTENTION");
                } else {
                    logMsg.append(" performanceLevel=NORMAL");
                }
            }

            // 기록반환결과크기
            String resultStr = formatResult(result, apiLog);
            logMsg.append(" resultSize=").append(resultStr.length()).append("bytes");

            // 기록반환결과
            if (apiLog == null || apiLog.logResult()) {
                logMsg.append(" result=").append(resultStr.replaceAll("\\s+", " "));
            }

            // 추가상태식별자
            logMsg.append(" status=SUCCESS");

            log.info(logMsg.toString());

        } catch (Exception e) {
            log.warn(
                    "[{}] [API로그예외] requestId={} message={}",
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                    requestId,
                    e.getMessage());
        }
    }

    /**
     * 기록방법법호출예외정보
     */
    private void logMethodException(
            String requestId,
            String className,
            String methodName,
            Throwable throwable,
            long executionTime,
            ApiLog apiLog) {
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            StringBuilder logMsg = new StringBuilder();

            // 로그형식
            logMsg.append("[").append(timestamp).append("] ");
            logMsg.append("[API예외] ");
            logMsg.append("requestId=").append(requestId);
            logMsg.append(" className=").append(className);
            logMsg.append(" methodName=").append(methodName);

            // 기록실행시간
            if (apiLog == null || apiLog.logTime()) {
                logMsg.append(" executionTime=").append(executionTime).append("ms");
            }

            // 기록예외정보
            if (apiLog == null || apiLog.logException()) {
                logMsg.append(" exceptionType=").append(throwable.getClass().getSimpleName());
                String exceptionMessage = throwable.getMessage();
                if (exceptionMessage != null) {
                    // 관리예외메시지중의행기호및다중빈격식, grep
                    exceptionMessage = exceptionMessage.replaceAll("\\s+", " ").trim();
                    logMsg.append(" exceptionMessage=").append(exceptionMessage);
                }

                // 추가예외원인(결과가예패키지설치예외)
                Throwable rootCause = getRootCause(throwable);
                if (rootCause != throwable) {
                    logMsg.append(" rootCauseType=").append(rootCause.getClass().getSimpleName());
                }
            }

            // 추가상태식별자
            logMsg.append(" status=ERROR");

            log.error(logMsg.toString(), throwable);

        } catch (Exception e) {
            log.warn(
                    "[{}] [API로그예외] requestId={} message={}",
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                    requestId,
                    e.getMessage());
        }
    }

    /**
     * 형식요청 매개변수
     */
    private String formatParameters(Object[] args, ApiLog apiLog) {
        if (args == null || args.length == 0) {
            return "없음매개변수";
        }

        try {
            String jsonStr = JSON.toJSONString(args);

            // 제한제어길이정도
            int maxLength = apiLog != null ? apiLog.maxParamLength() : 2000;
            if (jsonStr.length() > maxLength) {
                jsonStr = jsonStr.substring(0, maxLength) + "...[완료가져오기]";
            }

            return jsonStr;

        } catch (JSONException e) {
            return "매개변수순서열실패: " + e.getMessage();
        } catch (Exception e) {
            return "매개변수형식예외: " + e.getMessage();
        }
    }

    /**
     * 형식반환결과
     */
    private String formatResult(Object result, ApiLog apiLog) {
        if (result == null) {
            return "null";
        }

        try {
            String jsonStr = JSON.toJSONString(result);

            // 제한제어길이정도
            int maxLength = apiLog != null ? apiLog.maxResultLength() : 2000;
            if (jsonStr.length() > maxLength) {
                jsonStr = jsonStr.substring(0, maxLength) + "...[완료가져오기]";
            }

            return jsonStr;

        } catch (JSONException e) {
            return "결과순서열실패: " + e.getMessage();
        } catch (Exception e) {
            return "결과형식예외: " + e.getMessage();
        }
    }

    /**
     * 가져오기HTTP요청 객체
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 가져오기클라이언트IP주소
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
                // 다중단계관리의아래, 일개IP로클라이언트IP
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 가져오기사용자ID(에서session, header또는token중)
     */
    private String extractUserId(HttpServletRequest request) {
        try {
            // 1. 시도에서header중가져오기사용자ID
            String userId = request.getHeader("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }

            // 2. 시도에서session중가져오기사용자ID
            if (request.getSession(false) != null) {
                Object sessionUserId = request.getSession(false).getAttribute("userId");
                if (sessionUserId != null) {
                    return sessionUserId.toString();
                }
            }

            // 3. 시도에서JWT token중파싱사용자ID(결과가사용JWT)
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                // 가능으로추가JWT파싱
                // 로완료단일, 예가져오기token의일모듈분로식별자
                String token = authorization.substring(7);
                if (token.length() > 10) {
                    return "token_" + token.substring(token.length() - 8);
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 가져오기예외의본원인
     */
    private Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        return rootCause;
    }
}