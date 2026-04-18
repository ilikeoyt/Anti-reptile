package cn.ycx.AntiCrawler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


import java.util.List;

/**
 * @author ycx
 */
@ConfigurationProperties(prefix = "anti.reptile.manager")
public class AntiReptileProperties {

    /**
     * 是否启用反爬虫插件
     */
    private boolean enabled;

    /**
     * 是否启用全局拦截，默认为false，可设置为true全局拦截
     */
    private boolean globalFilterMode = false;

    /**
     * 非全局拦截下，需要反爬的接口列表，以'/'开头，以','分隔
     */
    private List<String> includeUrls;

    /**
     * 全局拦截下，需要排除反爬的接口列表，支持正则表达式，以','分隔
     */
    private List<String> excludeUrls;

    /**
     * 基于请求IP的反爬规则
     */
    private IpRule ipRule = new IpRule();

    /**
     * 基于请求User-Agent的反爬规则
     */
    private UaRule uaRule = new UaRule();
    
    /**
     * 基于Cookie的反爬规则
     */
    private CookieRule cookieRule = new CookieRule();
    
    /**
     * 基于行为链路的反爬规则
     */
    private BehaviorChainRule behaviorChainRule = new BehaviorChainRule();
    
    /**
     * 配置管理页面的管理员用户名
     */
    private String adminUsername = "admin";
    
    /**
     * 配置管理页面的管理员密码
     */
    private String adminPassword = "admin123";
    
    /**
     * 是否启用配置管理页面的认证，默认为true
     */
    private boolean enableAuth = true;
    
    /**
     * AI配置
     */
    private AIConfig ai = new AIConfig();

    public boolean isEnabled() {
        return enabled;
    }
    
    public static class CookieRule {
        /**
         * 是否启用Cookie Rule：默认启用
         */
        private boolean enabled = true;
        
        /**
         * 需要检查的Cookie名称列表，以','分隔
         */
        private List<String> requiredCookies;
        
        /**
         * 是否允许空Cookie：默认否
         */
        private boolean allowEmptyCookie = false;
        
        /**
         * Cookie黑名单列表，匹配则拦截，支持通配符*，以','分隔
         */
        private List<String> blackCookies;
        
        /**
         * 拦截策略：verify（验证码）或deny（直接拒绝），默认为verify
         */
        private String interceptorStrategy = "verify";
        
        /**
         * 白名单时长，单位秒，默认为3600秒（1小时）
         */
        private Integer whitelistExpirationTime = 10;
        
        /**
         * 触发规则后的惩罚时长（分钟），默认60分钟
         */
        private Integer penaltyExpirationTime = 60;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public List<String> getRequiredCookies() {
            return requiredCookies;
        }
        
        public void setRequiredCookies(List<String> requiredCookies) {
            this.requiredCookies = requiredCookies;
        }
        
        public boolean isAllowEmptyCookie() {
            return allowEmptyCookie;
        }
        
        public void setAllowEmptyCookie(boolean allowEmptyCookie) {
            this.allowEmptyCookie = allowEmptyCookie;
        }
        
        public List<String> getBlackCookies() {
            return blackCookies;
        }
        
        public void setBlackCookies(List<String> blackCookies) {
            this.blackCookies = blackCookies;
        }
        
        public String getInterceptorStrategy() {
            return interceptorStrategy;
        }
        
        public void setInterceptorStrategy(String interceptorStrategy) {
            this.interceptorStrategy = interceptorStrategy;
        }
        
        public Integer getWhitelistExpirationTime() {
            return whitelistExpirationTime;
        }
        
        public void setWhitelistExpirationTime(Integer whitelistExpirationTime) {
            this.whitelistExpirationTime = whitelistExpirationTime;
        }
        
        public Integer getPenaltyExpirationTime() {
            return penaltyExpirationTime;
        }
        
        public void setPenaltyExpirationTime(Integer penaltyExpirationTime) {
            this.penaltyExpirationTime = penaltyExpirationTime;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getIncludeUrls() {
        return includeUrls;
    }

    public void setIncludeUrls(List<String> includeUrls) {
        this.includeUrls = includeUrls;
    }

    public List<String> getExcludeUrls() {
        return excludeUrls;
    }

    public void setExcludeUrls(List<String> excludeUrls) {
        this.excludeUrls = excludeUrls;
    }

    public IpRule getIpRule() {
        return ipRule;
    }

    public void setIpRule(IpRule ipRule) {
        this.ipRule = ipRule;
    }

    public UaRule getUaRule() {
        return uaRule;
    }

    public void setUaRule(UaRule uaRule) {
        this.uaRule = uaRule;
    }

    public CookieRule getCookieRule() {
        return cookieRule;
    }

    public void setCookieRule(CookieRule cookieRule) {
        this.cookieRule = cookieRule;
    }
    
    public BehaviorChainRule getBehaviorChainRule() {
        return behaviorChainRule;
    }
    
    public void setBehaviorChainRule(BehaviorChainRule behaviorChainRule) {
        this.behaviorChainRule = behaviorChainRule;
    }

    public boolean isGlobalFilterMode() {
        return globalFilterMode;
    }
    public void setGlobalFilterMode(boolean globalFilterMode) {
        this.globalFilterMode = globalFilterMode;
    }
    
    public String getAdminUsername() {
        return adminUsername;
    }
    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }
    
    public String getAdminPassword() {
        return adminPassword;
    }
    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
    
    public boolean isEnableAuth() {
        return enableAuth;
    }
    public void setEnableAuth(boolean enableAuth) {
        this.enableAuth = enableAuth;
    }



    public static class IpRule {

        /**
         * 是否启用IP Rule：默认启用
         */
        private boolean enabled = true;

        /**
         * 时间窗口：默认5000ms
         */
        private Integer expirationTime = 5000;

        /**
         * 最大请求数，默认20
         */
        private Integer requestMaxSize = 20;

        /**
         * IP白名单，支持后缀'*'通配，以','分隔
         */
        private List<String> ignoreIp;

        /**
        * IP 黑名单，支持后缀'*'通配，以','分隔
        */
        private List<String> blackIp;

        /**
        * 触发规则后的惩罚时长（分钟），默认60分钟
        */
        private Integer penaltyExpirationTime = 60;

        /**
        * 拦截策略：verify（验证码）或deny（直接拒绝），默认为verify
        */
        private String interceptorStrategy = "verify";
        
        /**
        * 白名单时长，单位秒，默认为3600秒（1小时）
        */
        private Integer whitelistExpirationTime = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(Integer expirationTime) {
            this.expirationTime = expirationTime;
        }

        public Integer getRequestMaxSize() {
            return requestMaxSize;
        }

        public void setRequestMaxSize(Integer requestMaxSize) {
            this.requestMaxSize = requestMaxSize;
        }

        public List<String> getIgnoreIp() {
            return ignoreIp;
        }

        public void setIgnoreIp(List<String> ignoreIp) {
            this.ignoreIp = ignoreIp;
        }

        // getter 和 setter...
        public List<String> getBlackIp() {
            return blackIp;
        }
        
        public void setBlackIp(List<String> blackIp) {
            this.blackIp = blackIp;
        }
        
        public Integer getPenaltyExpirationTime() {
            return penaltyExpirationTime;
        }
        
        public void setPenaltyExpirationTime(Integer penaltyExpirationTime) {
            this.penaltyExpirationTime = penaltyExpirationTime;
        }
        
        public String getInterceptorStrategy() {
            return interceptorStrategy;
        }
        
        public void setInterceptorStrategy(String interceptorStrategy) {
            this.interceptorStrategy = interceptorStrategy;
        }
        
        public Integer getWhitelistExpirationTime() {
            return whitelistExpirationTime;
        }
        
        public void setWhitelistExpirationTime(Integer whitelistExpirationTime) {
            this.whitelistExpirationTime = whitelistExpirationTime;
        }   
    }

    public static class UaRule {
        /**
         * 是否启用User-Agent Rule：默认启用
         */
        private boolean enabled = true;

        /**
         * 是否允许Linux系统访问：默认否
         */
        private boolean allowedLinux = false;

        /**
         * 是否允许移动端设备访问：默认是
         */
        private boolean allowedMobile = true;

        /**
         *  是否允许移PC设备访问: 默认是
         */
        private boolean allowedPc = true;

        /**
         * 是否允许Iot设备访问：默认否
         */
        private boolean allowedIot = false;

        /**
         * 是否允许代理访问：默认否
         */
        private boolean allowedProxy = false;

        /**
         * User-Agent白名单：支持通配符*
         */
        private List<String> whiteUa;

        /**
         * User-Agent黑名单：支持通配符*
         */
        private List<String> blackUa;

        /**
         * 拦截策略：verify（验证码）或deny（直接拒绝），默认为verify
         */
        private String interceptorStrategy = "verify";
        
        /**
         * 白名单时长，单位秒，默认为3600秒（1小时）
         */
        private Integer whitelistExpirationTime = 10;
        
        /**
         * 触发规则后的惩罚时长（分钟），默认60分钟
         */
        private Integer penaltyExpirationTime = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAllowedLinux() {
            return allowedLinux;
        }

        public void setAllowedLinux(boolean allowedLinux) {
            this.allowedLinux = allowedLinux;
        }

        public boolean isAllowedMobile() {
            return allowedMobile;
        }

        public void setAllowedMobile(boolean allowedMobile) {
            this.allowedMobile = allowedMobile;
        }

        public boolean isAllowedPc() {
            return allowedPc;
        }

        public void setAllowedPc(boolean allowedPc) {
            this.allowedPc = allowedPc;
        }

        public boolean isAllowedIot() {
            return allowedIot;
        }

        public void setAllowedIot(boolean allowedIot) {
            this.allowedIot = allowedIot;
        }

        public boolean isAllowedProxy() {
            return allowedProxy;
        }

        public void setAllowedProxy(boolean allowedProxy) {
            this.allowedProxy = allowedProxy;
        }

        public List<String> getWhiteUa() {
            return whiteUa;
        }

        public void setWhiteUa(List<String> whiteUa) {
            this.whiteUa = whiteUa;
        }

        public List<String> getBlackUa() {
            return blackUa;
        }

        public void setBlackUa(List<String> blackUa) {
            this.blackUa = blackUa;
        }

        public String getInterceptorStrategy() {
            return interceptorStrategy;
        }

        public void setInterceptorStrategy(String interceptorStrategy) {
            this.interceptorStrategy = interceptorStrategy;
        }
        
        public Integer getWhitelistExpirationTime() {
            return whitelistExpirationTime;
        }
        
        public void setWhitelistExpirationTime(Integer whitelistExpirationTime) {
            this.whitelistExpirationTime = whitelistExpirationTime;
        }
        
        public Integer getPenaltyExpirationTime() {
            return penaltyExpirationTime;
        }
        
        public void setPenaltyExpirationTime(Integer penaltyExpirationTime) {
            this.penaltyExpirationTime = penaltyExpirationTime;
        }
    }
    
    /**
     * 行为链路规则配置类
     */
    public static class BehaviorChainRule {
        /**
         * 是否启用行为链路Rule：默认启用
         */
        private boolean enabled = true;
        
        /**
         * 链路规则映射，格式：目标接口=前置接口1,前置接口2,...
         * 例如：/api/detail=/api/list
         */
        private List<String> chainRules;
        
        /**
         * 链路有效期（毫秒）：默认300000ms（5分钟）
         */
        private Integer chainExpirationTime = 300000;
        
        /**
         * 白名单有效期（秒）：默认3600秒（1小时）
         */
        private Integer whitelistExpirationTime = 10;
        
        /**
         * 拦截策略：verify（验证码）或deny（直接拒绝），默认为verify
         */
        private String interceptorStrategy = "verify";
        
        /**
         * 触发规则后的惩罚时长（分钟），默认60分钟
         */
        private Integer penaltyExpirationTime = 60;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public List<String> getChainRules() {
            return chainRules;
        }
        
        public void setChainRules(List<String> chainRules) {
            this.chainRules = chainRules;
        }
        
        public Integer getChainExpirationTime() {
            return chainExpirationTime;
        }
        
        public void setChainExpirationTime(Integer chainExpirationTime) {
            this.chainExpirationTime = chainExpirationTime;
        }
        
        public Integer getWhitelistExpirationTime() {
            return whitelistExpirationTime;
        }
        
        public void setWhitelistExpirationTime(Integer whitelistExpirationTime) {
            this.whitelistExpirationTime = whitelistExpirationTime;
        }
        
        public String getInterceptorStrategy() {
            return interceptorStrategy;
        }
        
        public void setInterceptorStrategy(String interceptorStrategy) {
            this.interceptorStrategy = interceptorStrategy;
        }
        
        public Integer getPenaltyExpirationTime() {
            return penaltyExpirationTime;
        }
        
        public void setPenaltyExpirationTime(Integer penaltyExpirationTime) {
            this.penaltyExpirationTime = penaltyExpirationTime;
        }
    }
    
    /**
     * AI配置类
     */
    public static class AIConfig {
        /**
         * MiniMax API配置
         */
        private MiniMaxConfig minimax = new MiniMaxConfig();
        
        public MiniMaxConfig getMinimax() {
            return minimax;
        }
        
        public void setMinimax(MiniMaxConfig minimax) {
            this.minimax = minimax;
        }
    }
    
    /**
     * MiniMax配置类
     */
    public static class MiniMaxConfig {
        /**
         * API密钥
         */
        private String apiKey;
        
        /**
         * 群组ID
         */
        private String groupId;
        
        /**
         * 模型名称，默认为abab5.5-chat
         */
        private String model = "abab5.5-chat";
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getGroupId() {
            return groupId;
        }
        
        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
    }
    
    public AIConfig getAi() {
        return ai;
    }
    
    public void setAi(AIConfig ai) {
        this.ai = ai;
    }
}
