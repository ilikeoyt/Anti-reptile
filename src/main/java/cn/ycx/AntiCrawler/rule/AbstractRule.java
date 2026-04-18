package cn.ycx.AntiCrawler.rule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ycx
 * @since 2025/12
 */
public abstract class AbstractRule implements AntiReptileRule {


    @Override
    public boolean execute(HttpServletRequest request, HttpServletResponse response) {
        return doExecute(request,response);
    }

    protected abstract boolean doExecute(HttpServletRequest request, HttpServletResponse response);

    @Override
    public void reset(HttpServletRequest request, String realRequestUri) {
        //默认实现
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getInterceptorStrategy() {
        //默认实现，返回verify
        return "verify";
    }
}
