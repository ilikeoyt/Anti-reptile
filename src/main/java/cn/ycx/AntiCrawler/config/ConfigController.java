package cn.ycx.AntiCrawler.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理控制器
 */
@RestController
@RequestMapping("/anti-reptile")
@CrossOrigin(origins = "*")
public class ConfigController {

    @Autowired
    private AntiReptileProperties antiReptileProperties;

    @Autowired
    private ConfigManager configManager;

    /**
     * 访问配置页面
     */
    @GetMapping("/config/page")
    public String configPage() {
        return "redirect:/ycx-config/index.html";
    }

    /**
     * 获取当前配置
     */
    @GetMapping("/config")
    public AntiReptileProperties getConfig() {
        return antiReptileProperties;
    }

    /**
     * 更新配置
     */
    @PostMapping("/config")
    public Map<String, Object> updateConfig(@RequestBody AntiReptileProperties newConfig) {
        Map<String, Object> result = new HashMap<>();
        try {
            configManager.updateConfig(newConfig);
            result.put("success", true);
            result.put("message", "配置更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "配置更新失败: " + e.getMessage());
        }
        return result;
    }
}