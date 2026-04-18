package cn.ycx.AntiCrawler.interceptor;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import cn.ycx.AntiCrawler.annotation.AntiReptile;
import cn.ycx.AntiCrawler.ai.RequestInfo;
import cn.ycx.AntiCrawler.ai.RequestInfoCollector;
import cn.ycx.AntiCrawler.ai.RequestInfoStorage;
import cn.ycx.AntiCrawler.config.AntiReptileProperties;
import cn.ycx.AntiCrawler.module.VerifyImageDTO;
import cn.ycx.AntiCrawler.rule.AntiReptileRule;
import cn.ycx.AntiCrawler.rule.BehaviorChainRule;
import cn.ycx.AntiCrawler.rule.CookieRule;
import cn.ycx.AntiCrawler.rule.IpRule;
import cn.ycx.AntiCrawler.rule.RuleActuator;
import cn.ycx.AntiCrawler.rule.UaRule;
import cn.ycx.AntiCrawler.util.CrosUtil;
import cn.ycx.AntiCrawler.util.VerifyImageUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author ycx
 * @since 2025/12/10 17:45
 */
public class AntiReptileInterceptor extends HandlerInterceptorAdapter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();


    private String antiReptileForm;
    private String denyForm;

    private RuleActuator actuator;

    private VerifyImageUtil verifyImageUtil;

    private RequestInfoStorage requestInfoStorage;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    public void init(ServletContext context) {
        // 加载验证码模板
        ClassPathResource classPathResource = new ClassPathResource("verify/index.html");
        try {
            classPathResource.getInputStream();
            byte[] bytes = FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            this.antiReptileForm = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("反爬虫验证模板加载失败！");
            e.printStackTrace();
        }
        
        // 加载拒绝访问模板
        ClassPathResource denyResource = new ClassPathResource("deny/deny.html");
        try {
            denyResource.getInputStream();
            byte[] denyBytes = FileCopyUtils.copyToByteArray(denyResource.getInputStream());
            this.denyForm = new String(denyBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("反爬虫拒绝访问模板加载失败！");
            e.printStackTrace();
        }
        
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
        assert ctx != null;
        this.actuator = ctx.getBean(RuleActuator.class);
        this.verifyImageUtil = ctx.getBean(VerifyImageUtil.class);
        this.requestInfoStorage = ctx.getBean(RequestInfoStorage.class);
    }
    
    /**
     * 获取最新的AntiReptileProperties配置
     */
    private AntiReptileProperties getLatestConfig(HttpServletRequest request) {
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
        assert ctx != null;
        return ctx.getBean(AntiReptileProperties.class);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!initialized.get()) {
            init(request.getServletContext());
            initialized.set(true);
        }
        
        String requestUrl = request.getRequestURI();
        
        // 过滤特定路径的请求，不拦截也不记录
        if (requestUrl.contains("/.well-known/appspecific/com.chrome.devtools.json") || 
            requestUrl.equals("/error")) {
            System.out.println("[AntiReptileInterceptor] Filtered request: " + requestUrl);
            return true;
        }
        
        HandlerMethod handlerMethod;
        try {
            handlerMethod = (HandlerMethod) handler;
        } catch (ClassCastException e) {
            // 收集并保存请求信息（非HandlerMethod请求）
            RequestInfo requestInfo = RequestInfoCollector.collectRequestInfo(request, "正常请求", "无");
            requestInfo.setResponseStatus(200);
            requestInfo.setResponseTime(System.currentTimeMillis() - requestInfo.getRequestTime().getTime());
            requestInfoStorage.saveRequestInfo(requestInfo);
            return true;
        }
        Method method = handlerMethod.getMethod();
        AntiReptile antiReptile = AnnotationUtils.findAnnotation(method, AntiReptile.class);
        boolean isAntiReptileAnnotation = antiReptile != null;
        System.out.println("[AntiReptileInterceptor] Request URL: " + requestUrl + ", has @AntiReptile: " + isAntiReptileAnnotation);
        
        // 收集并保存请求信息（所有请求）
        RequestInfo requestInfo = RequestInfoCollector.collectRequestInfo(request, "正常请求", "无");
        
        if (isIntercept(request, requestUrl, isAntiReptileAnnotation)) {
            System.out.println("[AntiReptileInterceptor] Checking rules for: " + requestUrl);
            // 获取触发的规则
            AntiReptileRule triggeredRule = actuator.getTriggeredRule(request, response);
            if (triggeredRule != null) {
                System.out.println("[AntiReptileInterceptor] Rule triggered: " + triggeredRule.getClass().getSimpleName());
                
                // 更新请求信息
                requestInfo.setInterceptReason("规则触发");
                requestInfo.setInterceptRule(triggeredRule.getClass().getSimpleName());
                requestInfo.setResponseStatus(509);
                
                CrosUtil.setCrosHeader(response);
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(509);
                
                // 使用触发规则的拦截策略
                String ruleInterceptorStrategy = triggeredRule.getInterceptorStrategy();
                if ("deny".equals(ruleInterceptorStrategy)) {
                    // 直接拒绝访问，显示deny页面
                    response.getWriter().write(this.denyForm);
                } else {
                    // 默认使用验证码验证
                    VerifyImageDTO verifyImage = verifyImageUtil.generateVerifyImg();
                    verifyImageUtil.saveVerifyCodeToRedis(verifyImage);
                    // 保存触发的规则类型
                    String ruleType = getRuleType(triggeredRule);
                    verifyImageUtil.saveRuleTypeToRedis(verifyImage.getVerifyId(), ruleType);
                    String str1 = this.antiReptileForm.replace("verifyId_value", verifyImage.getVerifyId());
                    String str2 = str1.replaceAll("verifyImg_value", verifyImage.getVerifyImgStr());
                    String str3 = str2.replaceAll("realRequestUri_value", requestUrl);
                    response.getWriter().write(str3);
                }
                response.getWriter().close();
                saveRequestInfo(requestInfo);
                return false;
            }
        }
        
        // 计算响应时间并保存请求信息
        requestInfo.setResponseTime(System.currentTimeMillis() - requestInfo.getRequestTime().getTime());
        if (requestInfo.getResponseStatus() == 0) {
            requestInfo.setResponseStatus(200);
        }
        requestInfoStorage.saveRequestInfo(requestInfo);
        
        return true;
    }

    /**
     * 是否拦截
     * @param request HttpServletRequest
     * @param requestUrl 请求uri
     * @param isAntiReptileAnnotation 是否有AntiReptile注解
     * @return 是否拦截
     */
    public boolean isIntercept(HttpServletRequest request, String requestUrl, Boolean isAntiReptileAnnotation) {
        // 获取最新的配置
        AntiReptileProperties properties = getLatestConfig(request);
        List<String> includeUrls = properties.getIncludeUrls();
        List<String> excludeUrls = properties.getExcludeUrls();
        boolean globalFilterMode = properties.isGlobalFilterMode();
        
        if (includeUrls == null) {
            includeUrls = new ArrayList<>();
        }
        if (excludeUrls == null) {
            excludeUrls = new ArrayList<>();
        }
        
        // 首先检查是否在排除列表中
        for (String excludeUrl : excludeUrls) {
            if (Pattern.matches(excludeUrl, requestUrl)) {
                System.out.println("[AntiReptileInterceptor] URL excluded: " + requestUrl + ", pattern: " + excludeUrl);
                return false;
            }
        }
        
        // 检查是否在包含列表中
        boolean isInIncludeUrls = includeUrls.contains(requestUrl);
        for (String includeUrl : includeUrls) {
            if (Pattern.matches(includeUrl, requestUrl)) {
                isInIncludeUrls = true;
                break;
            }
        }
        
        // 只有在全局过滤模式或在包含列表中时才拦截
        // 不再考虑@AntiReptile注解，因为我们希望通过配置来控制拦截的路径
        if (globalFilterMode || isInIncludeUrls) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * 根据规则实例获取规则类型
     * @param rule 规则实例
     * @return 规则类型（ip, ua, cookie, behavior）
     */
    private String getRuleType(AntiReptileRule rule) {
        if (rule instanceof IpRule) {
            return "ip";
        } else if (rule instanceof UaRule) {
            return "ua";
        } else if (rule instanceof CookieRule) {
            return "cookie";
        } else if (rule instanceof BehaviorChainRule) {
            return "behavior";
        }
        return "";
    }
}
