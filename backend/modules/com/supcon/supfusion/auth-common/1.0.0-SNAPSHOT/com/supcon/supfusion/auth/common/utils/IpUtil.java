package com.supcon.supfusion.auth.common.utils;

import javax.servlet.http.HttpServletRequest;

public class IpUtil {

    /**
     * 获取客户端IP地址，支持代理转发的真是IP地址获取
     *
     * @param request
     * @return
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = null;
        if (request == null) {
            return null;
        }
        ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("x-forwarded-for");
            if (null == ip || ip.trim().length() > 0) {
                ip = request.getRemoteAddr();
            } else {
                String[] ars = ip.split(",");
                ip = ars[0].trim();
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip != null && ip.equals("0:0:0:0:0:0:0:1")) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}
