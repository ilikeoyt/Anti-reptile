package cn.ycx.AntiCrawler.util;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * 白名单管理器
 * 用于管理反爬虫白名单，支持按规则类型设置不同的白名单时长
 */
@Component
public class WhitelistManager {

    @Autowired
    private RedissonClient redissonClient;

    // 白名单前缀
    public static final String WHITELIST_PREFIX = "anti_reptile_whitelist_";

    /**
     * 检查IP是否在指定规则的白名单中
     * @param request HTTP请求
     * @param ruleType 规则类型（ip, ua, cookie, behavior）
     * @return 是否在白名单中
     */
    public boolean isInWhitelist(HttpServletRequest request, String ruleType) {
        String ipAddress = getIpAddr(request);
        RBucket<String> bucket = redissonClient.getBucket(WHITELIST_PREFIX + ruleType + "_" + ipAddress);
        return bucket.isExists();
    }

    /**
     * 将IP添加到指定规则的白名单中
     * @param request HTTP请求
     * @param ruleType 规则类型（ip, ua, cookie, behavior）
     * @param expirationTime 过期时间（秒）
     */
    public void addToWhitelist(HttpServletRequest request, String ruleType, int expirationTime) {
        String ipAddress = getIpAddr(request);
        RBucket<String> bucket = redissonClient.getBucket(WHITELIST_PREFIX + ruleType + "_" + ipAddress);
        bucket.set(ipAddress, expirationTime, TimeUnit.SECONDS);
        System.out.println("[Whitelist] IP " + ipAddress + " added to " + ruleType + " whitelist, expires in " + expirationTime + " seconds");
    }

    /**
     * 从指定规则的白名单中移除IP
     * @param request HTTP请求
     * @param ruleType 规则类型（ip, ua, cookie, behavior）
     */
    public void removeFromWhitelist(HttpServletRequest request, String ruleType) {
        String ipAddress = getIpAddr(request);
        RBucket<String> bucket = redissonClient.getBucket(WHITELIST_PREFIX + ruleType + "_" + ipAddress);
        bucket.delete();
    }

    /**
     * 获取客户端真实IP地址
     * @param request HTTP请求
     * @return IP地址
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
        return ip;
    }
}
