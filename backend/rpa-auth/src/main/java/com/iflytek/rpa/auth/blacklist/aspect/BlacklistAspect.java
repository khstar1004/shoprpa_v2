package com.iflytek.rpa.auth.blacklist.aspect;

import com.iflytek.rpa.auth.blacklist.dto.BlacklistCacheDto;
import com.iflytek.rpa.auth.blacklist.exception.ShouldBeBlackException;
import com.iflytek.rpa.auth.blacklist.exception.UserBlockedException;
import com.iflytek.rpa.auth.blacklist.service.BlackListService;
import com.iflytek.rpa.auth.core.entity.LoginDto;
import com.iflytek.rpa.auth.core.entity.User;
import com.iflytek.rpa.auth.core.service.UserService;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.sp.uap.dao.UserDao;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import java.time.LocalDateTime;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 이름단일 AOP 
 *  /login-status 및 /pre-authenticate 연결
 * - /login-status: 서비스에서검증 session 시호출해당연결
 * - /pre-authenticate: 로그인인증연결, 중지사용 안 함사용자로그인
 * 결과가에서이름단일중, 비고판매출력예외
 *
 * @author system
 * @date 2025-12-16
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class BlacklistAspect {

    private final BlackListService blackListService;
    private final UserService userService;

    /**
     * 지정:  LoginController 의 loginStatus 방법법(/login-status 연결)
     */
    @Pointcut("execution(* com.iflytek.rpa.auth.core.controller.LoginController.loginStatus(..))")
    public void loginStatusPointcut() {}

    /**
     * 지정:  LoginController 의 preAuthenticate 방법법(/pre-authenticate 연결)
     */
    @Pointcut("execution(* com.iflytek.rpa.auth.core.controller.LoginController.preAuthenticate(..))")
    public void preAuthenticatePointcut() {}

    /**
     * 그룹합치기: loginStatus 또는 preAuthenticate
     */
    @Pointcut("loginStatusPointcut() || preAuthenticatePointcut()")
    public void loginCheckPointcut() {}

    /**
     * 알림: 에서로그인닫기방법법실행전조회이름단일
     */
    @Around("loginCheckPointcut()")
    public Object checkBlacklist(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 가져오기현재요청 
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                //  Web 요청 , 직선연결행
                return joinPoint.proceed();
            }

            HttpServletRequest request = attributes.getRequest();
            HttpServletResponse response = attributes.getResponse();
            String requestURI = request.getRequestURI();

            String userId = null;
            String username = null;

            // 예개연결
            if (requestURI.contains("/pre-authenticate")) {
                // /pre-authenticate 연결: 에서요청 매개변수중가져오기휴대폰 번호, 후조회사용자ID
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    // 일개매개변수 해당예 LoginDto
                    Object firstArg = args[0];
                    if (firstArg instanceof LoginDto) {
                        LoginDto loginDto = (LoginDto) firstArg;
                        String phone = loginDto.getPhone();

                        if (StringUtils.hasText(phone)) {
                            // 통신경과휴대폰 번호조회사용자ID
                            userId = getUserIdByPhone(phone, request);
                            username = phone; // 시사용휴대폰 번호로사용자명
                            log.debug("에서 pre-authenticate 요청 중가져오기휴대폰 번호: {}, userId: {}", phone, userId);
                        }
                    }
                }
            } else {
                // /login-status 연결: 에서 session 중가져오기완료로그인사용자
                UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
                if (loginUser != null) {
                    userId = loginUser.getId();
                    username = loginUser.getLoginName();
                }
            }

            // 결과가가져오기까지사용자ID, 조회여부에서이름단일중
            if (userId != null && !userId.isEmpty()) {
                BlacklistCacheDto blacklist = blackListService.isBlocked(userId);

                if (blacklist != null) {
                    // 사용자에서이름단일중, 로그인
                    log.warn(
                            "AOP 까지이름단일사용자시도로그인, userId: {}, username: {}, reason: {}",
                            userId,
                            username,
                            blacklist.getReason());

                    // 결과가예완료로그인사용자(/login-status), 강함제어비고판매
                    // /pre-authenticate 연결본있음로그인, 필요하지 않습니다비고판매
                    if (!requestURI.contains("/pre-authenticate") && response != null) {
                        blackListService.forceLogout(request, response);
                    }

                    // 를시간변환로 LocalDateTime
                    LocalDateTime endTime = blacklist.getEndTimeMillis() != null
                            ? LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(blacklist.getEndTimeMillis()),
                                    java.time.ZoneId.systemDefault())
                            : null;

                    // 계획시간(초)
                    long remainingSeconds = 0;
                    if (endTime != null) {
                        remainingSeconds = java.time.Duration.between(LocalDateTime.now(), endTime)
                                .getSeconds();
                        if (remainingSeconds < 0) {
                            remainingSeconds = 0;
                        }
                    }

                    // 출력예외
                    throw new UserBlockedException(
                            blacklist.getUserId(),
                            blacklist.getUsername(),
                            blacklist.getReason(),
                            endTime,
                            remainingSeconds);
                }
            }

            // 계속실행서비스방법법
            return joinPoint.proceed();

        } catch (UserBlockedException | ShouldBeBlackException e) {
            throw e;
        } catch (Throwable e) {
            log.error("이름단일 AOP 실행예외", e);
            // 예외계속실행, 정상일반서비스
            return joinPoint.proceed();
        }
    }

    /**
     * 통신경과휴대폰 번호조회사용자ID
     */
    private String getUserIdByPhone(String phone, HttpServletRequest request) {
        try {
            AppResponse<User> response = userService.getUserInfoByPhone(phone, request);
            if (response == null || !response.ok() || response.getData() == null) {
                log.debug("찾을 수 없는 사용자, 휴대폰 번호: {}", phone);
                return null;
            }
            User user = response.getData();
            String userId = user.getId();
            if (!StringUtils.hasText(userId)) {
                log.debug("사용자ID비어 있습니다, 휴대폰 번호: {}", phone);
                return null;
            }
            return userId;
        } catch (Exception e) {
            log.debug("통신경과휴대폰 번호조회사용자ID실패: {}", phone, e);
            return null;
        }
    }
}