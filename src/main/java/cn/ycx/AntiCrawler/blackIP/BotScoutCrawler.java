package cn.ycx.AntiCrawler.blackIP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BotScout IP 黑名单爬虫 (Java版本)
 * 定时爬取 https://botscout.com/last_caught_cache.htm 上的IP信息
 */
public class BotScoutCrawler {

    private static final String URL = "https://botscout.com/last_caught_cache.htm";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    
    private final String outputFile;
    private final ObjectMapper objectMapper;

    public BotScoutCrawler(String outputFile) {
        this.outputFile = outputFile;
        this.objectMapper = new ObjectMapper();
    }

    public BotScoutCrawler() {
        // 默认为当前目录下的botscout_blacklist.json
        String defaultPath = System.getProperty("user.dir") + "/src/main/java/cn/ycx/AntiCrawler/blackIP/botscout_blacklist.json";
        this.outputFile = defaultPath;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取网页数据
     */
    public String fetchData() {
        System.out.println("[" + LocalDateTime.now() + "] 正在获取数据...");
        
        try {
            URL url = new URL(URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                        content.append(System.lineSeparator());
                    }
                }
                connection.disconnect();
                System.out.println("[" + LocalDateTime.now() + "] 数据获取成功");
                return content.toString();
            } else {
                System.err.println("[" + LocalDateTime.now() + "] 获取数据失败，状态码: " + responseCode);
                connection.disconnect();
                return null;
            }
        } catch (Exception e) {
            System.err.println("[" + LocalDateTime.now() + "] 获取数据失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解析IP地址
     */
    public Set<String> parseIps(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> allIps = new HashSet<>();

        // 使用正则表达式匹配IPv4地址
        String ipv4Pattern = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
        Pattern ipv4Regex = Pattern.compile(ipv4Pattern);
        Matcher ipv4Matcher = ipv4Regex.matcher(htmlContent);

        while (ipv4Matcher.find()) {
            String ip = ipv4Matcher.group();
            if (isValidPublicIp(ip)) {
                allIps.add(ip);
            }
        }

        // 使用正则表达式匹配IPv6地址（简化版）
        String ipv6Pattern = "\\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\b|\\b(?:[0-9a-fA-F]{1,4}:){1,7}:\\b|\\b(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}\\b";
        Pattern ipv6Regex = Pattern.compile(ipv6Pattern);
        Matcher ipv6Matcher = ipv6Regex.matcher(htmlContent);

        while (ipv6Matcher.find()) {
            String ip = ipv6Matcher.group();
            allIps.add(ip);
        }

        System.out.println("[" + LocalDateTime.now() + "] 解析到 " + allIps.size() + " 个有效IP地址");
        return allIps;
    }

    /**
     * 检查是否为有效的公网IP
     */
    private boolean isValidPublicIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // 检查是否是IPv4
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            try {
                int firstOctet = Integer.parseInt(parts[0]);
                int secondOctet = Integer.parseInt(parts[1]);

                // 过滤私有IP段
                // 10.0.0.0/8
                if (firstOctet == 10) {
                    return false;
                }
                // 172.16.0.0/12
                if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
                    return false;
                }
                // 192.168.0.0/16
                if (firstOctet == 192 && secondOctet == 168) {
                    return false;
                }
                // 127.0.0.0/8 (回环地址)
                if (firstOctet == 127) {
                    return false;
                }
                // 169.254.0.0/16 (链路本地地址)
                if (firstOctet == 169 && secondOctet == 254) {
                    return false;
                }
                // 224.0.0.0/4 (多播地址)
                if (firstOctet >= 224 && firstOctet <= 239) {
                    return false;
                }
                // 240.0.0.0/4 (保留地址)
                if (firstOctet >= 240 && firstOctet <= 255) {
                    return false;
                }

                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // IPv6 暂时全部保留
        return true;
    }

    /**
     * 保存IP列表到JSON文件
     */
    public void saveToJson(Set<String> ips) {
        if (ips == null || ips.isEmpty()) {
            System.out.println("[" + LocalDateTime.now() + "] 没有IP需要保存");
            return;
        }

        try {
            Path filePath = Paths.get(outputFile);
            Set<String> existingIps = new HashSet<>();

            // 如果文件存在，先读取旧数据
            if (Files.exists(filePath)) {
                JsonNode root = objectMapper.readTree(filePath.toFile());
                JsonNode ipsNode = root.get("ips");
                if (ipsNode != null && ipsNode.isArray()) {
                    for (JsonNode ipNode : ipsNode) {
                        existingIps.add(ipNode.asText().trim());
                    }
                }
                System.out.println("[" + LocalDateTime.now() + "] 已存在 " + existingIps.size() + " 个IP");
            }

            // 合并新旧数据
            Set<String> allIps = new HashSet<>(existingIps);
            allIps.addAll(ips);
            int newCount = allIps.size() - existingIps.size();

            // 创建JSON对象
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("update_time", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            rootNode.put("source", URL);
            rootNode.put("total_count", allIps.size());
            rootNode.put("new_count", newCount);

            // 添加IP数组
            ArrayNode ipsArray = rootNode.putArray("ips");
            for (String ip : allIps) {
                ipsArray.add(ip);
            }

            // 保存到文件
            Files.createDirectories(filePath.getParent());
            objectMapper.writeValue(filePath.toFile(), rootNode);

            System.out.println("[" + LocalDateTime.now() + "] 保存成功: " + allIps.size() + " 个IP (新增 " + newCount + " 个)");

        } catch (Exception e) {
            System.err.println("[" + LocalDateTime.now() + "] 保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 保存IP列表到纯文本文件
     */
    public void saveToTxt(Set<String> ips) {
        if (ips == null || ips.isEmpty()) {
            return;
        }

        String txtFile = outputFile.replace(".json", ".txt");

        try {
            Path filePath = Paths.get(txtFile);
            Set<String> existingIps = new HashSet<>();

            // 读取已存在的IP
            if (Files.exists(filePath)) {
                try (BufferedReader lines = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = lines.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            existingIps.add(trimmed);
                        }
                    }
                }
            }

            // 合并新旧数据
            Set<String> allIps = new HashSet<>(existingIps);
            allIps.addAll(ips);

            // 保存到文件
            Files.createDirectories(filePath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                for (String ip : allIps) {
                    writer.write(ip);
                    writer.newLine();
                }
            }

            System.out.println("[" + LocalDateTime.now() + "] TXT文件保存成功: " + allIps.size() + " 个IP");

        } catch (Exception e) {
            System.err.println("[" + LocalDateTime.now() + "] TXT文件保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 运行爬虫
     */
    public void run() {
        System.out.println("[" + LocalDateTime.now() + "] 开始爬取 BotScout IP 黑名单...");

        // 获取数据
        String htmlContent = fetchData();
        if (htmlContent == null) {
            return;
        }

        // 解析IP
        Set<String> ips = parseIps(htmlContent);
        if (ips.isEmpty()) {
            System.out.println("[" + LocalDateTime.now() + "] 未解析到IP地址");
            return;
        }

        // 保存数据
        saveToJson(ips);
        saveToTxt(ips);

        System.out.println("[" + LocalDateTime.now() + "] 爬取完成");
    }

    /**
     * 获取统计信息
     */
    public BlacklistStatistics getStatistics() {
        try {
            Path filePath = Paths.get(outputFile);
            if (Files.exists(filePath)) {
                JsonNode root = objectMapper.readTree(filePath.toFile());
                int totalIps = root.path("total_count").asInt(0);
                String lastUpdate = root.path("update_time").asText("未知");
                String source = root.path("source").asText(URL);

                return new BlacklistStatistics(totalIps, lastUpdate, source);
            }
        } catch (Exception e) {
            System.err.println("获取统计信息失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 黑名单统计信息
     */
    public static class BlacklistStatistics {
        private final int totalIps;
        private final String lastUpdate;
        private final String source;

        public BlacklistStatistics(int totalIps, String lastUpdate, String source) {
            this.totalIps = totalIps;
            this.lastUpdate = lastUpdate;
            this.source = source;
        }

        public int getTotalIps() {
            return totalIps;
        }

        public String getLastUpdate() {
            return lastUpdate;
        }

        public String getSource() {
            return source;
        }
    }
}
