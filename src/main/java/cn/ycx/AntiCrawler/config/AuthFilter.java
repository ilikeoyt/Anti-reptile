package cn.ycx.AntiCrawler.config;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 认证过滤器，用于保护配置管理页面和配置API
 */
@Component
public class AuthFilter implements Filter {

    private static final String LOGIN_PATH = "/ycx-config/login.html";
    private static final String LOGIN_API = "/ycx-config/login";
    private static final String LOGOUT_API = "/ycx-config/logout";
    private static final String SESSION_USER_KEY = "anti_reptile_admin_user";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestUri = httpRequest.getRequestURI();

        // 获取AntiReptileProperties配置
        AntiReptileProperties properties = getAntiReptileProperties(httpRequest);
        
        // 如果未启用认证，直接放行
        if (properties == null || !properties.isEnableAuth()) {
            chain.doFilter(request, response);
            return;
        }

        // 登录页面和登录API不需要认证
        if (requestUri.equals(LOGIN_PATH) || requestUri.equals(LOGIN_API) || requestUri.equals(LOGOUT_API)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查是否访问配置管理相关的路径
        if (requestUri.startsWith("/ycx-config/") || requestUri.equals("/anti-reptile/config")) {
            // 检查是否已登录
            HttpSession session = httpRequest.getSession(false);
            if (session != null && session.getAttribute(SESSION_USER_KEY) != null) {
                // 已登录，放行
                chain.doFilter(request, response);
            } else {
                // 未登录，重定向到登录页面
                httpResponse.sendRedirect(LOGIN_PATH);
            }
        } else {
            // 非配置管理路径，放行
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }

    /**
     * 获取AntiReptileProperties配置
     */
    private AntiReptileProperties getAntiReptileProperties(HttpServletRequest request) {
        try {
            return (AntiReptileProperties) request.getServletContext().getAttribute(AntiReptileProperties.class.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
