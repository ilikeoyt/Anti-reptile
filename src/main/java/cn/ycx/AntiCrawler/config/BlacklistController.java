package cn.ycx.AntiCrawler.config;

import cn.ycx.AntiCrawler.blackIP.BlackIPManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 黑名单控制器
 * 提供黑名单相关的API接口
 */
@RestController
@RequestMapping("/anti-reptile")
public class BlacklistController {

    private final BlackIPManager blackIPManager;

    @Autowired
    public BlacklistController(BlackIPManager blackIPManager) {
        this.blackIPManager = blackIPManager;
    }

    /**
     * 获取当前黑名单IP列表
     * @return 黑名单IP列表
     */
    @GetMapping("/blacklist")
    public Map<String, Object> getBlacklist() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 重新加载黑名单以确保获取最新数据
            blackIPManager.loadBlacklist();
            
            // 获取所有黑名单IP
            List<String> ips = blackIPManager.getAllBlacklistIPs().stream()
                .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("message", "获取黑名单成功");
            response.put("size", ips.size());
            response.put("ips", ips);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取黑名单失败: " + e.getMessage());
        }
        return response;
    }
}
