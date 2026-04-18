# BotScout IP 黑名单爬虫使用说明

## 功能介绍

定时爬取 `https://botscout.com/last_caught_cache.htm` 上的恶意IP地址，用于反爬虫系统的IP黑名单。

## 安装依赖

```bash
pip install requests schedule
```

## 使用方法

### 1. 立即运行一次

```bash
python botscout_ip_crawler.py --run-once
```

### 2. 定时运行（默认每60分钟）

```bash
python botscout_ip_crawler.py
```

### 3. 自定义定时间隔（例如每30分钟）

```bash
python botscout_ip_crawler.py --interval 30
```

### 4. 自定义输出文件

```bash
python botscout_ip_crawler.py --output my_blacklist.json
```

### 5. 查看统计信息

```bash
python botscout_ip_crawler.py --stats
```

## 输出文件

脚本会生成两个文件：

1. **botscout_blacklist.json** - JSON格式，包含完整信息
   - update_time: 更新时间
   - source: 数据源
   - total_count: IP总数
   - ips: IP地址列表

2. **botscout_blacklist.txt** - 纯文本格式，每行一个IP
   - 可直接用于防火墙规则
   - 便于其他程序读取

## 特性

- ✅ 自动过滤私有IP和保留IP
- ✅ 支持IPv4和IPv6
- ✅ 自动合并新旧数据，避免重复
- ✅ 统计新增IP数量
- ✅ 支持定时自动更新
- ✅ 详细的日志输出

## 集成到反爬虫系统

### Java集成示例

```java
@Component
public class BotScoutBlacklistLoader {
    
    @Value("${botscout.blacklist.file:botscout_blacklist.json}")
    private String blacklistFile;
    
    private Set<String> blacklist = new HashSet<>();
    
    @PostConstruct
    public void loadBlacklist() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(blacklistFile));
            JsonNode ips = root.get("ips");
            
            if (ips != null && ips.isArray()) {
                for (JsonNode ip : ips) {
                    blacklist.add(ip.asText());
                }
            }
            
            System.out.println("加载 BotScout 黑名单: " + blacklist.size() + " 个IP");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean isInBlacklist(String ip) {
        return blacklist.contains(ip);
    }
}
```

### Python集成示例

```python
import json

def load_botscout_blacklist(filepath='botscout_blacklist.json'):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
            return set(data.get('ips', []))
    except Exception as e:
        print(f"加载黑名单失败: {e}")
        return set()

# 使用示例
blacklist = load_botscout_blacklist()
if ip_address in blacklist:
    print(f"IP {ip_address} 在黑名单中")
```

## 注意事项

1. **合法性**：确保使用该脚本符合当地法律法规
2. **误报处理**：BotScout的IP可能存在误报，建议结合其他检测手段
3. **定期更新**：建议设置定时任务，保持黑名单最新
4. **备份数据**：定期备份黑名单文件，防止数据丢失

## 定时任务配置（Linux）

使用 crontab 设置每小时运行一次：

```bash
# 编辑 crontab
crontab -e

# 添加以下行（每小时运行一次）
0 * * * * cd /path/to/your/project && python botscout_ip_crawler.py --run-once
```

## 许可证

本脚本仅供学习和研究使用，请遵守相关法律法规。
