package cn.ycx.AntiCrawler.rule;

import org.springframework.beans.factory.annotation.Autowired;

import cn.ycx.AntiCrawler.config.AntiReptileProperties;
import cn.ycx.AntiCrawler.util.WhitelistManager;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author ycx
 * @since 2025/12
 * 基于Cookie的反爬规则
 */
public class CookieRule extends AbstractRule {

    private AntiReptileProperties properties;
    
    private WhitelistManager whitelistManager;
    
    public CookieRule(AntiReptileProperties properties, WhitelistManager whitelistManager) {
        this.properties = properties;
        this.whitelistManager = whitelistManager;
    }
    
    public CookieRule() {
        // 默认构造函数，用于Spring自动配置
    }

    @Override
    protected boolean doExecute(HttpServletRequest request, HttpServletResponse response) {
        AntiReptileProperties.CookieRule cookieRule = properties.getCookieRule();
        
        if (!cookieRule.isEnabled()) {
            return false;
        }
        
        // 检查是否在白名单中
        if (whitelistManager.isInWhitelist(request, "cookie")) {
            return false;
        }
        
        Cookie[] cookies = request.getCookies();
        
        // 1. 检查是否允许空Cookie
        if (cookies == null || cookies.length == 0) {
            if (!cookieRule.isAllowEmptyCookie()) {
                System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Empty Cookie not allowed");
                return true;
            }
            return false;
        }
        
        // 2. 检查是否包含必要的Cookie
        List<String> requiredCookies = cookieRule.getRequiredCookies();
        if (requiredCookies != null && !requiredCookies.isEmpty()) {
            for (String requiredCookie : requiredCookies) {
                boolean found = false;
                for (Cookie cookie : cookies) {
                    if (requiredCookie.equals(cookie.getName())) {
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Missing required Cookie: " + requiredCookie);
                    return true;
                }
            }
        }
        
        // 3. 检查Cookie是否在黑名单中
        List<String> blackCookies = cookieRule.getBlackCookies();
        if (blackCookies != null && !blackCookies.isEmpty()) {
            for (Cookie cookie : cookies) {
                String cookieInfo = cookie.getName() + "=" + cookie.getValue();
                for (String blackCookie : blackCookies) {
                    if (matchCookie(cookieInfo, blackCookie)) {
                        System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Cookie in blacklist: " + cookieInfo);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    @Override
    public void reset(HttpServletRequest request, String realRequestUri) {
        /**
         * 添加到白名单
         */
        int whitelistExpirationTime = properties.getCookieRule().getWhitelistExpirationTime();
        whitelistManager.addToWhitelist(request, "cookie", whitelistExpirationTime);
    }
    
    @Override
    public int getOrder() {
        return 2; // 优先级低于UA规则，高于IP规则
    }
    
    @Override
    public String getInterceptorStrategy() {
        return properties.getCookieRule().getInterceptorStrategy();
    }
    
    /**
     * 匹配Cookie是否符合规则（支持通配符*）
     * @param cookieInfo 实际的Cookie信息（格式：name=value）
     * @param rule 规则（支持*通配符）
     * @return 是否匹配
     */
    private boolean matchCookie(String cookieInfo, String rule) {
        if (cookieInfo == null || rule == null) {
            return false;
        }
        // 将通配符*转换为正则表达式
        String regex = rule.replace("*", ".*");
        return cookieInfo.matches(regex);
    }
}