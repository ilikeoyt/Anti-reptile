package cn.ycx.AntiCrawler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import cn.ycx.AntiCrawler.interceptor.AntiReptileInterceptor;

/**
 * @author ycx
 * @since 2025/12/10 17:40
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    private AntiReptileInterceptor antiReptileInterceptor;

    public WebMvcConfig(AntiReptileInterceptor antiReptileInterceptor) {
        this.antiReptileInterceptor = antiReptileInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 排除配置页面和API的拦截
        registry.addInterceptor(this.antiReptileInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/anti-reptile/**")
                .excludePathPatterns("/ycx-config/**")
                .excludePathPatterns("/login")
                .excludePathPatterns("/register")
                .excludePathPatterns("/");
        super.addInterceptors(registry);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 添加配置页面的资源映射
        registry.addResourceHandler("/ycx-config/**")
                .addResourceLocations("classpath:/config/");
        super.addResourceHandlers(registry);
    }
}
