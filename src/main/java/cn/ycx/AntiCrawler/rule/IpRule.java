package cn.ycx.AntiCrawler.rule;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import cn.ycx.AntiCrawler.blackIP.BlackIPManager;
import cn.ycx.AntiCrawler.config.AntiReptileProperties;
import cn.ycx.AntiCrawler.util.WhitelistManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author ycx
 * @since 2025/12
 */
public class IpRule extends AbstractRule {

    private RedissonClient redissonClient;
    
    private AntiReptileProperties properties;
    
    private WhitelistManager whitelistManager;
    
    private BlackIPManager blackIPManager;
    
    public IpRule(RedissonClient redissonClient, AntiReptileProperties properties, WhitelistManager whitelistManager, BlackIPManager blackIPManager) {
        this.redissonClient = redissonClient;
        this.properties = properties;
        this.whitelistManager = whitelistManager;
        this.blackIPManager = blackIPManager;
    }
    
    public IpRule() {
        // 默认构造函数，用于Spring自动配置
    }

    private static final String RATELIMITER_COUNT_PREFIX = "ratelimiter_request_count";
    private static final String RATELIMITER_EXPIRATIONTIME_PREFIX = "ratelimiter_expirationtime";
    private static final String RATELIMITER_HIT_CRAWLERSTRATEGY = "ratelimiter_hit_crawlerstrategy";

    @Override
    @SuppressWarnings("unchecked")
    protected boolean doExecute(HttpServletRequest request, HttpServletResponse response) {
        if (!properties.getIpRule().isEnabled()) {
            return false;
        }
        
        // 检查是否在白名单中
        if (whitelistManager.isInWhitelist(request, "ip")) {
            System.out.println("[IpRule] IP is in whitelist, bypassing IP check");
            return false;
        }
        
        String ipAddress = getIpAddr(request);
        System.out.println("[IpRule] Checking IP: " + ipAddress + ", URL: " + request.getRequestURI());
        System.out.println("[IpRule] Request Max Size: " + properties.getIpRule().getRequestMaxSize() + ", Expiration Time: " + properties.getIpRule().getExpirationTime() + "ms");
        
        // 1. 检查BotScout黑名单
        if (blackIPManager != null && blackIPManager.isInBlacklist(ipAddress)) {
            System.out.println("[IpRule] IP in BotScout blacklist: " + ipAddress);
            return true;
        }
        
        // 2. 检查配置的黑名单（新增）
        List<String> blackIpList = properties.getIpRule().getBlackIp();
        if (blackIpList != null && blackIpList.size() > 0) {
            for (String blackIp : blackIpList) {
                if (blackIp.endsWith("*")) {
                    blackIp = blackIp.substring(0, blackIp.length() - 1);
                }
                
                // 处理localhost的特殊情况
                if ("127.0.0.1".equals(blackIp) && ("0:0:0:0:0:0:0:1".equals(ipAddress) || "localhost".equals(ipAddress))) {
                    System.out.println("Blacklisted IP blocked (localhost): " + ipAddress);
                    return true;
                }
                
                if (ipAddress.startsWith(blackIp)) {
                    System.out.println("Blacklisted IP blocked: " + ipAddress);
                    return true;
                }
            }
        }
        
        List<String> ignoreIpList = properties.getIpRule().getIgnoreIp();
        if (ignoreIpList != null && ignoreIpList.size() > 0) {
            for (String ignoreIp : ignoreIpList) {
                if (ignoreIp.endsWith("*")) {
                    ignoreIp = ignoreIp.substring(0, ignoreIp.length() - 1);
                }
                if (ipAddress.startsWith(ignoreIp)) {
                    return false;
                }
            }
        }
        String requestUrl = request.getRequestURI();
        //毫秒，默认5000
        int expirationTime = properties.getIpRule().getExpirationTime();
        //最高expirationTime时间内请求数
        int requestMaxSize = properties.getIpRule().getRequestMaxSize();
        RAtomicLong rRequestCount = redissonClient.getAtomicLong(RATELIMITER_COUNT_PREFIX.concat(requestUrl).concat(ipAddress));
        RAtomicLong rExpirationTime = redissonClient.getAtomicLong(RATELIMITER_EXPIRATIONTIME_PREFIX.concat(requestUrl).concat(ipAddress));
        if (!rExpirationTime.isExists()) {
            rRequestCount.set(0L);
            rExpirationTime.set(0L);
            rExpirationTime.expire(expirationTime, TimeUnit.MILLISECONDS);
        } else {
            RMap rHitMap = redissonClient.getMap(RATELIMITER_HIT_CRAWLERSTRATEGY);
            if ((rRequestCount.incrementAndGet() > requestMaxSize) || rHitMap.containsKey(ipAddress)) {
                        //触发爬虫策略，设置惩罚时长
                        int penaltyExpirationTime = properties.getIpRule().getPenaltyExpirationTime();
                        rExpirationTime.expire(penaltyExpirationTime, TimeUnit.MINUTES);
                        //保存触发来源
                        rHitMap.put(ipAddress,requestUrl);
                        System.out.println("Intercepted request, uri: " + requestUrl + ", ip：" + ipAddress + " request " + requestMaxSize + " times in " + expirationTime + " ms, blocked for " + penaltyExpirationTime + " minutes");
                        return true;
                    }
        }
        return false;
    }

    /**
     * 重置已记录规则
     * @param request 请求
     * @param realRequestUri 原始请求uri
     */
    @Override
    public void reset(HttpServletRequest request, String realRequestUri) {
        String ipAddress = getIpAddr(request);
        String requestUrl = realRequestUri;
        /**
         * 重置计数器
         */
        int expirationTime = properties.getIpRule().getExpirationTime();
        RAtomicLong rRequestCount = redissonClient.getAtomicLong(RATELIMITER_COUNT_PREFIX.concat(requestUrl).concat(ipAddress));
        RAtomicLong rExpirationTime = redissonClient.getAtomicLong(RATELIMITER_EXPIRATIONTIME_PREFIX.concat(requestUrl).concat(ipAddress));
        rRequestCount.set(0L);
        rExpirationTime.set(0L);
        rExpirationTime.expire(expirationTime, TimeUnit.MILLISECONDS);
        /**
         * 清除记录
         */
        RMap rHitMap = redissonClient.getMap(RATELIMITER_HIT_CRAWLERSTRATEGY);
        rHitMap.remove(ipAddress);
        
        /**
         * 添加到白名单
         */
        int whitelistExpirationTime = properties.getIpRule().getWhitelistExpirationTime();
        whitelistManager.addToWhitelist(request, "ip", whitelistExpirationTime);
    }

    private static String getIpAddr(HttpServletRequest request) {
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
        return ip;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getInterceptorStrategy() {
        return properties.getIpRule().getInterceptorStrategy();
    }
}
