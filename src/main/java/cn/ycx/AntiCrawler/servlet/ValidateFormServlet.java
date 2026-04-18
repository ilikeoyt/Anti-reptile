package cn.ycx.AntiCrawler.servlet;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import cn.ycx.AntiCrawler.ValidateFormService;
import cn.ycx.AntiCrawler.util.CrosUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ycx
 */
public class ValidateFormServlet extends HttpServlet {

    private ValidateFormService validateFormService;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    private synchronized void init(ServletContext servletContext) {
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        assert ctx != null;
        this.validateFormService = ctx.getBean(ValidateFormService.class);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!initialized.get()) {
            init(request.getServletContext());
            initialized.set(true);
        }
        String result = validateFormService.validate(request);
        CrosUtil.setCrosHeader(response);
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(200);
        response.getWriter().write(result);
        response.getWriter().close();
        return;
    }
}
