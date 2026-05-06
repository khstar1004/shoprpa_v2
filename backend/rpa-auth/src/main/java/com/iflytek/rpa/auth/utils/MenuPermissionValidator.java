package com.iflytek.rpa.auth.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 메뉴권한인증도구유형
 * 사용인증사용자여부있음권한방문지정의메뉴경로
 */
@Slf4j
public class MenuPermissionValidator {

    /**
     * 검증메뉴권한(admin평면)
     * @param request HTTP요청 
     * @return 인증결과
     */
    public static AppResponse<Boolean> checkMenuPermission(HttpServletRequest request) {
        try {
            // 1. 가져오기Referer
            String referer = request.getHeader("Referer");
            if (StringUtils.isNotBlank(referer)) {
                // 2. 에서referer파싱메뉴경로
                String menuPath = extractMenuPathFromReferer(referer);
                if (StringUtils.isNotBlank(menuPath)) {
                    // 3. 에서Session가져오기사용자메뉴경로목록
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        @SuppressWarnings("unchecked")
                        Set<String> userMenuPaths = (Set<String>) session.getAttribute("userMenuPaths");
                        if (userMenuPaths != null && !userMenuPaths.isEmpty()) {
                            // 4. 인증메뉴경로여부에서사용자메뉴목록중
                            if (!isPathAllowed(menuPath, userMenuPaths)) {
                                log.warn("사용자권한이 없습니다방문메뉴경로: {}, Referer: {}", menuPath, referer);
                                return AppResponse.error(ErrorCodeEnum.E_NO_POWER, "권한이 없습니다: 시스템 관리자에게 문의하세요");
                            }
                        } else {
                            log.warn("세션에서 사용자 메뉴 경로 목록을 찾을 수 없습니다, 메뉴 권한을 인증할 수 없습니다, Referer: {}", referer);
                            return AppResponse.error(ErrorCodeEnum.E_NO_POWER, "권한이 없습니다: 메뉴 권한 정보를 찾을 수 없습니다");
                        }
                    } else {
                        log.warn("Session찾을 수 없습니다, 메뉴 권한을 인증할 수 없습니다, Referer: {}", referer);
                        return AppResponse.error(ErrorCodeEnum.E_NO_POWER, "권한이 없습니다: Session찾을 수 없습니다");
                    }
                }
                // 결과가불가파싱메뉴경로, 기록로그행(가능예API호출대기)
            }
            // 결과가있음Referer또는불가파싱, 행(가능예직선연결방문또는API호출)
            return AppResponse.success(true);
        } catch (Exception e) {
            log.error("검증메뉴권한실패", e);
            // 검증실패시행, 정상일반프로세스
            return AppResponse.success(true);
        }
    }

    /**
     * 에서referer중가져오기메뉴경로
     *
     * @param referer Referer내용
     * @return 메뉴경로, 결과가파싱실패반환null
     */
    public static String extractMenuPathFromReferer(String referer) {
        try {
            URI uri = new URI(referer);
            String path = uri.getPath();

            if (StringUtils.isBlank(path)) {
                return null;
            }

            // 제거/admin전(결과가저장에서)
            if (path.startsWith("/admin")) {
                path = path.substring("/admin".length());
            }

            // 결과가경로비어 있습니다또는있음, 반환null
            if (StringUtils.isBlank(path) || "/".equals(path)) {
                return null;
            }

            // 제거모듈
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }

            return path;
        } catch (URISyntaxException e) {
            log.debug("파싱Referer URI실패: {}", referer, e);
            return null;
        }
    }

    /**
     * 조회경로여부에서허용의메뉴경로목록중
     * 지요소매칭및전매칭
     *
     * @param path 필요조회의경로
     * @param allowedPaths 허용의메뉴경로 합치기
     * @return 결과가허용반환true, 아니요이면반환false
     */
    public static boolean isPathAllowed(String path, Set<String> allowedPaths) {
        if (StringUtils.isBlank(path) || allowedPaths == null || allowedPaths.isEmpty()) {
            return false;
        }

        // 경로
        path = normalizeMenuPath(path);

        // 매칭
        if (allowedPaths.contains(path)) {
            return true;
        }

        // 전매칭: 조회여부있음단계메뉴경로
        // 예: 사용자있음/schedule, 이면/schedule/task허용
        for (String allowedPath : allowedPaths) {
            String normalizedAllowedPath = normalizeMenuPath(allowedPath);
            if (StringUtils.isNotBlank(normalizedAllowedPath) && path.startsWith(normalizedAllowedPath + "/")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 메뉴경로
     * @param path 기존경로
     * @return 후의경로
     */
    public static String normalizeMenuPath(String path) {
        if (StringUtils.isBlank(path)) {
            return path;
        }
        path = path.trim();
        // 제거모듈
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}