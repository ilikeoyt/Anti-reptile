package cn.ycx.AntiCrawler.rule;

import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import cn.ycx.AntiCrawler.config.AntiReptileProperties;
import cn.ycx.AntiCrawler.util.WhitelistManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 行为链路反爬规则
 * 检查IP访问接口的前置链路是否符合要求
 */
public class BehaviorChainRule extends AbstractRule {

    private RedissonClient redissonClient;

    private AntiReptileProperties properties;
    
    private WhitelistManager whitelistManager;
    
    public BehaviorChainRule(RedissonClient redissonClient, AntiReptileProperties properties, WhitelistManager whitelistManager) {
        this.redissonClient = redissonClient;
        this.properties = properties;
        this.whitelistManager = whitelistManager;
    }
    
    public BehaviorChainRule() {
        // 默认构造函数，用于Spring自动配置
    }

    private static final String CHAIN_PREFIX = "behavior_chain_";
    private static final String CHAIN_RULES_MAP = "behavior_chain_rules_map";
    private static final String WILDCARD = "*";
    private static final String BEHAVIOR_CHAIN_WHITELIST_PREFIX = "behavior_chain_whitelist_";
    
    // 缓存解析后的链路规则
    private Map<String, List<String>> parsedChainRules;

    @Override
    protected boolean doExecute(HttpServletRequest request, HttpServletResponse response) {
        if (!properties.getBehaviorChainRule().isEnabled()) {
            return false;
        }
        
        // 获取客户端IP
        String ipAddress = getIpAddr(request);
        
        // 检查IP是否在白名单中，如果在则直接放行
        if (whitelistManager.isInWhitelist(request, "behavior")) {
            System.out.println("[BehaviorChain] IP " + ipAddress + " is in whitelist, bypassing behavior chain check");
            return false;
        }
        // 获取当前请求URI
        String currentUri = request.getRequestURI();
        
        // 统一 URI 格式，移除末尾的斜杠（除了根路径"/"）
        if (currentUri.length() > 1 && currentUri.endsWith("/")) {
            currentUri = currentUri.substring(0, currentUri.length() - 1);
        }
        
        // 解析链路规则（每次请求都重新解析，确保配置更新后能立即生效）
        parseChainRules();
        
        // 打印当前请求信息
        System.out.println("[BehaviorChain] Processing request: IP=" + ipAddress + ", URI=" + currentUri);
        
        // 检查当前URI是否有链路要求（支持通配符匹配）
        List<String> requiredPreUris = null;
        
        // 先尝试精确匹配
        requiredPreUris = parsedChainRules.get(currentUri);
        System.out.println("[BehaviorChain] Exact match for " + currentUri + ": " + requiredPreUris);
        
        // 如果没有精确匹配，尝试通配符匹配
        if (requiredPreUris == null || requiredPreUris.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : parsedChainRules.entrySet()) {
                String pattern = entry.getKey();
                if (isWildcardMatch(pattern, currentUri)) {
                    requiredPreUris = entry.getValue();
                    System.out.println("[BehaviorChain] Wildcard match: pattern=" + pattern + ", uri=" + currentUri + ", pre-uris=" + requiredPreUris);
                    break;
                }
            }
        }
        
        if (requiredPreUris == null || requiredPreUris.isEmpty()) {
            // 没有找到匹配的链路规则，放行但不记录访问
            System.out.println("[BehaviorChain] No matching chain rule found for URI: " + currentUri);
            return false;
        }
        
        // 检查IP是否访问过前置URI
        RSet<String> ipUriSet = redissonClient.getSet(CHAIN_PREFIX + ipAddress);
        boolean hasValidPreUri = false;
        
        // 打印当前链路状态
        System.out.println("[BehaviorChain] Checking IP " + ipAddress + " accessing " + currentUri + ", current chain: " + ipUriSet + ", required pre-uris: " + requiredPreUris);
        
        for (String preUri : requiredPreUris) {
            if (ipUriSet.contains(preUri)) {
                hasValidPreUri = true;
                break;
            }
        }
        
        if (!hasValidPreUri) {
            // 未找到有效前置链路，拦截（不记录当前URI访问）
            System.out.println("Behavior chain blocked: IP " + ipAddress + " accessed " + currentUri + " without required pre-uri");
            return true;
        }
        
        // 有有效前置链路，记录当前URI访问
        recordUriAccess(ipAddress, currentUri);
        return false;
    }
    
    /**
     * 记录IP访问的URI
     */
    private void recordUriAccess(String ipAddress, String uri) {
        RSet<String> ipUriSet = redissonClient.getSet(CHAIN_PREFIX + ipAddress);
        ipUriSet.add(uri);
        // 设置过期时间
        int expirationTime = properties.getBehaviorChainRule().getChainExpirationTime();
        ipUriSet.expire(expirationTime, TimeUnit.MILLISECONDS);
        
        // 打印完整链路
        System.out.println("[BehaviorChain] IP " + ipAddress + " accessed " + uri + ", full chain: " + ipUriSet);
    }
    
    /**
     * 解析链路规则
     */
    private void parseChainRules() {
        parsedChainRules = new HashMap<>();
        List<String> chainRules = properties.getBehaviorChainRule().getChainRules();
        
        System.out.println("[BehaviorChain] Parsing chain rules: " + chainRules);
        System.out.println("[BehaviorChain] Chain rules enabled: " + properties.getBehaviorChainRule().isEnabled());
        System.out.println("[BehaviorChain] Chain rules list: " + chainRules);
        
        if (chainRules != null && !chainRules.isEmpty()) {
            for (String rule : chainRules) {
                System.out.println("[BehaviorChain] Processing rule: '" + rule + "'");
                String[] parts = rule.split("=");
                System.out.println("[BehaviorChain] Split result length: " + parts.length);
                for (int i = 0; i < parts.length; i++) {
                    System.out.println("[BehaviorChain] Part " + i + ": '" + parts[i] + "'");
                }
                
                if (parts.length == 2) {
                    String targetUri = parts[0].trim();
                    // 统一 URI 格式，移除末尾的斜杠（除了根路径"/"）
                    if (targetUri.length() > 1 && targetUri.endsWith("/")) {
                        targetUri = targetUri.substring(0, targetUri.length() - 1);
                    }
                    
                    String[] preUrisArray = parts[1].split(",");
                    List<String> preUris = new ArrayList<>();
                    for (String preUri : preUrisArray) {
                        String trimmedPreUri = preUri.trim();
                        // 统一 URI 格式，移除末尾的斜杠（除了根路径"/"）
                        if (trimmedPreUri.length() > 1 && trimmedPreUri.endsWith("/")) {
                            trimmedPreUri = trimmedPreUri.substring(0, trimmedPreUri.length() - 1);
                        }
                        preUris.add(trimmedPreUri);
                    }
                    
                    parsedChainRules.put(targetUri, preUris);
                    System.out.println("[BehaviorChain] Parsed rule: " + targetUri + " -> " + preUris);
                } else {
                    System.out.println("[BehaviorChain] Invalid rule format: " + rule);
                }
            }
        } else {
            System.out.println("[BehaviorChain] Chain rules is null or empty");
        }
        
        System.out.println("[BehaviorChain] Parsed chain rules map: " + parsedChainRules);
        System.out.println("[BehaviorChain] Parsed chain rules map size: " + parsedChainRules.size());
    }
    

    
    /**
     * 获取客户端IP地址
     */
    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理IPv6回环地址
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
    

    
    @Override
    public int getOrder() {
        // 优先级：高于UA规则，低于Cookie规则
        return 1;
    }
    
    @Override
    public void reset(HttpServletRequest request, String realRequestUri) {
        String ipAddress = getIpAddr(request);
        // 清除IP的链路记录
        RSet<String> ipUriSet = redissonClient.getSet(CHAIN_PREFIX + ipAddress);
        ipUriSet.clear();
        // 清除缓存的规则，以便重新解析
        parsedChainRules = null;
        
        /**
         * 添加到白名单
         */
        int whitelistExpirationTime = properties.getBehaviorChainRule().getWhitelistExpirationTime();
        whitelistManager.addToWhitelist(request, "behavior", whitelistExpirationTime);
    }
    
    @Override
    public String getInterceptorStrategy() {
        return properties.getBehaviorChainRule().getInterceptorStrategy();
    }
    
    /**
     * 通配符匹配方法
     * @param pattern 通配符模式，例如 /product/*
     * @param uri 实际请求URI，例如 /product/2
     * @return 是否匹配
     */
    private boolean isWildcardMatch(String pattern, String uri) {
        // 将通配符模式转换为正则表达式
        // 1. 将特殊字符转义
        String regex = pattern.replaceAll("([\\+\\.\\[\\]\\(\\)\\{\\}\\^\\$\\|\\?\\*\\\\])", "\\$1");
        // 2. 将 * 替换为 .* (匹配任意字符)
        regex = regex.replaceAll("\\*", ".*");
        // 3. 添加开始和结束标记，确保完全匹配
        regex = "^" + regex + "$";
        
        // 使用正则表达式匹配
        return uri.matches(regex);
    }
}
