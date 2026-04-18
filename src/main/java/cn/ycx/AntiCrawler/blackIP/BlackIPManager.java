package cn.ycx.AntiCrawler.blackIP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 黑名单管理器
 * 管理从BotScout等来源获取的恶意IP
 */
@Component
public class BlackIPManager {

    private final Set<String> blacklist = new CopyOnWriteArraySet<>();
    private static final String BLACKLIST_FILE;
    
    static {
        // 尝试多种路径查找
        String[] possiblePaths = {
            // 方案1: 当前工作目录
            System.getProperty("user.dir") + "/src/main/java/cn/ycx/AntiCrawler/blackIP/botscout_blacklist.json",
            // 方案2: ycx-anti-reptile项目目录
            System.getProperty("user.dir").replace("demo", "ycx-anti-reptile") + "/src/main/java/cn/ycx/AntiCrawler/blackIP/botscout_blacklist.json",
            // 方案3: 上级目录
            new File(System.getProperty("user.dir")).getParent() + "/ycx-anti-reptile/src/main/java/cn/ycx/AntiCrawler/blackIP/botscout_blacklist.json"
        };
        
        String foundPath = null;
        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                foundPath = path;
                break;
            }
        }
        
        BLACKLIST_FILE = foundPath != null ? foundPath : possiblePaths[0];
        System.out.println("[BlackIPManager] 黑名单文件路径: " + BLACKLIST_FILE);
    }

    /**
     * 初始化时加载黑名单
     */
    @PostConstruct
    public void init() {
        loadBlacklist();
    }

    /**
     * 加载黑名单
     */
    public void loadBlacklist() {
        try {
            File file = new File(BLACKLIST_FILE);
            if (file.exists()) {
                // 先清空现有黑名单
                blacklist.clear();
                
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(file);
                JsonNode ipsNode = root.get("ips");

                if (ipsNode != null && ipsNode.isArray()) {
                    for (JsonNode ipNode : ipsNode) {
                        String ip = ipNode.asText().trim();
                        if (!ip.isEmpty()) {
                            blacklist.add(ip);
                        }
                    }
                }

                System.out.println("[BlackIPManager] 加载黑名单完成，共 " + blacklist.size() + " 个IP");
            } else {
                System.out.println("[BlackIPManager] 黑名单文件不存在: " + BLACKLIST_FILE);
            }
        } catch (Exception e) {
            System.err.println("[BlackIPManager] 加载黑名单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查IP是否在黑名单中
     * @param ip IP地址
     * @return 是否在黑名单中
     */
    public boolean isInBlacklist(String ip) {
        return blacklist.contains(ip);
    }

    /**
     * 获取黑名单大小
     * @return 黑名单大小
     */
    public int getBlacklistSize() {
        return blacklist.size();
    }

    /**
     * 清空黑名单
     */
    public void clearBlacklist() {
        blacklist.clear();
        System.out.println("[BlackIPManager] 黑名单已清空");
    }

    /**
     * 手动添加IP到黑名单
     * @param ip IP地址
     */
    public void addToBlacklist(String ip) {
        if (!ip.isEmpty() && !blacklist.contains(ip)) {
            blacklist.add(ip);
            System.out.println("[BlackIPManager] 添加IP到黑名单: " + ip);
        }
    }

    /**
     * 从黑名单中移除IP
     * @param ip IP地址
     */
    public void removeFromBlacklist(String ip) {
        if (blacklist.remove(ip)) {
            System.out.println("[BlackIPManager] 从黑名单中移除IP: " + ip);
        }
    }
    
    /**
     * 获取所有黑名单IP
     * @return 黑名单IP集合
     */
    public Set<String> getAllBlacklistIPs() {
        return new HashSet<>(blacklist);
    }
}
