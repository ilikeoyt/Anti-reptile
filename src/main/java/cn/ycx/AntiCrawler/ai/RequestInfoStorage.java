package cn.ycx.AntiCrawler.ai;

import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 请求信息存储类，实现2小时自动过期机制
 */
@Component
public class RequestInfoStorage {

    private static final String REQUEST_INFO_KEY_PREFIX = "anti_reptile:request_info:";
    private static final String REQUEST_INFO_LIST_KEY = "anti_reptile:request_info_list";
    private static final long EXPIRE_MINUTES = 10;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 保存请求信息
     */
    public void saveRequestInfo(RequestInfo requestInfo) {
        String key = REQUEST_INFO_KEY_PREFIX + requestInfo.getRequestId();
        
        // 保存单个请求信息，设置10分钟过期
        redissonClient.getBucket(key).set(requestInfo, EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        // 将请求ID添加到列表中
        RList<String> requestList = redissonClient.getList(REQUEST_INFO_LIST_KEY);
        requestList.add(requestInfo.getRequestId());
        
        // 设置列表过期时间（10分钟）
        requestList.expire(EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 根据请求ID获取请求信息
     */
    public RequestInfo getRequestInfo(String requestId) {
        String key = REQUEST_INFO_KEY_PREFIX + requestId;
        return (RequestInfo) redissonClient.getBucket(key).get();
    }

    /**
     * 获取所有请求信息
     */
    public List<RequestInfo> getAllRequestInfos() {
        List<RequestInfo> requestInfos = new ArrayList<>();
        RList<String> requestList = redissonClient.getList(REQUEST_INFO_LIST_KEY);
        
        for (String requestId : requestList) {
            RequestInfo requestInfo = getRequestInfo(requestId);
            if (requestInfo != null) {
                requestInfos.add(requestInfo);
            }
        }
        
        return requestInfos;
    }

    /**
     * 获取指定数量的最新请求信息
     */
    public List<RequestInfo> getLatestRequestInfos(int limit) {
        List<RequestInfo> allRequestInfos = getAllRequestInfos();
        int size = allRequestInfos.size();
        
        if (size <= limit) {
            return allRequestInfos;
        }
        
        return allRequestInfos.subList(size - limit, size);
    }

    /**
     * 清除所有请求信息
     */
    public void clearAllRequestInfos() {
        RList<String> requestList = redissonClient.getList(REQUEST_INFO_LIST_KEY);
        
        for (String requestId : requestList) {
            String key = REQUEST_INFO_KEY_PREFIX + requestId;
            redissonClient.getBucket(key).delete();
        }
        
        requestList.delete();
    }

    /**
     * 更新请求信息的AI分析结果
     */
    public void updateAiAnalysisResult(String requestId, String analysisResult) {
        RequestInfo requestInfo = getRequestInfo(requestId);
        if (requestInfo != null) {
            requestInfo.setAiAnalysisResult(analysisResult);
            String key = REQUEST_INFO_KEY_PREFIX + requestId;
            redissonClient.getBucket(key).set(requestInfo, EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * 获取请求信息数量
     */
    public int getRequestInfoCount() {
        RList<String> requestList = redissonClient.getList(REQUEST_INFO_LIST_KEY);
        return requestList.size();
    }
}
