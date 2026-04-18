package cn.ycx.AntiCrawler.ai;

import cn.ycx.AntiCrawler.config.AntiReptileProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MiniMax AI服务集成类
 */
@Service
public class MiniMaxAIService {

    private AntiReptileProperties antiReptileProperties;
    
    @Autowired
    public MiniMaxAIService(AntiReptileProperties antiReptileProperties) {
        this.antiReptileProperties = antiReptileProperties;
    }
    
    public MiniMaxAIService() {
    }

    private static final String API_URL = "https://api.minimax.chat/v1/text/chatcompletion_v2";

    /**
     * 分析请求信息
     */
    public String analyzeRequestInfo(List<RequestInfo> requestInfos) {
        if (requestInfos == null || requestInfos.isEmpty()) {
            return "没有可分析的请求信息";
        }

        String prompt = buildAnalysisPrompt(requestInfos);
        return callMiniMaxAPI(prompt);
    }

    /**
     * 流式分析请求信息
     */
    public SseEmitter analyzeRequestInfoStream(List<RequestInfo> requestInfos) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        executor.execute(() -> {
            try {
                if (requestInfos == null || requestInfos.isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data("没有可分析的请求信息"));
                    emitter.complete();
                    return;
                }

                String prompt = buildAnalysisPrompt(requestInfos);
                callMiniMaxAPIStream(prompt, emitter);
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI分析失败: " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                executor.shutdown();
            }
        });
        
        return emitter;
    }

    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(List<RequestInfo> requestInfos) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下请求信息，从多个维度进行是否为爬虫流量的分析：\n\n");
        
        prompt.append("## 请求信息列表\n\n");
        for (int i = 0; i < requestInfos.size(); i++) {
            RequestInfo info = requestInfos.get(i);
            prompt.append("### 请求 ").append(i + 1).append("\n");
            prompt.append("- 请求ID: ").append(info.getRequestId()).append("\n");
            prompt.append("- 请求时间: ").append(info.getRequestTime()).append("\n");
            prompt.append("- 请求方法: ").append(info.getRequestMethod()).append("\n");
            prompt.append("- 请求URL: ").append(info.getRequestUrl()).append("\n");
            prompt.append("- 查询参数: ").append(info.getQueryString() != null ? info.getQueryString() : "无").append("\n");
            prompt.append("- 客户端IP: ").append(info.getRemoteAddr()).append("\n");
            prompt.append("- User-Agent: ").append(info.getUserAgent()).append("\n");
            prompt.append("- Referer: ").append(info.getReferer() != null ? info.getReferer() : "无").append("\n");
            prompt.append("- Cookie: ").append(truncateString(info.getCookies(), 200)).append("\n");
            prompt.append("- 请求体: ").append(truncateString(info.getRequestBody(), 500)).append("\n");
            prompt.append("- Content-Type: ").append(info.getContentType() != null ? info.getContentType() : "无").append("\n");
            prompt.append("- 协议: ").append(info.getProtocol()).append("\n");
            prompt.append("- 响应状态: ").append(info.getResponseStatus()).append("\n");
            prompt.append("- 响应时间: ").append(info.getResponseTime()).append("ms\n");
            prompt.append("- 拦截规则: ").append(info.getInterceptRule() != null ? info.getInterceptRule() : "无").append("\n");
            prompt.append("- 拦截原因: ").append(info.getInterceptReason() != null ? info.getInterceptReason() : "无").append("\n\n");
        }

        prompt.append("## 分析要求\n\n");
        prompt.append("请从以下维度进行分析：\n");
        prompt.append("1. 爬虫行为模式识别：分析请求是否存在明显的爬虫特征，如高频请求、固定时间间隔、异常请求路径等\n");
        prompt.append("2. IP地址分析：识别异常IP、代理IP、VPN、数据中心IP等可疑来源\n");
        prompt.append("3. User-Agent分析：识别可疑的User-Agent，包括自动化工具、爬虫框架、无头浏览器等\n");
        prompt.append("4. 请求频率分析：分析请求的时间间隔和频率模式，识别异常的请求模式\n");
        prompt.append("5. 请求路径分析：分析请求的目标路径是否存在异常模式，如遍历行为、敏感路径访问、以及脚本实现的直接路径访问而忽略前置路径等\n");
        prompt.append("6. Cookie分析：分析Cookie的完整性、异常情况和会话特征\n");
        prompt.append("7. 拦截规则分析：分析请求被拦截的规则和原因，评估拦截的合理性和准确性\n");
        prompt.append("8. 509状态码代表该条请求已经被反爬规则命中，不是表示服务器带宽超限，请在分析中注意区分\n");
        prompt.append("9. 请求体分析：分析请求体的内容、格式和异常情况\n");
        prompt.append("10. 威胁等级评估：根据以上分析，评估每个请求的威胁等级（低/中/高）\n");
        prompt.append("11. 防护建议：提供针对性的反爬虫防护建议\n\n");
        prompt.append("12. 最终判断结论：针对上面的分析得出一个最终的结论，判断该请求是否为爬虫流量。\n\n");
        
        prompt.append("## 输出格式要求\n\n");
        prompt.append("【重要】请严格按照以下结构化格式输出分析结果，**无论流量有多少条**，不得修改格式、不得添加额外内容、不得遗漏任何部分：\n\n");
        prompt.append("## 爬虫行为模式识别\n[详细分析]\n\n");
        prompt.append("## IP地址分析\n[详细分析]\n\n");
        prompt.append("## User-Agent分析\n[详细分析]\n\n");
        prompt.append("## 请求频率分析\n[详细分析]\n\n");
        prompt.append("## 请求路径分析\n[详细分析]\n\n");
        prompt.append("## Cookie分析\n[详细分析]\n\n");
        prompt.append("## 拦截规则分析\n[详细分析]\n\n");
        prompt.append("## 509状态码分析\n[详细分析]\n\n");
        prompt.append("## 请求体分析\n[详细分析]\n\n");
        prompt.append("## 威胁等级评估\n[详细分析]\n\n");
        prompt.append("## 防护建议\n[详细分析]\n\n");
        prompt.append("## 最终判断结论\n[详细分析]\n\n");
        prompt.append("【重要】输出格式要求：\n");
        prompt.append("1. 必须严格按照上述格式输出，每个标题必须完全一致\n");
        prompt.append("2. 不得添加任何额外的标题或内容\n");
        prompt.append("3. 不得省略任何一个部分\n");
        prompt.append("4. 每个部分的[详细分析]部分必须包含具体、详细的分析内容\n");
        prompt.append("5. 不得使用其他格式，如列表、表格等\n");
        prompt.append("6. 必须使用Markdown格式输出\n");
        prompt.append("7. 不得在输出开始或结束添加任何额外的说明或总结\n");

        return prompt.toString();
    }

    /**
     * 截断字符串
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    /**
     * 调用MiniMax API
     */
    private String callMiniMaxAPI(String prompt) {
        // 获取配置，添加空值检查
        String apiKey = null;
        String groupId = null;
        String model = null;
        
        try {
            if (antiReptileProperties != null && antiReptileProperties.getAi() != null && 
                antiReptileProperties.getAi().getMinimax() != null) {
                apiKey = antiReptileProperties.getAi().getMinimax().getApiKey();
                groupId = antiReptileProperties.getAi().getMinimax().getGroupId();
                model = antiReptileProperties.getAi().getMinimax().getModel();
            }
        } catch (Exception e) {
            System.out.println("[MiniMaxAIService] 获取配置失败: " + e.getMessage());
        }
        
        // 检查API密钥
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("[MiniMaxAIService] API密钥未配置或为空");
            return "AI分析失败: 请先配置MiniMax API密钥";
        }
        
        // 群组ID如果为空，使用空字符串
        if (groupId == null) {
            groupId = "";
        }
        
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            // 构建JSON请求体（根据MiniMax SDK文档）
            String requestBody = String.format(
                "{" +
                "\"model\": \"%s\", " +
                "\"max_tokens\": 2000, " +
                "\"system\": \"You are a helpful assistant\", " +
                "\"messages\": [" +
                "{" +
                "\"role\": \"user\", " +
                "\"content\": [" +
                "{" +
                "\"type\": \"text\", " +
                "\"text\": \"%s\"" +
                "}" +
                "]" +
                "}" +
                "]" +
                "}",
                model, prompt.replace("\"", "\\\"").replace("\n", "\\n")
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    // 简单解析JSON响应，提取text字段
                    String responseStr = response.toString();
                    System.out.println("[MiniMaxAIService] API响应: " + responseStr);
                    
                    // 尝试不同的解析方法
                    try {
                        // 方法1: 查找text字段
                        int textStart = responseStr.indexOf("\"text\":");
                        if (textStart != -1) {
                            // 跳过 "text": 部分
                            textStart += 7;
                            // 跳过可能的空格
                            while (textStart < responseStr.length() && Character.isWhitespace(responseStr.charAt(textStart))) {
                                textStart++;
                            }
                            // 检查是否是字符串
                            if (textStart < responseStr.length() && responseStr.charAt(textStart) == '"') {
                                textStart++;
                                // 查找结束引号，考虑转义情况
                                int textEnd = textStart;
                                boolean escaped = false;
                                while (textEnd < responseStr.length()) {
                                    char c = responseStr.charAt(textEnd);
                                    if (c == '"' && !escaped) {
                                        break;
                                    }
                                    escaped = (c == '\\');
                                    textEnd++;
                                }
                                if (textEnd < responseStr.length()) {
                                    String result = responseStr.substring(textStart, textEnd)
                                        .replace("\\n", "\n")
                                        .replace("\\t", "\t")
                                        .replace("\\\"", "\"");
                                    System.out.println("[MiniMaxAIService] 解析结果: " + result);
                                    return result;
                                }
                            }
                        }
                        
                        // 方法2: 查找choices数组中的第一个元素
                        int choicesStart = responseStr.indexOf("\"choices\":[");
                        if (choicesStart != -1) {
                            int firstChoiceStart = responseStr.indexOf("{", choicesStart);
                            if (firstChoiceStart != -1) {
                                int textStartInChoice = responseStr.indexOf("\"text\":", firstChoiceStart);
                                if (textStartInChoice != -1) {
                                    // 跳过 "text": 部分
                                    textStartInChoice += 7;
                                    // 跳过可能的空格
                                    while (textStartInChoice < responseStr.length() && Character.isWhitespace(responseStr.charAt(textStartInChoice))) {
                                        textStartInChoice++;
                                    }
                                    // 检查是否是字符串
                                    if (textStartInChoice < responseStr.length() && responseStr.charAt(textStartInChoice) == '"') {
                                        textStartInChoice++;
                                        // 查找结束引号，考虑转义情况
                                        int textEnd = textStartInChoice;
                                        boolean escaped = false;
                                        while (textEnd < responseStr.length()) {
                                            char c = responseStr.charAt(textEnd);
                                            if (c == '"' && !escaped) {
                                                break;
                                            }
                                            escaped = (c == '\\');
                                            textEnd++;
                                        }
                                        if (textEnd < responseStr.length()) {
                                            String result = responseStr.substring(textStartInChoice, textEnd)
                                                .replace("\\n", "\n")
                                                .replace("\\t", "\t")
                                                .replace("\\\"", "\"");
                                            System.out.println("[MiniMaxAIService] 解析结果: " + result);
                                            return result;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("[MiniMaxAIService] 解析异常: " + e.getMessage());
                    }
                    
                    return "AI分析失败: 无法解析响应结果，请查看服务器日志获取详细信息";
                }
            } else {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    return "API调用失败: " + errorResponse.toString();
                }
            }
        } catch (Exception e) {
            return "AI分析失败: " + e.getMessage();
        }
    }

    /**
     * 流式调用MiniMax API
     */
    private void callMiniMaxAPIStream(String prompt, SseEmitter emitter) {
        // 获取配置，添加空值检查
        String apiKey = null;
        String groupId = null;
        String model = null;
        
        try {
            if (antiReptileProperties != null && antiReptileProperties.getAi() != null && 
                antiReptileProperties.getAi().getMinimax() != null) {
                apiKey = antiReptileProperties.getAi().getMinimax().getApiKey();
                groupId = antiReptileProperties.getAi().getMinimax().getGroupId();
                model = antiReptileProperties.getAi().getMinimax().getModel();
            }
        } catch (Exception e) {
            System.out.println("[MiniMaxAIService] 获取配置失败: " + e.getMessage());
        }
        
        // 检查API密钥
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("[MiniMaxAIService] API密钥未配置或为空");
            try {
                emitter.send(SseEmitter.event().name("error").data("AI分析失败: 请先配置MiniMax API密钥"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        
        if (groupId == null) {
            groupId = "";
        }
        
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            String requestBody = String.format(
                "{" +
                "\"model\": \"%s\", " +
                "\"max_tokens\": 2000, " +
                "\"stream\": true, " +
                "\"system\": \"You are a helpful assistant\", " +
                "\"messages\": [" +
                "{" +
                "\"role\": \"user\", " +
                "\"content\": [" +
                "{" +
                "\"type\": \"text\", " +
                "\"text\": \"%s\"" +
                "}" +
                "]" +
                "}" +
                "]" +
                "}",
                model, prompt.replace("\"", "\\\"").replace("\n", "\\n")
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("[MiniMaxAIService] Stream line: " + line);
                        
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                System.out.println("[MiniMaxAIService] Stream completed");
                                break;
                            }
                            
                            // 解析流式数据
                            String text = parseStreamData(data);
                            if (text != null && !text.isEmpty()) {
                                emitter.send(SseEmitter.event().name("message").data(text));
                            }
                        }
                    }
                }
            } else {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    emitter.send(SseEmitter.event().name("error").data("API调用失败: " + errorResponse.toString()));
                }
            }
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("error").data("AI分析失败: " + e.getMessage()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 解析流式数据
     */
    private String parseStreamData(String data) {
        try {
            System.out.println("[MiniMaxAIService] Parsing data: " + data);
            
            // 尝试解析delta.content格式（OpenAI风格）
            int deltaStart = data.indexOf("\"delta\":");
            if (deltaStart != -1) {
                int contentStart = data.indexOf("\"content\":\"", deltaStart);
                if (contentStart != -1) {
                    contentStart += 11;
                    int contentEnd = findEndQuote(data, contentStart);
                    if (contentEnd != -1) {
                        String content = data.substring(contentStart, contentEnd);
                        String result = unescapeJson(content);
                        System.out.println("[MiniMaxAIService] Parsed content (delta): " + result);
                        return result;
                    }
                }
            }
            
            // 尝试解析content格式（直接格式）
            int contentStart = data.indexOf("\"content\":\"");
            if (contentStart != -1) {
                contentStart += 11;
                int contentEnd = findEndQuote(data, contentStart);
                if (contentEnd != -1) {
                    String content = data.substring(contentStart, contentEnd);
                    String result = unescapeJson(content);
                    System.out.println("[MiniMaxAIService] Parsed content (direct): " + result);
                    return result;
                }
            }
            
            // 尝试解析text格式
            int textStart = data.indexOf("\"text\":\"");
            if (textStart != -1) {
                textStart += 8;
                int textEnd = findEndQuote(data, textStart);
                if (textEnd != -1) {
                    String text = data.substring(textStart, textEnd);
                    String result = unescapeJson(text);
                    System.out.println("[MiniMaxAIService] Parsed text: " + result);
                    return result;
                }
            }
            
        } catch (Exception e) {
            System.out.println("[MiniMaxAIService] 解析流式数据异常: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }
    
    /**
     * 查找结束引号（考虑转义）
     */
    private int findEndQuote(String data, int start) {
        int end = start;
        boolean escaped = false;
        while (end < data.length()) {
            char c = data.charAt(end);
            if (c == '"' && !escaped) {
                return end;
            }
            escaped = (c == '\\' && !escaped);
            end++;
        }
        return -1;
    }
    
    /**
     * 反转义JSON字符串
     */
    private String unescapeJson(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
}
