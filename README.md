# ycx-AntiCrawler - 分布式系统反爬虫组件

## 项目概述

**ycx-AntiCrawler** 是一款专为基于 Spring Boot 开发的分布式系统设计的开源反爬虫接口防刷组件。它通过多种规则和技术手段，有效识别和阻止恶意爬虫行为，保护系统资源和数据安全。

## 核心功能

### 1. 多维度规则防护

- **IP 规则**：基于时间窗口的请求频率限制，支持 IP 黑白名单
- **User-Agent 规则**：基于设备类型、操作系统的访问控制
- **Cookie 规则**：基于 Cookie 存在性和内容的验证
- **行为链路规则**：基于用户操作路径的合理性验证
- **浏览器指纹检测**：识别自动化浏览器和异常环境

### 2. 智能验证码系统

- **多种验证码类型**：中文、英文字母+数字、简单算术
- **多种展示形式**：静态图片和 GIF 动图
- **浏览器环境检测**：在验证码页面检测浏览器指纹
- **风险提示**：对异常浏览器环境显示"浏览器环境存在风险"提示

### 3. AI 分析能力

- **基于 MiniMax API** 的智能爬虫行为分析
- **请求模式识别**：识别异常请求模式和行为
- **拦截规则分析**：分析触发的拦截规则并提供智能建议

### 4. 灵活配置管理

- **配置管理页面**：可视化配置界面
- **全局/局部拦截模式**：支持全局拦截或指定接口拦截
- **细粒度配置**：每种规则都可单独配置和启用/禁用
- **白名单机制**：支持 IP、User-Agent 等白名单设置

## 技术架构

- **基于 Spring MVC 拦截器**：通过 HandlerInterceptor 实现请求过滤
- **责任链模式**：将不同过滤规则通过责任链组织
- **Redis 存储**：使用 Redis 存储请求计数、黑名单等数据
- **自动配置**：基于 Spring Boot 自动配置机制
- **可扩展设计**：提供抽象接口，支持自定义规则扩展

## 系统要求

- **Spring Boot**：支持 Spring Boot 1.x 和 2.x
- **Redis**：用于存储规则数据和状态
- **JDK**：Java 8 及以上

## 快速开始

### 1. 安装依赖

在 Maven 项目的 `pom.xml` 文件中添加以下依赖：

```xml
<dependency>
    <groupId>cn.ycx.project</groupId>
    <artifactId>ycx-AntiCrawler</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>
```

### 2. 基本配置

在 `application.yml` 或 `application.properties` 中添加以下配置：

```yaml
anti:
  reptile:
    manager:
      enabled: true  # 启用反爬虫插件
      global-filter-mode: false  # 全局拦截模式，false 为局部拦截
      include-urls: /api/**,/admin/**  # 需要反爬的接口路径
      admin-username: admin  # 配置管理页面用户名
      admin-password: admin123  # 配置管理页面密码
      
      # IP 规则配置
      ip-rule:
        enabled: true
        expiration-time: 5000  # 时间窗口（毫秒）
        request-max-size: 20  # 最大请求数
        ignore-ip: 127.0.0.1,192.168.*  # IP 白名单
        
      # User-Agent 规则配置
      ua-rule:
        enabled: true
        allowed-linux: false  # 是否允许 Linux 系统
        allowed-mobile: true  # 是否允许移动端
        allowed-pc: true  # 是否允许 PC 端
        
      # Cookie 规则配置
      cookie-rule:
        enabled: true
        allow-empty-cookie: false  # 是否允许空 Cookie
        
      # 行为链路规则配置
      behavior-chain-rule:
        enabled: true
        chain-rules: /api/detail=/api/list  # 链路规则
        
      # AI 配置
      ai:
        minimax:
          api-key: your-api-key  # MiniMax API 密钥
          group-id: your-group-id  # MiniMax 群组 ID
```

### 3. 启用反爬虫

有两种方式启用反爬虫保护：

#### 方式一：使用注解

在需要保护的接口上添加 `@AntiReptile` 注解：

```java
@RestController
@RequestMapping("/api")
public class ApiController {

    @AntiReptile
    @GetMapping("/data")
    public String getData() {
        return "Protected data";
    }
}
```

#### 方式二：使用配置文件

在配置文件中设置需要拦截的 URL 模式：

```yaml
anti:
  reptile:
    manager:
      include-urls: /api/**,/admin/**
```

### 4. 前端处理

前端需要处理 509 状态码的响应，弹出验证码页面：

```javascript
import axios from 'axios';

axios.interceptors.response.use(
  data => {
    return data;
  },
  error => {
    if (error.response && error.response.status === 509) {
      let html = error.response.data;
      let verifyWindow = window.open("","_blank","height=400,width=560");
      verifyWindow.document.write(html);
      verifyWindow.document.getElementById("baseUrl").value = window.location.origin;
    }
    return Promise.reject(error);
  }
);
```

## 浏览器指纹检测

系统会在验证码页面收集并验证浏览器指纹，包括：

- **基础信息**：User-Agent、语言、平台、CPU 核心数
- **屏幕信息**：屏幕尺寸、颜色深度、设备像素比
- **浏览器特性**：localStorage、sessionStorage、indexedDB 支持
- **Canvas 指纹**：Canvas 绘制特征
- **自动化特征**：webdriver 标志、Chrome 自动化标志
- **行为模式**：鼠标移动模式、窗口大小

异常的浏览器环境会被识别并拦截，显示"浏览器环境存在风险"提示。

## 配置管理

系统提供了配置管理页面，访问路径为 `/ycx-config/index.html`，使用配置的管理员账号登录后，可以：

- **查看系统状态**：当前规则触发情况
- **修改规则配置**：实时调整规则参数
- **查看拦截日志**：分析被拦截的请求
- **管理白名单**：添加或移除白名单条目

## 高级特性

### 1. 自定义规则

通过实现 `AntiReptileRule` 接口，可以添加自定义反爬虫规则：

```java
public class CustomRule extends AbstractRule {
    @Override
    public boolean doFilter(HttpServletRequest request) {
        // 自定义规则逻辑
        return true; // true 表示通过，false 表示拦截
    }
}
```

### 2. 拦截策略

每种规则都支持两种拦截策略：
- **verify**：显示验证码，验证通过后可继续访问
- **deny**：直接拒绝访问，返回 403 状态码

### 3. 惩罚机制

触发规则后，系统会对违规 IP 或会话进行惩罚，惩罚时长可配置。

## 部署注意事项

1. **Redis 配置**：确保 Redis 连接正常，用于存储规则数据
2. **性能优化**：对于高流量系统，建议调整时间窗口和请求阈值
3. **白名单设置**：合理设置白名单，避免误拦截正常请求
4. **AI 配置**：如需使用 AI 分析功能，需配置 MiniMax API 密钥

## 监控与日志

系统会记录以下信息：
- **拦截日志**：被拦截的请求详情
- **规则触发**：各规则的触发次数
- **验证结果**：验证码验证结果
- **AI 分析**：AI 分析的结果和建议

## 示例项目

项目中包含一个 demo 模块，展示了如何集成和使用 ycx-AntiCrawler：

1. **启动 demo 项目**：运行 `DemoApplication` 类
2. **访问测试接口**：`http://localhost:8080/api/data`
3. **触发验证码**：快速刷新页面多次
4. **测试浏览器指纹**：使用自动化工具访问

## 常见问题

### Q: 验证码一直无法通过怎么办？
A: 可能是浏览器环境被检测为异常，尝试使用正常浏览器访问，或检查是否有自动化工具特征。

### Q: 如何排除某些接口不被拦截？
A: 在全局拦截模式下，使用 `exclude-urls` 配置排除不需要拦截的接口。

### Q: 性能影响如何？
A: 系统采用高效的 Redis 操作和责任链设计，对正常请求的性能影响很小。

### Q: 如何自定义验证码样式？
A: 可以修改 `verify/index.html` 文件自定义验证码页面样式。


## 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 贡献指南

欢迎提交 Issue 和 Pull Request，共同改进项目。提交代码前请确保：

1. 遵循项目代码风格
2. 编写测试用例
3. 更新文档

---

**ycx-AntiCrawler** - 让爬虫无所遁形，保护您的系统安全！