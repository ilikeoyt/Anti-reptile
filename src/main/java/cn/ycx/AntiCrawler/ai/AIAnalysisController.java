package cn.ycx.AntiCrawler.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI分析控制器，提供API接口
 */
@RestController
@RequestMapping("/anti-reptile/ai")
@CrossOrigin(origins = "*")
public class AIAnalysisController {

    @Autowired
    private RequestInfoStorage requestInfoStorage;

    @Autowired
    private MiniMaxAIService miniMaxAIService;

    /**
     * 获取所有请求信息
     */
    @GetMapping("/requests")
    public Map<String, Object> getAllRequests() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<RequestInfo> requests = requestInfoStorage.getAllRequestInfos();
            result.put("success", true);
            result.put("data", requests);
            result.put("count", requests.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取请求信息失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取指定数量的最新请求信息
     */
    @GetMapping("/requests/latest")
    public Map<String, Object> getLatestRequests(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<RequestInfo> requests = requestInfoStorage.getLatestRequestInfos(limit);
            result.put("success", true);
            result.put("data", requests);
            result.put("count", requests.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取最新请求信息失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取请求信息数量
     */
    @GetMapping("/requests/count")
    public Map<String, Object> getRequestCount() {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = requestInfoStorage.getRequestInfoCount();
            result.put("success", true);
            result.put("count", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取请求信息数量失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 清除所有请求信息
     */
    @DeleteMapping("/requests")
    public Map<String, Object> clearAllRequests() {
        Map<String, Object> result = new HashMap<>();
        try {
            requestInfoStorage.clearAllRequestInfos();
            result.put("success", true);
            result.put("message", "清除请求信息成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "清除请求信息失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 分析所有请求信息
     */
    @PostMapping("/analyze")
    public Map<String, Object> analyzeRequests() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<RequestInfo> requests = requestInfoStorage.getAllRequestInfos();
            if (requests.isEmpty()) {
                result.put("success", false);
                result.put("message", "没有可分析的请求信息");
                return result;
            }

            String analysisResult = miniMaxAIService.analyzeRequestInfo(requests);
            
            // 更新所有请求信息的AI分析结果
            for (RequestInfo request : requests) {
                requestInfoStorage.updateAiAnalysisResult(request.getRequestId(), analysisResult);
            }

            result.put("success", true);
            result.put("analysis", analysisResult);
            result.put("analyzedCount", requests.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "AI分析失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 分析指定数量的最新请求信息
     */
    @PostMapping("/analyze/latest")
    public Map<String, Object> analyzeLatestRequests(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<RequestInfo> requests = requestInfoStorage.getLatestRequestInfos(limit);
            if (requests.isEmpty()) {
                result.put("success", false);
                result.put("message", "没有可分析的请求信息");
                return result;
            }

            String analysisResult = miniMaxAIService.analyzeRequestInfo(requests);
            
            // 更新请求信息的AI分析结果
            for (RequestInfo request : requests) {
                requestInfoStorage.updateAiAnalysisResult(request.getRequestId(), analysisResult);
            }

            result.put("success", true);
            result.put("analysis", analysisResult);
            result.put("analyzedCount", requests.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "AI分析失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 流式分析所有请求信息
     */
    @GetMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeRequestsStream() {
        List<RequestInfo> requests = requestInfoStorage.getAllRequestInfos();
        return miniMaxAIService.analyzeRequestInfoStream(requests);
    }

    /**
     * 流式分析指定数量的最新请求信息
     */
    @GetMapping(value = "/analyze/latest/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeLatestRequestsStream(@RequestParam(defaultValue = "10") int limit) {
        List<RequestInfo> requests = requestInfoStorage.getLatestRequestInfos(limit);
        return miniMaxAIService.analyzeRequestInfoStream(requests);
    }
}
