package cn.ycx.AntiCrawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import cn.ycx.AntiCrawler.module.VerifyImageDTO;
import cn.ycx.AntiCrawler.module.VerifyImageVO;
import cn.ycx.AntiCrawler.rule.RuleActuator;
import cn.ycx.AntiCrawler.util.VerifyImageUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ycx
 * @since 2026
 */

public class ValidateFormService {

    @Autowired
    private RuleActuator actuator;

    @Autowired
    private VerifyImageUtil verifyImageUtil;

    public String validate(HttpServletRequest request) throws UnsupportedEncodingException {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setHeaderEncoding("UTF-8");
        List items = null;
        try {
            items = upload.parseRequest(request);
        } catch (FileUploadException e) {
            e.printStackTrace();
            return "{\"result\":false, \"error\":\"server_error\"}";
        }
        
        // 检查items是否为null或空
        if (items == null || items.isEmpty()) {
            System.out.println("No form items found");
            return "{\"result\":false, \"error\":\"server_error\"}";
        }
        
        Map<String, String> params = new HashMap<String, String>();
        for(Object object : items){
            FileItem fileItem = (FileItem) object;
            if (fileItem.isFormField()) {
                params.put(fileItem.getFieldName(), fileItem.getString("UTF-8"));
            }
        }
        
        // 1. 验证浏览器指纹
        String fingerprintStr = params.get("browserFingerprint");
        if (!validateBrowserFingerprint(fingerprintStr)) {
            System.out.println("Browser fingerprint validation failed");
            return "{\"result\":false, \"error\":\"browser_risk\"}";
        }
        
        // 2. 验证请求头一致性
        if (!validateHeaders(request)) {
            System.out.println("Request headers validation failed");
            return "{\"result\":false, \"error\":\"browser_risk\"}";
        }
        
        // 3. 新增：验证请求间隔时间
        String sessionId = request.getSession().getId();
        Long lastRequestTime = (Long) request.getSession().getAttribute("lastRequestTime");
        Long currentTime = System.currentTimeMillis();
        
        if (lastRequestTime != null) {
            long timeDiff = currentTime - lastRequestTime;
            if (timeDiff < 100) { // 100ms内的请求视为异常
                System.out.println("Detected too fast request interval");
                return "{\"result\":false, \"error\":\"browser_risk\"}";
            }
        }
        request.getSession().setAttribute("lastRequestTime", currentTime);
        
        String verifyId = params.get("verifyId");
        String result =  params.get("result");
        String realRequestUri = params.get("realRequestUri");
        String actualResult = verifyImageUtil.getVerifyCodeFromRedis(verifyId);
        if (actualResult != null && request != null && actualResult.equals(result.toLowerCase())) {
            // 获取触发的规则类型
            String ruleType = verifyImageUtil.getRuleTypeFromRedis(verifyId);
            if (ruleType != null && !ruleType.isEmpty()) {
                // 只对触发的规则进行 reset
                actuator.resetByRuleType(request, realRequestUri, ruleType);
                System.out.println("[ValidateForm] Reset rule type: " + ruleType);
            } else {
                // 如果没有规则类型信息，则对所有规则进行 reset（兼容旧逻辑）
                actuator.reset(request, realRequestUri);
            }
            return "{\"result\":true}";
        }
        return "{\"result\":false, \"error\":\"captcha_error\"}";
    }
    
    /**
     * 验证浏览器指纹
     * @param fingerprintStr 浏览器指纹JSON字符串
     * @return 是否验证通过
     */
    private boolean validateBrowserFingerprint(String fingerprintStr) {
        if (fingerprintStr == null) {
            return false;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode fingerprint = mapper.readTree(fingerprintStr);
            
            // 验证基本特征
            String userAgent = fingerprint.get("userAgent").asText();
            if (userAgent == null || userAgent.isEmpty()) {
                return false;
            }
            
            // 新增：检测HeadlessChrome
            if (userAgent.contains("HeadlessChrome")) {
                System.out.println("Detected HeadlessChrome");
                return false;
            }
            
            // 验证屏幕尺寸合理性
            int screenWidth = fingerprint.get("screenWidth").asInt();
            int screenHeight = fingerprint.get("screenHeight").asInt();
            if (screenWidth < 100 || screenHeight < 100 || screenWidth > 8000 || screenHeight > 8000) {
                return false;
            }
            
            // 验证Canvas指纹（简单验证是否存在）
            JsonNode canvasNode = fingerprint.get("canvas");
            if (canvasNode == null || canvasNode.isNull() || canvasNode.asText().isEmpty()) {
                return false;
            }
            
            // 验证支持的特性（真实浏览器通常支持这些特性）
            boolean localStorage = fingerprint.get("localStorage").asBoolean();
            boolean sessionStorage = fingerprint.get("sessionStorage").asBoolean();
            boolean indexedDB = fingerprint.get("indexedDB").asBoolean();
            
            // 如果不支持这些基本特性，很可能是自动化工具
            if (!localStorage || !sessionStorage || !indexedDB) {
                return false;
            }
            
            // 新增：检测自动化控制特征
            if (fingerprint.has("webdriver") && fingerprint.get("webdriver").asBoolean()) {
                System.out.println("Detected webdriver flag");
                return false;
            }
            
            // 新增：检测Chrome自动化标志
            if (fingerprint.has("chromeAutomation") && fingerprint.get("chromeAutomation").asBoolean()) {
                System.out.println("Detected Chrome automation flag");
                return false;
            }
            
            // 新增：检测窗口大小异常
            if (fingerprint.has("windowOuterSize")) {
                String outerSize = fingerprint.get("windowOuterSize").asText();
                if (outerSize.equals("0x0") || outerSize.equals("1x1")) {
                    System.out.println("Detected abnormal window size");
                    return false;
                }
            }
            
            // 新增：检测鼠标移动模式
            if (fingerprint.has("mouseMovePattern")) {
                int moveCount = fingerprint.get("mouseMovePattern").asInt();
                if (moveCount < 3) {
                    System.out.println("Detected abnormal mouse movement pattern");
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            System.out.println("Failed to parse browser fingerprint: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证请求头一致性
     * @param request HTTP请求
     * @return 是否验证通过
     */
    private boolean validateHeaders(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String accept = request.getHeader("Accept");
        String acceptLanguage = request.getHeader("Accept-Language");
        
        // 检查核心请求头是否存在
        if (userAgent == null || accept == null || acceptLanguage == null) {
            return false;
        }
        
        // 检查请求头的合理性
        if (userAgent.contains("HeadlessChrome")) {
            return false;
        }
        
        // 移除过于严格的检查，避免误判正常浏览器
        // 不同浏览器和请求类型可能有不同的请求头
        
        return true;
    }

    public String refresh(HttpServletRequest request) {
        String verifyId = request.getParameter("verifyId");
        verifyImageUtil.deleteVerifyCodeFromRedis(verifyId);
        verifyImageUtil.deleteRuleTypeFromRedis(verifyId);
        VerifyImageDTO verifyImage = verifyImageUtil.generateVerifyImg();
        verifyImageUtil.saveVerifyCodeToRedis(verifyImage);
        VerifyImageVO verifyImageVO = new VerifyImageVO();
        BeanUtils.copyProperties(verifyImage, verifyImageVO);
        String result = "{\"verifyId\": \"" + verifyImageVO.getVerifyId() + "\",\"verifyImgStr\": \"" + verifyImageVO.getVerifyImgStr() + "\"}";
        return result;
    }
}
