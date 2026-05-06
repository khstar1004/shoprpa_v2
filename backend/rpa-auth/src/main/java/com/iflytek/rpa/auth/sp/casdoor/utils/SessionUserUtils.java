package com.iflytek.rpa.auth.sp.casdoor.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.casbin.casdoor.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @desc: Session사용자도구유형, 사용에서session중가져오기사용자 정보
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/12/11
 */
public class SessionUserUtils {

    private static final Logger logger = LoggerFactory.getLogger(SessionUserUtils.class);

    /**
     * 에서session중가져오기Casdoor사용자객체
     *
     * @param request HTTP요청 
     * @return Casdoor사용자객체, 결과가로그인되지 않았습니다또는session찾을 수 없습니다이면반환null
     */
    public static User getUserFromSession(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return null;
            }

            User user = (User) session.getAttribute("user");
            if (user == null) {
                return null;
            }

            return user;
        } catch (Exception e) {
            logger.warn("에서session가져오기사용자 정보실패", e);
            return null;
        }
    }

    /**
     * 에서session중가져오기현재테넌트ID(owner)
     *
     * @param request HTTP요청 
     * @return 테넌트ID(owner), 결과가로그인되지 않았습니다또는session찾을 수 없습니다이면반환null
     */
    public static String getTenantOwnerFromSession(HttpServletRequest request) {
        User user = getUserFromSession(request);
        if (user == null) {
            return null;
        }

        if (user.owner != null && !user.owner.isEmpty()) {
            return user.owner;
        }

        return null;
    }

    /**
     * 조회사용자여부완료로그인
     *
     * @param request HTTP요청 
     * @return 결과가사용자완료로그인반환true, 아니요이면반환false
     */
    public static boolean isUserLoggedIn(HttpServletRequest request) {
        User user = getUserFromSession(request);
        return user != null && user.name != null && !user.name.isEmpty();
    }
}