package cn.ycx.AntiCrawler.ai;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求信息收集器，用于收集与反爬相关的请求信息
 */
public class RequestInfoCollector {

    /**
     * 收集请求信息
     */
    public static RequestInfo collectRequestInfo(HttpServletRequest request, String interceptReason, String interceptRule) {
        RequestInfo requestInfo = new RequestInfo();
        
        requestInfo.setRequestId(generateRequestId());
        requestInfo.setRequestTime(new Date());
        requestInfo.setRequestMethod(request.getMethod());
        requestInfo.setRequestUrl(request.getRequestURL().toString());
        requestInfo.setRequestUri(request.getRequestURI());
        requestInfo.setQueryString(request.getQueryString());
        requestInfo.setRemoteAddr(getClientIpAddress(request));
        requestInfo.setUserAgent(request.getHeader("User-Agent"));
        requestInfo.setReferer(request.getHeader("Referer"));
        requestInfo.setCookies(extractCookies(request));
        requestInfo.setHeaders(extractHeaders(request));
        requestInfo.setParameters(extractParameters(request));
        requestInfo.setRequestBody(extractRequestBody(request));
        requestInfo.setInterceptReason(interceptReason);
        requestInfo.setInterceptRule(interceptRule);
        requestInfo.setContentType(request.getContentType());
        requestInfo.setContentLength(request.getContentLength());
        requestInfo.setProtocol(request.getProtocol());
        
        return requestInfo;
    }

    /**
     * 生成请求ID
     */
    private static String generateRequestId() {
        return System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * 获取客户端IP地址
     */
    private static String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 提取Cookie信息
     */
    private static String extractCookies(HttpServletRequest request) {
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return "";
        }
        
        StringBuilder cookieBuilder = new StringBuilder();
        for (javax.servlet.http.Cookie cookie : cookies) {
            cookieBuilder.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
        }
        return cookieBuilder.toString();
    }

    /**
     * 提取请求头信息
     */
    private static Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    /**
     * 提取请求参数
     */
    private static Map<String, String[]> extractParameters(HttpServletRequest request) {
        return request.getParameterMap();
    }

    /**
     * 提取请求体
     */
    private static String extractRequestBody(HttpServletRequest request) {
        try {
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                Map<String, String[]> parameterMap = request.getParameterMap();
                if (parameterMap != null && !parameterMap.isEmpty()) {
                    StringBuilder bodyBuilder = new StringBuilder();
                    for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                        if (bodyBuilder.length() > 0) {
                            bodyBuilder.append("&");
                        }
                        bodyBuilder.append(entry.getKey()).append("=");
                        if (entry.getValue() != null && entry.getValue().length > 0) {
                            bodyBuilder.append(entry.getValue()[0]);
                        }
                    }
                    return bodyBuilder.toString();
                }
            }
        } catch (Exception e) {
            System.out.println("[RequestInfoCollector] Failed to extract request body: " + e.getMessage());
        }
        return "";
    }
}
