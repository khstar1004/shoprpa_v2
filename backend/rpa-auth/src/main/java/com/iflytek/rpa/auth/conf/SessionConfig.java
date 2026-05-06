package com.iflytek.rpa.auth.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Session Cookie 매칭유형
 * 사용지정Spring Session의Cookie매칭, 예Cookie의경로
 *
 * 제목: 
 * - 사용의context-path로 /api/rpa-auth
 * - 아래, Spring Session를Cookie의path로context-path
 * - 가져오기 Cookie에서 /api/rpa-auth 아래, 불가에서 /api/ 아래의사용중사용
 * - 통신경과매칭, 강함제어를Cookie의path로 /api/, Cookie의사용공유
 *
 * 비고: Java매칭의단계높이YAML매칭, 확인Cookie경로일지정예 /api/
 *
 * @author lihang
 * @date 2025-12-08
 */
@Configuration
public class SessionConfig {

    /**
     * 매칭Session Cookie의순서열기기
     * 강함제어Cookie의path로 /api/, 덮어쓰기의context-path매칭
     *
     * @return CookieSerializer
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();

        // 강함제어Cookie경로로 /api/, 아니요예의context-path (/api/rpa-auth)
        // Cookie가능으로에서 /api/ 경로아래의모든서비스공유
        serializer.setCookiePath("/");

        // Cookie이름, Spring Session로SESSION
        serializer.setCookieName("SESSION");

        // Cookie대저장시간(초), 및@EnableRedisHttpSession의maxInactiveIntervalInSeconds보관일
        // 604800초 = 7
        //        serializer.setCookieMaxAge(604800);

        // 사용HttpOnly, 중지JavaScript방문Cookie, 높이설치전체
        serializer.setUseHttpOnlyCookie(true);

        // 여부사용Secure(에서HTTPS아래입출력Cookie)
        // 열기발송가능으로아니요사용, 운영 환경생성사용
        // serializer.setUseSecureCookie(true);

        // SameSite속성, 중지CSRF
        // Lax: 허용모듈분삼방법요청 Cookie(GET요청 )
        // Strict: 전체금지삼방법요청 Cookie
        // None: 허용모든삼방법요청 Cookie(필요시Secure=true)
        serializer.setSameSite("Lax");

        return serializer;
    }
}