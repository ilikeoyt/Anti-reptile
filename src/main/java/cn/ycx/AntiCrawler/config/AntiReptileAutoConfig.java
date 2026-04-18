package cn.ycx.AntiCrawler.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.redisson.api.RedissonClient;

import cn.ycx.AntiCrawler.blackIP.BlackIPManager;
import cn.ycx.AntiCrawler.blackIP.BlackIPScheduler;
import cn.ycx.AntiCrawler.ValidateFormService;
import cn.ycx.AntiCrawler.constant.AntiReptileConsts;
import cn.ycx.AntiCrawler.interceptor.AntiReptileInterceptor;
import cn.ycx.AntiCrawler.rule.AntiReptileRule;
import cn.ycx.AntiCrawler.rule.IpRule;
import cn.ycx.AntiCrawler.rule.RuleActuator;
import cn.ycx.AntiCrawler.rule.UaRule;
import cn.ycx.AntiCrawler.rule.CookieRule;
import cn.ycx.AntiCrawler.rule.BehaviorChainRule;
import cn.ycx.AntiCrawler.servlet.RefreshFormServlet;
import cn.ycx.AntiCrawler.servlet.ValidateFormServlet;
import cn.ycx.AntiCrawler.util.VerifyImageUtil;
import cn.ycx.AntiCrawler.util.WhitelistManager;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RedissonAutoConfiguration 的 AutoConfigureOrder 为默认值(0)，此处在它后面加载
 * @author ycx
 * @since 2025/12
 */
@Configuration
@EnableConfigurationProperties(AntiReptileProperties.class)
@ConditionalOnProperty(prefix = "anti.reptile.manager", value = "enabled", havingValue = "true")
@Import({RedissonAutoConfig.class, WebMvcConfig.class})
@EnableScheduling
public class AntiReptileAutoConfig {

    @Bean
    public ServletRegistrationBean validateFormServlet() {
        return new ServletRegistrationBean(new ValidateFormServlet(), AntiReptileConsts.VALIDATE_REQUEST_URI);
    }

    @Bean
    public ServletRegistrationBean refreshFormServlet() {
        return new ServletRegistrationBean(new RefreshFormServlet(), AntiReptileConsts.REFRESH_REQUEST_URI);
    }

    @Bean
    @ConditionalOnProperty(prefix = "anti.reptile.manager.ip-rule",value = "enabled", havingValue = "true", matchIfMissing = true)
    public IpRule ipRule(RedissonClient redissonClient, AntiReptileProperties properties, WhitelistManager whitelistManager, BlackIPManager blackIPManager){
        return new IpRule(redissonClient, properties, whitelistManager, blackIPManager);
    }

    @Bean
    @ConditionalOnProperty(prefix = "anti.reptile.manager.ua-rule",value = "enabled", havingValue = "true", matchIfMissing = true)
    public UaRule uaRule(AntiReptileProperties properties, WhitelistManager whitelistManager) {
        return new UaRule(properties, whitelistManager);
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "anti.reptile.manager.cookie-rule",value = "enabled", havingValue = "true", matchIfMissing = true)
    public CookieRule cookieRule(AntiReptileProperties properties, WhitelistManager whitelistManager) {
        return new CookieRule(properties, whitelistManager);
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "anti.reptile.manager.behavior-chain-rule",value = "enabled", havingValue = "true", matchIfMissing = true)
    public BehaviorChainRule behaviorChainRule(RedissonClient redissonClient, AntiReptileProperties properties, WhitelistManager whitelistManager) {
        return new BehaviorChainRule(redissonClient, properties, whitelistManager);
    }

    @Bean
    public VerifyImageUtil verifyImageUtil() {
        return new VerifyImageUtil();
    }

    @Bean
    public RuleActuator ruleActuator(final List<AntiReptileRule> rules){
        final List<AntiReptileRule> antiReptileRules = rules.stream()
                .sorted(Comparator.comparingInt(AntiReptileRule::getOrder)).collect(Collectors.toList());
        return new RuleActuator(antiReptileRules);
    }

    @Bean
    public ValidateFormService validateFormService(){
        return new ValidateFormService();
    }

    @Bean
    public WhitelistManager whitelistManager() {
        return new WhitelistManager();
    }

    @Bean
    public BlackIPManager blackIPManager() {
        return new BlackIPManager();
    }

    @Bean
    public BlackIPScheduler blackIPScheduler(BlackIPManager blackIPManager) {
        return new BlackIPScheduler(blackIPManager);
    }

    @Bean
    public AntiReptileInterceptor antiReptileInterceptor() {
        return new AntiReptileInterceptor();
    }

    @Bean
    public ConfigManager configManager(AntiReptileProperties antiReptileProperties) {
        return new ConfigManager(antiReptileProperties);
    }

    @Bean
    public ConfigController configController() {
        return new ConfigController();
    }

    @Bean
    public FilterRegistrationBean corsFilter() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(new CorsFilter());
        registrationBean.addUrlPatterns("/anti-reptile/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean authFilter() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(new AuthFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }

    @Bean
    public AuthController antiReptileAuthController() {
        return new AuthController();
    }

    @Bean
    public BlacklistController blacklistController() {
        return new BlacklistController(blackIPManager());
    }

    @Bean
    public cn.ycx.AntiCrawler.ai.AIAnalysisController aiAnalysisController() {
        return new cn.ycx.AntiCrawler.ai.AIAnalysisController();
    }

    @Bean
    public cn.ycx.AntiCrawler.ai.RequestInfoStorage requestInfoStorage() {
        return new cn.ycx.AntiCrawler.ai.RequestInfoStorage();
    }

    @Bean
    public cn.ycx.AntiCrawler.ai.MiniMaxAIService miniMaxAIService(AntiReptileProperties antiReptileProperties) {
        return new cn.ycx.AntiCrawler.ai.MiniMaxAIService(antiReptileProperties);
    }

    @Bean
    public ServletContextInitializer servletContextInitializer(AntiReptileProperties antiReptileProperties) {
        return servletContext -> {
            // 将AntiReptileProperties存储到ServletContext中，供AuthFilter使用
            servletContext.setAttribute(AntiReptileProperties.class.getName(), antiReptileProperties);
        };
    }

}
