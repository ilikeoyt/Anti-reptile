package cn.ycx.AntiCrawler.rule;

import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;

import cn.ycx.AntiCrawler.config.AntiReptileProperties;
import cn.ycx.AntiCrawler.util.WhitelistManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ycx
 */
public class UaRule extends AbstractRule {

    private AntiReptileProperties properties;
    
    private WhitelistManager whitelistManager;
    
    public UaRule(AntiReptileProperties properties, WhitelistManager whitelistManager) {
        this.properties = properties;
        this.whitelistManager = whitelistManager;
    }
    
    public UaRule() {
        // 默认构造函数，用于Spring自动配置
    }

    @Override
    protected boolean doExecute(HttpServletRequest request, HttpServletResponse response) {
        AntiReptileProperties.UaRule uaRule = properties.getUaRule();
        
        if (!uaRule.isEnabled()) {
            return false;
        }
        
        // 检查是否在白名单中
        if (whitelistManager.isInWhitelist(request, "ua")) {
            System.out.println("[UaRule] IP is in whitelist, bypassing UA check");
            return false;
        }
        
        String userAgentStr = request.getHeader("User-Agent");
        System.out.println("[UaRule] User-Agent: " + userAgentStr);
        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentStr);
        OperatingSystem os = userAgent.getOperatingSystem();
        OperatingSystem osGroup = userAgent.getOperatingSystem().getGroup();
        DeviceType deviceType = userAgent.getOperatingSystem().getDeviceType();
        System.out.println("[UaRule] DeviceType: " + deviceType + ", OS: " + os + ", OS Group: " + osGroup);
        System.out.println("[UaRule] Allowed PC: " + uaRule.isAllowedPc() + ", Allowed Mobile: " + uaRule.isAllowedMobile() + ", Allowed Linux: " + uaRule.isAllowedLinux());
        
        // 1. 白名单检查：如果匹配白名单，直接允许访问
        java.util.List<String> whiteUaList = uaRule.getWhiteUa();
        if (whiteUaList != null && !whiteUaList.isEmpty() && userAgentStr != null) {
            for (String whiteUa : whiteUaList) {
                if (matchUserAgent(userAgentStr, whiteUa)) {
                    return false; // 匹配白名单，允许访问
                }
            }
        }
        
        // 2. 黑名单检查：如果匹配黑名单，直接拦截
        java.util.List<String> blackUaList = uaRule.getBlackUa();
        if (blackUaList != null && !blackUaList.isEmpty() && userAgentStr != null) {
            for (String blackUa : blackUaList) {
                System.out.println("[UaRule] Checking black UA pattern: " + blackUa);
                if (matchUserAgent(userAgentStr, blackUa)) {
                    System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Matched black UA: " + blackUa + ", User-Agent: " + userAgentStr);
                    return true; // 匹配黑名单，拦截访问
                }
            }
        }
        
        // 3. 原有设备和操作系统检查逻辑
        if (DeviceType.UNKNOWN.equals(deviceType)) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Unknown device, User-Agent: " + userAgentStr);
            return true;
        } else if (OperatingSystem.UNKNOWN.equals(os)
                || OperatingSystem.UNKNOWN_MOBILE.equals(os)
                || OperatingSystem.UNKNOWN_TABLET.equals(os)) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Unknown OperatingSystem, User-Agent: " + userAgentStr);
            return true;
        }
        if (!uaRule.isAllowedLinux() && (OperatingSystem.LINUX.equals(osGroup) || OperatingSystem.LINUX.equals(os))) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Not Allowed Linux request, User-Agent: " + userAgent.toString());
            return true;
        }
        if (!uaRule.isAllowedMobile() && (DeviceType.MOBILE.equals(deviceType) || DeviceType.TABLET.equals(deviceType))) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Not Allowed Mobile Device request, User-Agent: " + userAgent.toString());
            return true;
        }
        if (!uaRule.isAllowedPc() && DeviceType.COMPUTER.equals(deviceType)) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Not Allowed PC request, User-Agent: " + userAgentStr);
            return true;
        }
        if (!uaRule.isAllowedIot() && (DeviceType.DMR.equals(deviceType) || DeviceType.GAME_CONSOLE.equals(deviceType) || DeviceType.WEARABLE.equals(deviceType))) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Not Allowed Iot Device request, User-Agent: " + userAgentStr);
            return true;
        }
        if (!uaRule.isAllowedProxy() && OperatingSystem.PROXY.equals(os)) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Not Allowed Proxy request, User-Agent: " + userAgentStr);
            return true;
        }
        return false;
    }

    @Override
    public void reset(HttpServletRequest request, String realRequestUri) {
        /**
         * 添加到白名单
         */
        int whitelistExpirationTime = properties.getUaRule().getWhitelistExpirationTime();
        whitelistManager.addToWhitelist(request, "ua", whitelistExpirationTime);
    }
    
    /**
     * 匹配User-Agent是否符合规则（支持通配符*）
     * @param userAgentStr 实际的User-Agent字符串
     * @param rule 规则（支持*通配符）
     * @return 是否匹配
     */
    private boolean matchUserAgent(String userAgentStr, String rule) {
        if (userAgentStr == null || rule == null) {
            return false;
        }
        // 将通配符*转换为正则表达式
        String regex = rule.replace("*", ".*");
        return userAgentStr.matches(regex);
    }

    @Override
    public int getOrder() {
        return 1;
    }
    
    @Override
    public String getInterceptorStrategy() {
        return properties.getUaRule().getInterceptorStrategy();
    }
}
