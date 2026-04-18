package cn.ycx.AntiCrawler.rule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ycx
 * @since 2025/12
 */
public interface AntiReptileRule {

    /**
     * 执行规则
     * @param request 请求
     * @param response 响应
     * @return 是否拦截
     */
    boolean execute(HttpServletRequest request, HttpServletResponse response);

    /**
     * 重置规则
     * @param request 请求
     * @param realRequestUri 原始请求uri
     */
    void reset(HttpServletRequest request, String realRequestUri);

    /**
     * 规则优先级
     * @return 优先级
     */
    int getOrder();

    /**
     * 获取规则的拦截策略
     * @return 拦截策略：verify（验证码）或deny（直接拒绝）
     */
    String getInterceptorStrategy();

}
