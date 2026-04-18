package cn.ycx.AntiCrawler.config;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * 配置管理器，用于实现配置的热加载
 */
@Component
public class ConfigManager {

    private final AntiReptileProperties antiReptileProperties;

    public ConfigManager(AntiReptileProperties antiReptileProperties) {
        this.antiReptileProperties = antiReptileProperties;
    }

    /**
     * 更新配置
     */
    public void updateConfig(AntiReptileProperties newConfig) {
        // 使用 BeanUtils 复制属性，实现配置的热更新
        BeanUtils.copyProperties(newConfig, antiReptileProperties);
        
        // 处理嵌套对象的复制
        if (newConfig.getIpRule() != null) {
            BeanUtils.copyProperties(newConfig.getIpRule(), antiReptileProperties.getIpRule());
        }
        
        if (newConfig.getUaRule() != null) {
            BeanUtils.copyProperties(newConfig.getUaRule(), antiReptileProperties.getUaRule());
        }
        
        if (newConfig.getCookieRule() != null) {
            BeanUtils.copyProperties(newConfig.getCookieRule(), antiReptileProperties.getCookieRule());
        }
        
        if (newConfig.getBehaviorChainRule() != null) {
            // 显式复制行为链路规则的所有属性
            AntiReptileProperties.BehaviorChainRule behaviorChainRule = antiReptileProperties.getBehaviorChainRule();
            AntiReptileProperties.BehaviorChainRule newBehaviorChainRule = newConfig.getBehaviorChainRule();
            
            behaviorChainRule.setEnabled(newBehaviorChainRule.isEnabled());
            behaviorChainRule.setChainRules(newBehaviorChainRule.getChainRules());
            behaviorChainRule.setChainExpirationTime(newBehaviorChainRule.getChainExpirationTime());
            behaviorChainRule.setWhitelistExpirationTime(newBehaviorChainRule.getWhitelistExpirationTime());
            behaviorChainRule.setInterceptorStrategy(newBehaviorChainRule.getInterceptorStrategy());
            behaviorChainRule.setPenaltyExpirationTime(newBehaviorChainRule.getPenaltyExpirationTime());
            
            System.out.println("[ConfigManager] 行为链路规则已更新：" + behaviorChainRule.getChainRules());
        }
        
        // 处理 AI 配置的复制
        if (newConfig.getAi() != null) {
            AntiReptileProperties.AIConfig aiConfig = antiReptileProperties.getAi();
            AntiReptileProperties.AIConfig newAiConfig = newConfig.getAi();
            
            // 复制 MiniMax 配置
            if (newAiConfig.getMinimax() != null) {
                AntiReptileProperties.MiniMaxConfig minimaxConfig = aiConfig.getMinimax();
                AntiReptileProperties.MiniMaxConfig newMinimaxConfig = newAiConfig.getMinimax();
                
                minimaxConfig.setApiKey(newMinimaxConfig.getApiKey());
                minimaxConfig.setGroupId(newMinimaxConfig.getGroupId());
                minimaxConfig.setModel(newMinimaxConfig.getModel());
                
                System.out.println("[ConfigManager] AI 配置已更新，API Key: " + (minimaxConfig.getApiKey() != null ? "已设置" : "未设置"));
            }
        }
    }

    /**
     * 获取当前配置
     */
    public AntiReptileProperties getConfig() {
        return antiReptileProperties;
    }
}