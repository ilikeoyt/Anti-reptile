package cn.ycx.AntiCrawler.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器，用于处理登录和登出请求
 */
@RestController
@RequestMapping("/ycx-config")
public class AuthController {

    @Autowired
    private AntiReptileProperties antiReptileProperties;

    private static final String SESSION_USER_KEY = "anti_reptile_admin_user";

    /**
     * 处理登录请求
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 检查是否启用认证
        if (!antiReptileProperties.isEnableAuth()) {
            result.put("success", true);
            result.put("message", "认证未启用");
            return result;
        }

        // 验证用户名和密码
        if (antiReptileProperties.getAdminUsername().equals(loginRequest.getUsername()) &&
                antiReptileProperties.getAdminPassword().equals(loginRequest.getPassword())) {
            // 登录成功，将用户信息存储到session
            session.setAttribute(SESSION_USER_KEY, loginRequest.getUsername());
            result.put("success", true);
            result.put("message", "登录成功");
        } else {
            // 登录失败
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }

        return result;
    }

    /**
     * 处理登出请求
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 移除session中的用户信息
        session.removeAttribute(SESSION_USER_KEY);
        result.put("success", true);
        result.put("message", "登出成功");

        return result;
    }

    /**
     * 登录请求参数
     */
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
