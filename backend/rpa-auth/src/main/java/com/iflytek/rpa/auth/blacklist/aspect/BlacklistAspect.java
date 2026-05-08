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
 * 사용자 차단 상태를 로그인 관련 API 앞에서 검사하는 AOP입니다.
 * - /login-status: 로그인 세션 확인 시 차단 상태를 검사합니다.
 * - /pre-authenticate: 로그인 인증 전에 차단 상태를 검사합니다.
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
     * LoginController.loginStatus(/login-status) 진입점입니다.
     */
    @Pointcut("execution(* com.iflytek.rpa.auth.core.controller.LoginController.loginStatus(..))")
    public void loginStatusPointcut() {}

    /**
     * LoginController.preAuthenticate(/pre-authenticate) 진입점입니다.
     */
    @Pointcut("execution(* com.iflytek.rpa.auth.core.controller.LoginController.preAuthenticate(..))")
    public void preAuthenticatePointcut() {}

    /**
     * 로그인 상태 확인과 사전 인증 진입점을 함께 묶습니다.
     */
    @Pointcut("loginStatusPointcut() || preAuthenticatePointcut()")
    public void loginCheckPointcut() {}

    /**
     * 로그인 관련 메서드 실행 전에 사용자 차단 상태를 검사합니다.
     */
    @Around("loginCheckPointcut()")
    public Object checkBlacklist(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return joinPoint.proceed();
            }

            HttpServletRequest request = attributes.getRequest();
            HttpServletResponse response = attributes.getResponse();
            String requestURI = request.getRequestURI();

            String userId = null;
            String username = null;

            if (requestURI.contains("/pre-authenticate")) {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    Object firstArg = args[0];
                    if (firstArg instanceof LoginDto) {
                        LoginDto loginDto = (LoginDto) firstArg;
                        String phone = loginDto.getPhone();

                        if (StringUtils.hasText(phone)) {
                            userId = getUserIdByPhone(phone, request);
                            username = phone;
                            log.debug("pre-authenticate 요청에서 휴대폰 번호를 확인했습니다. phone: {}, userId: {}", phone, userId);
                        }
                    }
                }
            } else {
                UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
                if (loginUser != null) {
                    userId = loginUser.getId();
                    username = loginUser.getLoginName();
                }
            }

            if (userId != null && !userId.isEmpty()) {
                BlacklistCacheDto blacklist = blackListService.isBlocked(userId);

                if (blacklist != null) {
                    log.warn(
                            "차단된 사용자의 로그인 관련 요청을 차단합니다. userId: {}, username: {}, reason: {}",
                            userId,
                            username,
                            blacklist.getReason());

                    if (!requestURI.contains("/pre-authenticate") && response != null) {
                        blackListService.forceLogout(request, response);
                    }

                    LocalDateTime endTime = blacklist.getEndTimeMillis() != null
                            ? LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(blacklist.getEndTimeMillis()),
                                    java.time.ZoneId.systemDefault())
                            : null;

                    long remainingSeconds = 0;
                    if (endTime != null) {
                        remainingSeconds = java.time.Duration.between(LocalDateTime.now(), endTime)
                                .getSeconds();
                        if (remainingSeconds < 0) {
                            remainingSeconds = 0;
                        }
                    }

                    throw new UserBlockedException(
                            blacklist.getUserId(),
                            blacklist.getUsername(),
                            blacklist.getReason(),
                            endTime,
                            remainingSeconds);
                }
            }

            return joinPoint.proceed();

        } catch (UserBlockedException | ShouldBeBlackException e) {
            throw e;
        } catch (Throwable e) {
            log.error("사용자 차단 AOP 실행 예외", e);
            return joinPoint.proceed();
        }
    }

    /**
     * 휴대폰 번호로 사용자ID를 조회합니다.
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
                log.debug("사용자ID가 비어 있습니다. phone: {}", phone);
                return null;
            }
            return userId;
        } catch (Exception e) {
            log.debug("휴대폰 번호로 사용자ID 조회 실패: {}", phone, e);
            return null;
        }
    }
}
