package com.iflytek.rpa.utils;

import java.net.InetAddress;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * @ClassName IpUtil
 * @Author taozhang4
 * @Date 2019/9/17 10:02
 **/
public class IpUtil {
    public static final Logger LOGGER = LoggerFactory.getLogger(IpUtil.class);

    /**
     * 관리
     * <p>
     * X-Forwarded-For: Squid서비스관리
     * Proxy-Client-IP: apache서비스관리
     * WL-Proxy-Client-IP: weblogic서비스관리
     * X-Real-IP: nginx서비스관리
     * HTTP_CLIENT_IP: 있음관리서비스서버
     */
    static final String[] PROXYS = {
        "X-Real-IP", "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP"
    };

    static final String LOCALHOST_IP_V4 = "127.0.0.1";
    static final String LOCALHOST_IP_V6 = "0:0:0:0:0:0:0:1";

    /**
     * 가져오기클라이언트ip reactive
     */
    public static String getIpAddr(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();

        String ipAddress = null;
        for (String proxy : PROXYS) {
            ipAddress = headers.getFirst(proxy);
            if (!StringUtils.isEmpty(ipAddress) && !"unknown".equalsIgnoreCase(ipAddress)) {
                break;
            }
        }

        if (StringUtils.isEmpty(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = Objects.requireNonNull(request.getRemoteAddress())
                    .getAddress()
                    .getHostAddress();
        }

        String ipSeparator = ",";
        if (!StringUtils.isEmpty(ipAddress) && ipAddress.indexOf(ipSeparator) > 0) {
            ipAddress = ipAddress.substring(0, ipAddress.indexOf(ipSeparator));
        }

        return ipAddress;
    }

    /**
     * 가져오기클라이언트ip servlet
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress = null;

        for (String proxy : PROXYS) {
            ipAddress = request.getHeader(proxy);
            if (!StringUtils.isEmpty(ipAddress) && !"unknown".equalsIgnoreCase(ipAddress)) {
                break;
            }
        }

        if (StringUtils.isEmpty(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            // 근거네트워크가져오기본기기매칭의IP
            if (LOCALHOST_IP_V4.equals(ipAddress) || LOCALHOST_IP_V6.equals(ipAddress)) {
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                    ipAddress = inet.getHostAddress();
                } catch (Exception ignore) {
                }
            }
        }

        String ipSeparator = ",";
        if (!StringUtils.isEmpty(ipAddress) && ipAddress.indexOf(ipSeparator) > 0) {
            ipAddress = ipAddress.substring(0, ipAddress.indexOf(ipSeparator));
        }

        return ipAddress;
    }
}