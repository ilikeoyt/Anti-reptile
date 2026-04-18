package cn.ycx.AntiCrawler.rule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author ycx
 * @since 2025/12
 */
public class RuleActuator {

    private  List<AntiReptileRule> ruleList;

    public RuleActuator(List<AntiReptileRule> rules) {
        ruleList = rules;
    }

    /**
     * 是否允许通过请求
     * @param request 请求
     * @param response 响应
     * @return 请求是否允许通过
     */
    public boolean isAllowed(HttpServletRequest request , HttpServletResponse response){
        for (AntiReptileRule rule: ruleList){
            if (rule.execute(request,response)){
                return false;
            }
        }
        return true;
    }

    /**
     * 获取触发的规则
     * @param request 请求
     * @param response 响应
     * @return 触发的规则，未触发返回null
     */
    public AntiReptileRule getTriggeredRule(HttpServletRequest request, HttpServletResponse response) {
        for (AntiReptileRule rule: ruleList){
            if (rule.execute(request,response)){
                return rule;
            }
        }
        return null;
    }

    public void reset(HttpServletRequest request, String realRequestUri){
       ruleList.forEach(rule -> rule.reset(request, realRequestUri));
    }
    
    /**
     * 只对指定类型的规则进行 reset 操作
     * @param request 请求
     * @param realRequestUri 原始请求uri
     * @param ruleType 规则类型（ip, ua, cookie, behavior）
     */
    public void resetByRuleType(HttpServletRequest request, String realRequestUri, String ruleType){
       ruleList.forEach(rule -> {
           String currentRuleType = getRuleType(rule);
           if (ruleType.equals(currentRuleType)) {
               rule.reset(request, realRequestUri);
           }
       });
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
