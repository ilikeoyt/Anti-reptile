package cn.ycx.AntiCrawler.blackIP;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 黑名单定时任务调度器
 * 定期运行BotScout爬虫更新黑名单
 */
@Component
public class BlackIPScheduler {

    private static final String BLACKLIST_FILE;
    private final BlackIPManager blackIPManager;
    private final BotScoutCrawler botScoutCrawler;

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
        System.out.println("[BlackIPScheduler] 黑名单文件路径: " + BLACKLIST_FILE);
    }

    public BlackIPScheduler(BlackIPManager blackIPManager) {
        this.blackIPManager = blackIPManager;
        // 使用与BlackIPManager相同的黑名单文件路径
        this.botScoutCrawler = new BotScoutCrawler(BLACKLIST_FILE);
    }

    /**
     * 应用启动时立即执行一次
     */
    @PostConstruct
    public void init() {
        System.out.println("[BlackIPScheduler] 启动时执行黑名单更新");
        runCrawler();
    }

    /**
     * 每半小时运行一次爬虫
     */
    @Scheduled(fixedRate = 1800000) // 半小时 = 30 * 60 * 1000 = 1800000毫秒
    public void scheduledUpdate() {
        System.out.println("[BlackIPScheduler] 执行定时黑名单更新");
        runCrawler();
    }

    /**
     * 运行Java爬虫
     */
    private void runCrawler() {
        try {
            System.out.println("[BlackIPScheduler] 开始运行BotScout爬虫...");

            // 执行Java爬虫
            botScoutCrawler.run();

            System.out.println("[BlackIPScheduler] 爬虫执行成功");
            // 重新加载黑名单
            blackIPManager.loadBlacklist();

        } catch (Exception e) {
            System.err.println("[BlackIPScheduler] 运行爬虫失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 手动触发更新
     */
    public void triggerUpdate() {
        runCrawler();
    }
}
