# 智能文献检索系统 — 全量代码文档

> 基于 Elasticsearch + Spring Boot 3 + Vue 3 的智能文献检索平台

---

## 目录

- [项目配置](#项目配置)
- [后端代码](#后端代码)
  - [启动入口](#启动入口)
  - [公共类 common](#公共类-common)
  - [实体类 entity](#实体类-entity)
  - [配置类 config](#配置类-config)
  - [工具类 util](#工具类-util)
  - [ES 文档层 es](#es-文档层-es)
  - [DTO 数据传输对象](#dto-数据传输对象)
  - [VO 视图对象](#vo-视图对象)
  - [Mapper 数据访问层](#mapper-数据访问层)
  - [Service 接口层](#service-接口层)
  - [Service 实现层](#service-实现层)
  - [Controller 控制层](#controller-控制层)
- [前端代码](#前端代码)
  - [入口与路由](#入口与路由)
  - [API 封装](#api-封装)
  - [页面组件](#页面组件)

---

## 项目配置

### pom.xml（依赖）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
  </parent>

  <groupId>com.example</groupId>
  <artifactId>literature-search-system</artifactId>
  <version>1.0.0</version>
  <description>基于Elasticsearch与SpringBoot的智能文献系统</description>

  <properties>
    <java.version>17</java.version>
  </properties>

  <dependencies>
    <!-- Spring Boot Web -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- Spring Boot Data Elasticsearch -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
    </dependency>
    <!-- Spring Boot Validation -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <!-- MySQL 驱动 -->
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <version>8.4.0</version>
    </dependency>
    <!-- MyBatis-Plus Spring Boot 3 专用 -->
    <dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
      <version>3.5.8</version>
    </dependency>
    <!-- Hutool -->
    <dependency>
      <groupId>cn.hutool</groupId>
      <artifactId>hutool-all</artifactId>
      <version>5.8.30</version>
    </dependency>
    <!-- Lombok -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
    <!-- PDFBox -->
    <dependency>
      <groupId>org.apache.pdfbox</groupId>
      <artifactId>pdfbox</artifactId>
      <version>2.0.33</version>
    </dependency>
    <!-- Apache Tika（已引入但未使用） -->
    <dependency>
      <groupId>org.apache.tika</groupId>
      <artifactId>tika-core</artifactId>
      <version>2.9.2</version>
    </dependency>
    <!-- JWT -->
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>0.11.5</version>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>0.11.5</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>0.11.5</version>
      <scope>runtime</scope>
    </dependency>
    <!-- Spring Boot Test -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/literature_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

  elasticsearch:
    uris: http://localhost:9300

  jackson:
    deserialization:
      fail-on-unknown-properties: false

  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

server:
  port: 8080

logging:
  level:
    com.example: debug

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

file:
  upload:
    path: E:/Java project/literature-search-system/uploads/
    max-size: 10485760
```

### vue.config.js

```js
module.exports = {
  devServer: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

---

## 后端代码

### 启动入口

#### LiteratureSearchSystemApplication.java

```java
package com.example.literaturesearchsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LiteratureSearchSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(LiteratureSearchSystemApplication.class, args);
        // syncAllToEs() 已在 SearchServiceImpl 的 @PostConstruct 中自动执行
    }
}
```

---

### 公共类 common

#### Result.java — 统一响应封装

```java
package com.example.literaturesearchsystem.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;
    private Long total;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    private Result(Integer code, String message, T data, Long total) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.total = total;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> successWithMsg(String message, T data) {
        return new Result<>(200, message, data);
    }

    public static <T> Result<T> successWithPage(T data, Long total) {
        return new Result<>(200, "success", data, total);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> unauthorized() {
        return new Result<>(401, "请先登录", null);
    }

    public static <T> Result<T> forbidden() {
        return new Result<>(403, "权限不足", null);
    }
}
```

#### LiteratureStatusEnum.java — 文献状态枚举

```java
package com.example.literaturesearchsystem.common;

import lombok.Getter;

@Getter
public enum LiteratureStatusEnum {

    PENDING(0, "待审核", "warning"),
    APPROVED(1, "已通过", "success"),
    REJECTED(2, "已驳回", "danger");

    private final Integer code;
    private final String desc;
    private final String badgeClass;

    LiteratureStatusEnum(Integer code, String desc, String badgeClass) {
        this.code = code;
        this.desc = desc;
        this.badgeClass = badgeClass;
    }

    public static String getDescByCode(Integer code) {
        for (LiteratureStatusEnum status : values()) {
            if (status.code.equals(code)) return status.desc;
        }
        return "未知";
    }

    public static String getBadgeClass(Integer code) {
        for (LiteratureStatusEnum status : values()) {
            if (status.code.equals(code)) return status.badgeClass;
        }
        return "info";
    }
}
```

#### CorrectionStatusEnum.java — 修正状态枚举

```java
package com.example.literaturesearchsystem.common;

import lombok.Getter;

@Getter
public enum CorrectionStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回");

    private final Integer code;
    private final String desc;

    CorrectionStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        for (CorrectionStatusEnum status : values()) {
            if (status.code.equals(code)) return status.desc;
        }
        return "未知";
    }
}
```

---

### 实体类 entity

#### User.java

```java
package com.example.literaturesearchsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String avatar;
    private Integer role;    // 0-普通用户 1-文献经略专员 2-管理员
    private Integer status;  // 0-正常 1-禁用
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

#### Literature.java

```java
package com.example.literaturesearchsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Literature {
    private Long id;
    private Long contributorId;
    private String title;
    private String authors;
    private String abstractText;
    private Integer publishYear;
    private String journal;
    private String keywords;
    private String doi;
    private String category;
    private String fileUrl;
    private Integer status;       // 0-待审核 1-已通过 2-已驳回
    private Integer viewCount;
    private Integer favoriteCount;
    private String reviewRemark;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### Favorite.java

```java
package com.example.literaturesearchsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long literatureId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

#### SearchHistory.java

```java
package com.example.literaturesearchsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("search_history")
public class SearchHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String keyword;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime searchTime;
}
```

#### LiteratureCorrection.java

```java
package com.example.literaturesearchsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("literature_correction")
public class LiteratureCorrection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long literatureId;
    private Long contributorId;
    private String correctionData;  // JSON 格式存储修正内容
    private Integer status;         // 0-待审核 1-已通过 2-已驳回
    private String reviewRemark;
    private Long reviewerId;
    private LocalDateTime reviewTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

---

### 配置类 config

#### WebConfig.java — CORS 与拦截器注册

```java
package com.example.literaturesearchsystem.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/login", "/api/user/register", "/uploads/**");
    }
}
```

#### LoginInterceptor.java — JWT 登录拦截器

```java
package com.example.literaturesearchsystem.config;

import com.example.literaturesearchsystem.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 放行公开接口
        if (path.contains("/user/login") || path.contains("/user/register") ||
                path.contains("/literature/search") || path.contains("/literature/list") ||
                path.contains("/search/suggest") || path.contains("/search/test") ||
                path.contains("/literature/") && !path.contains("/add") && !path.contains("/update") && !path.contains("/delete")) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return false;
        }

        return true;
    }
}
```

#### MybatisPlusConfig.java — 分页插件

```java
package com.example.literaturesearchsystem.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setOverflow(false);
        paginationInterceptor.setMaxLimit(100L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
```

#### FileUploadConfig.java — 静态资源映射

```java
package com.example.literaturesearchsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
```

#### ElasticsearchInitConfig.java — ES 索引初始化

```java
package com.example.literaturesearchsystem.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchInitConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchInitConfig.class);
    private final ElasticsearchClient client;

    public ElasticsearchInitConfig(ElasticsearchClient client) {
        this.client = client;
    }

    @PostConstruct
    public void initIndex() {
        try {
            if (client.indices().exists(e -> e.index("literature")).value()) {
                log.info("ES索引 literature 已存在，跳过创建");
                return;
            }

            CreateIndexResponse response = client.indices().create(c -> c
                    .index("literature")
                    .mappings(m -> m
                            .properties("title", p -> p.text(t -> t))
                            .properties("authors", p -> p.text(t -> t))
                            .properties("abstractText", p -> p.text(t -> t))
                            .properties("keywords", p -> p.text(t -> t))
                            .properties("suggest", p -> p.completion(cs -> cs))
                    )
            );

            log.info("ES索引创建成功: {}", response.index());
        } catch (Exception e) {
            log.error("ES索引初始化失败", e);
        }
    }
}
```

---

### 工具类 util

#### JwtUtil.java — JWT 工具

```java
package com.example.literaturesearchsystem.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET_STRING = "LiteratureSearchSystem2026SecureJwtKey!";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRE = 1000 * 60 * 60 * 24 * 7; // 7天

    public String generateToken(Long userId, String username, Integer role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(KEY)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? Long.valueOf(claims.getSubject()) : null;
    }

    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("username") : null;
    }

    public Integer getRole(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (Integer) claims.get("role") : null;
    }

    public boolean validateToken(String token) {
        Claims claims = parseToken(token);
        return claims != null && claims.getExpiration().after(new Date());
    }
}
```

#### FileUploadUtil.java — 文件上传工具

```java
package com.example.literaturesearchsystem.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class FileUploadUtil {

    @Value("${file.upload.path}")
    private String uploadPath;

    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建上传目录: {}", uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String newFileName = UUID.randomUUID().toString() + extension;
            String filePath = uploadPath + newFileName;
            file.transferTo(new File(filePath));

            log.info("文件上传成功: {}", filePath);
            return "/uploads/" + newFileName;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            String filePath = uploadPath + fileName;
            File file = new File(filePath);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) log.info("文件删除成功: {}", filePath);
                else log.warn("文件删除失败: {}", filePath);
            } else {
                log.warn("文件不存在: {}", filePath);
            }
        } catch (Exception e) {
            log.error("文件删除异常", e);
        }
    }
}
```

#### PdfParserUtil.java — PDF 解析工具（基于 PDFBox）

```java
package com.example.literaturesearchsystem.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PdfParserUtil {

    @Autowired
    private CategoryClassifier categoryClassifier;

    public Map<String, String> parsePdf(MultipartFile file) {
        Map<String, String> result = new HashMap<>();

        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(5, document.getNumberOfPages()));
            String text = stripper.getText(document);

            String cleanText = text.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("\\s+", " ");

            String title       = extractChineseTitle(cleanText);
            String author      = extractChineseAuthor(cleanText);
            String year        = extractYear(cleanText);
            String abstractText = extractChineseAbstract(cleanText);
            String doi         = extractDoi(cleanText);
            String keywords    = extractChineseKeywords(cleanText);
            String journal     = extractChineseJournal(cleanText);
            String category    = categoryClassifier.classify(title, keywords, abstractText);

            result.put("title",        title        != null ? title        : "");
            result.put("author",       author       != null ? author       : "");
            result.put("year",         year         != null ? year         : "");
            result.put("abstractText", abstractText != null ? abstractText : "");
            result.put("doi",          doi          != null ? doi          : "");
            result.put("keywords",     keywords     != null ? keywords     : "");
            result.put("journal",      journal      != null ? journal      : "");
            result.put("category",     category);

            log.info("PDF解析成功: 标题={}, 作者={}, 年份={}, 期刊={}", title, author, year, journal);

        } catch (Exception e) {
            log.error("PDF解析失败: {}", e.getMessage());
            result.put("error", e.getMessage());
        }

        return result;
    }

    private String extractChineseTitle(String text) {
        if (text == null || text.isEmpty()) return null;

        Pattern p1 = Pattern.compile("题目[：:]\\s*([^。\\n]{10,100})");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) {
            String title = m1.group(1).trim()
                    .replaceAll("DOI.*$", "").replaceAll("引用格式.*$", "")
                    .replaceAll("收稿日期.*$", "").replaceAll("网络首发日期.*$", "").trim();
            if (title.length() > 5 && title.length() < 150) return title;
        }

        Pattern p2 = Pattern.compile("([\\u4e00-\\u9fa5]{10,80}[研究分析基于方法评价模型应用设计实现]{2,5})");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) {
            String title = m2.group(1).trim();
            if (title.length() > 10 && title.length() < 150) return title;
        }

        String firstPart = text.length() > 500 ? text.substring(0, 500) : text;
        for (String sentence : firstPart.split("[。\\n]")) {
            sentence = sentence.trim();
            if (sentence.length() > 15 && sentence.length() < 150 &&
                    !sentence.matches(".*[a-zA-Z]{5,}.*") &&
                    !sentence.contains("DOI") && !sentence.contains("ISSN") &&
                    !sentence.contains("Abstract") && !sentence.contains("收稿日期")) {
                return sentence;
            }
        }
        return null;
    }

    private String extractChineseAuthor(String text) {
        if (text == null || text.isEmpty()) return null;
        Pattern p = Pattern.compile("作者[：:]\\s*([\\u4e00-\\u9fa5]{2,4}(?:[，,、][\\u4e00-\\u9fa5]{2,4}){0,5})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String author = m.group(1).trim()
                    .replaceAll("收稿日期.*$", "").replaceAll("\\d+.*$", "").trim();
            if (author.length() > 0 && author.length() < 50) return author;
        }
        return null;
    }

    private String extractYear(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher m = Pattern.compile("\\b(20[0-2][0-9]|19[0-9]{2})\\b").matcher(text);
        return m.find() ? m.group() : null;
    }

    private String extractChineseAbstract(String text) {
        if (text == null || text.isEmpty()) return null;
        Pattern p = Pattern.compile("摘要[：:]\\s*([\\u4e00-\\u9fa5\\，\\。\\；\\"\\"\\！\\？]{50,800})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String abstractText = m.group(1).trim();
            int endIndex = abstractText.indexOf("。");
            if (endIndex > 50 && endIndex < 500) abstractText = abstractText.substring(0, endIndex + 1);
            if (abstractText.length() > 500) abstractText = abstractText.substring(0, 500);
            return abstractText;
        }
        return null;
    }

    private String extractDoi(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher m1 = Pattern.compile("DOI[：:]\\s*(10\\.\\d{4,5}/[^\\s]+)").matcher(text);
        if (m1.find()) return m1.group(1).trim().replaceAll("[。，,.;:、\\[\\]\\(\\)]$", "");
        Matcher m2 = Pattern.compile("\\b(10\\.\\d{4,5}/[^\\s]{5,50})\\b").matcher(text);
        if (m2.find()) return m2.group(1).trim().replaceAll("[。，,.;:、]$", "");
        return null;
    }

    private String extractChineseKeywords(String text) {
        if (text == null || text.isEmpty()) return null;
        Pattern p1 = Pattern.compile("关键词[：:]\\s*([\\u4e00-\\u9fa5\\；\\；\\，]{10,200})");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) {
            String kw = m1.group(1).trim()
                    .replaceAll("[a-zA-Z\\s]+", "").replaceAll("DOI.*$", "").trim()
                    .replaceAll("[。，,.;:、]$", "").replaceAll("[；;]", ",");
            if (kw.length() > 0 && kw.length() < 200) return kw;
        }
        Pattern p2 = Pattern.compile("关键词[：:]\\s*([^。\\n]{10,200})");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) {
            String kw = m2.group(1).trim()
                    .replaceAll("[a-zA-Z\\s]+", "").replaceAll("DOI.*$", "").trim()
                    .replaceAll("[。，,.;:、]$", "").replaceAll("[；;]", ",");
            if (kw.length() > 0 && kw.length() < 200) return kw;
        }
        return null;
    }

    private String extractChineseJournal(String text) {
        if (text == null || text.isEmpty()) return null;
        Pattern p = Pattern.compile("([\\u4e00-\\u9fa5]{2,15}?(?:学报|杂志|期刊|研究|地理|经济|管理|理论与实践))");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String journal = m.group(1).trim();
            if (journal.length() > 2 && journal.length() < 30) return journal;
        }
        return null;
    }
}
```

#### CategoryClassifier.java — 关键词规则分类器

```java
package com.example.literaturesearchsystem.util;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class CategoryClassifier {

    private static final Map<String, String[]> CATEGORY_KEYWORDS = new HashMap<>();

    static {
        CATEGORY_KEYWORDS.put("计算机科学", new String[]{
            "计算机","算法","软件","编程","代码","数据","网络","人工智能","AI",
            "机器学习","深度学习","神经网络","大数据","云计算","物联网","区块链",
            "操作系统","数据库","前端","后端","架构","信息安全","密码学",
            "图像处理","自然语言","NLP","计算机视觉","模式识别","数据挖掘",
            "推荐系统","搜索引擎","人机交互","虚拟现实","VR","AR","增强现实"
        });
        CATEGORY_KEYWORDS.put("物理学", new String[]{
            "物理","量子","相对论","力学","光学","电磁","热力学","天体",
            "凝聚态","粒子","原子","分子","声学","核物理","等离子体",
            "固体物理","流体力学","统计物理","宇宙学","黑洞","暗物质",
            "引力波","光谱","激光","半导体","超导","磁性","纳米","光子"
        });
        CATEGORY_KEYWORDS.put("化学", new String[]{
            "化学","有机","无机","分析化学","物理化学","高分子","催化",
            "合成","反应","分子","材料化学","生物化学","药物化学","绿色化学",
            "电化学","光化学","热化学","色谱","质谱","光谱","晶体","纳米材料",
            "聚合物","表面化学","胶体","萃取","蒸馏","滴定","酸碱","氧化还原"
        });
        CATEGORY_KEYWORDS.put("生物学", new String[]{
            "生物","基因","细胞","生态","进化","遗传","分子生物","微生物",
            "植物","动物","神经科学","生物技术","生物信息","蛋白质","酶",
            "DNA","RNA","染色体","突变","转录","翻译","代谢","信号转导",
            "免疫","疫苗","抗体","抗原","生物多样性","生态系统","保育"
        });
        CATEGORY_KEYWORDS.put("数学", new String[]{
            "数学","代数","几何","拓扑","微积分","概率","统计","数值",
            "方程","函数","矩阵","优化","运筹","线性代数","高等数学",
            "离散数学","组合数学","数论","群论","环论","域论","微分方程",
            "偏微分方程","傅里叶","拉普拉斯","变分法","混沌","分形","图论","博弈论"
        });
        CATEGORY_KEYWORDS.put("医学", new String[]{
            "医学","临床","疾病","药物","治疗","手术","诊断","病理",
            "肿瘤","心血管","神经","免疫","疫苗","护理","药学","内科",
            "外科","儿科","妇产科","眼科","耳鼻喉","皮肤科","精神科",
            "放射科","影像学","超声","CT","MRI","康复","预防医学"
        });
        CATEGORY_KEYWORDS.put("经济学", new String[]{
            "经济","金融","市场","贸易","财政","GDP","货币","银行",
            "投资","股票","保险","税收","产业","劳动","发展经济",
            "高铁","开通","城乡","收入","差距","空间","溢出",
            "夜间灯光","基尼系数","双重差分","空间计量","区域经济",
            "城市发展","交通","基础设施","城镇化","宏观经济","微观经济",
            "国际贸易","汇率","通货膨胀","失业","经济增长","资源配置"
        });
        CATEGORY_KEYWORDS.put("管理学", new String[]{
            "管理","企业","战略","营销","人力","组织","领导","决策",
            "供应链","物流","项目","质量","创新","创业","财务管理",
            "会计","审计","绩效","激励","团队","沟通","冲突","变革",
            "风险管理","危机管理","知识管理","信息系统","电子商务",
            "客户关系","CRM","企业资源","ERP"
        });
        CATEGORY_KEYWORDS.put("法学", new String[]{
            "法律","法学","宪法","刑法","民法","经济法","国际法","诉讼",
            "司法","立法","法治","权利","义务","合同","侵权","物权",
            "债权","婚姻法","继承法","商法","行政法","劳动法","仲裁",
            "调解","辩护","原告","被告","证据","判决","上诉","司法解释"
        });
        CATEGORY_KEYWORDS.put("教育学", new String[]{
            "教育","教学","课程","学习","培训","教师","学生","学校",
            "高等教育","职业教育","学前教育","特殊教育","在线教育",
            "远程教育","素质教育","应试教育","义务教育","学历","学位",
            "考试","评价","德育","智育","体育","美育","劳动教育",
            "教育技术","教育心理","教育管理","教育政策"
        });
        CATEGORY_KEYWORDS.put("文学", new String[]{
            "文学","小说","诗歌","散文","戏剧","语言","语言学","翻译",
            "修辞","文艺","古典","现代文学","古代文学","外国文学",
            "比较文学","文学理论","文学批评","作家","作品","叙事",
            "象征","隐喻","风格","流派","现实主义","浪漫主义",
            "现代主义","后现代","女性文学","儿童文学"
        });
        CATEGORY_KEYWORDS.put("历史学", new String[]{
            "历史","史学","考古","文物","古籍","古代","近代","现代史",
            "文化史","社会史","经济史","政治史","军事史","外交史",
            "思想史","科技史","艺术史","文明","王朝","帝国","革命",
            "战争","条约","制度","风俗","传统","遗产","博物馆",
            "档案","文献","史料"
        });
        CATEGORY_KEYWORDS.put("哲学", new String[]{
            "哲学","伦理","逻辑","美学","认识论","形而上学","辩证法",
            "马克思主义","儒家","道家","佛学","西方哲学","中国哲学",
            "古代哲学","现代哲学","存在主义","现象学","解释学","批判理论",
            "实用主义","分析哲学","道德","正义","自由","平等","人性",
            "意识","真理","意义","价值"
        });
        CATEGORY_KEYWORDS.put("艺术学", new String[]{
            "艺术","美术","音乐","设计","影视","舞蹈","戏剧","摄影",
            "绘画","雕塑","建筑","书法","动漫","动画","游戏设计",
            "平面设计","工业设计","服装设计","环境设计","数字媒体",
            "表演","声乐","器乐","指挥","编导","电影","电视剧",
            "纪录片","艺术史","美学","艺术批评"
        });
        CATEGORY_KEYWORDS.put("心理学", new String[]{
            "心理","认知","行为","情绪","人格","社会心理","发展心理",
            "临床心理","咨询","精神","意识","潜意识","记忆","学习",
            "思维","语言","智力","动机","情感","压力","焦虑","抑郁",
            "心理治疗","心理测量","实验心理","生理心理","教育心理",
            "管理心理","消费心理","犯罪心理"
        });
        CATEGORY_KEYWORDS.put("社会学", new String[]{
            "社会","社会学","人口","人类学","民俗","社区","城市化",
            "家庭","性别","阶层","文化","变迁","现代化","全球化",
            "社会问题","贫困","不平等","流动","移民","种族","民族",
            "宗教","信仰","组织","制度","规范","角色","社会化",
            "社会资本","社会网络","社会运动"
        });
        CATEGORY_KEYWORDS.put("政治学", new String[]{
            "政治","政策","行政","国际关系","外交","治理","民主",
            "政党","选举","公共管理","地缘政治","主权","领土",
            "国家安全","国防","军事","联盟","条约","联合国","欧盟",
            "意识形态","左翼","右翼","自由主义","保守主义","社会主义",
            "资本主义","威权","极权","革命","改革"
        });
        CATEGORY_KEYWORDS.put("地理学", new String[]{
            "地理","地图","GIS","遥感","地貌","气候","水文","土壤",
            "城市规划","区域","国土","海洋","大气","植被","冰川",
            "沙漠","河流","湖泊","山脉","平原","盆地","经纬度",
            "投影","坐标系","空间分析","地理信息","卫星","无人机",
            "测绘","地质","自然灾害"
        });
        CATEGORY_KEYWORDS.put("环境科学", new String[]{
            "环境","生态","环保","污染","气候","能源","资源","可持续",
            "碳中和","绿色","循环经济","废物","水处理","大气污染",
            "水污染","土壤污染","噪声污染","固废","回收","再生",
            "可再生能源","太阳能","风能","水能","生物质","地热",
            "气候变化","全球变暖","碳排放","碳足迹","环境保护"
        });
        CATEGORY_KEYWORDS.put("其他", new String[]{
            "综合","交叉","跨学科","前沿","新兴","综述","展望"
        });
    }

    public String classify(String title, String keywords, String abstractText) {
        String text = (title != null ? title : "") + " " +
                      (keywords != null ? keywords : "") + " " +
                      (abstractText != null ? abstractText : "");
        text = text.toLowerCase();

        Map<String, Integer> scoreMap = new HashMap<>();
        for (Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : entry.getValue()) {
                if (text.contains(kw.toLowerCase())) score += 2;
            }
            if (score > 0) scoreMap.put(entry.getKey(), score);
        }

        String bestCategory = null;
        int maxScore = 0;
        for (Map.Entry<String, Integer> entry : scoreMap.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }

        return maxScore < 2 ? "其他" : bestCategory;
    }
}
```

---

### ES 文档层 es

#### LiteratureDocument.java

```java
package com.example.literaturesearchsystem.es.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

@Data
@Document(indexName = "literature")
public class LiteratureDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String authors;

    @Field(type = FieldType.Text)
    private String abstractText;

    @Field(type = FieldType.Integer)
    private Integer publishYear;

    @Field(type = FieldType.Keyword)
    private String journal;

    @Field(type = FieldType.Text)
    private String keywords;

    @Field(type = FieldType.Keyword)
    private String doi;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String fileUrl;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @CompletionField
    private Completion suggest;
}
```

#### LiteratureEsRepository.java

```java
package com.example.literaturesearchsystem.es.repository;

import com.example.literaturesearchsystem.es.document.LiteratureDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiteratureEsRepository extends ElasticsearchRepository<LiteratureDocument, Long> {
}
```

---

### DTO 数据传输对象

#### LoginDTO.java

```java
package com.example.literaturesearchsystem.dto;
import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;
}
```

#### RegisterDTO.java

```java
package com.example.literaturesearchsystem.dto;
import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String nickname;
    private String email;
}
```

#### LiteratureSearchDTO.java

```java
package com.example.literaturesearchsystem.dto;
import lombok.Data;

@Data
public class LiteratureSearchDTO {
    private String keyword;
    private String author;
    private String journal;
    private String category;
    private String keywords;
    private Integer startYear;
    private Integer endYear;
    private Integer page;
    private Integer size;
}
```

#### LiteratureUploadDTO.java

```java
package com.example.literaturesearchsystem.dto;
import lombok.Data;

@Data
public class LiteratureUploadDTO {
    private String title;
    private String authors;
    private String abstractText;
    private Integer publishYear;
    private String journal;
    private String keywords;
    private String doi;
    private String category;
    private String fileUrl;
}
```

#### LiteratureCorrectionDTO.java

```java
package com.example.literaturesearchsystem.dto;
import lombok.Data;

@Data
public class LiteratureCorrectionDTO {
    private Long literatureId;
    private String title;
    private String authors;
    private String abstractText;
    private Integer publishYear;
    private String journal;
    private String keywords;
    private String doi;
    private String category;
}
```

---

### VO 视图对象

#### LoginVO.java

```java
package com.example.literaturesearchsystem.vo;
import lombok.Data;

@Data
public class LoginVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private Integer role;
    private String token;
}
```

#### UserVO.java

```java
package com.example.literaturesearchsystem.vo;
import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private Integer role;
}
```

#### SearchResultVO.java

```java
package com.example.literaturesearchsystem.vo;
import lombok.Data;
import java.util.List;

@Data
public class SearchResultVO {
    private List<SearchHitVO> records;
    private Long total;
    private Integer page;
    private Integer size;

    @Data
    public static class SearchHitVO {
        private Long id;
        private String title;
        private String authors;
        private String abstractText;
        private Integer publishYear;
        private String journal;
        private String keywords;
        private String doi;
        private String category;
        private Integer viewCount;
        private Float score;
        private String fileUrl;
    }
}
```

#### ContributionVO.java

```java
package com.example.literaturesearchsystem.vo;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContributionVO {
    private Long id;
    private String title;
    private String authors;
    private String journal;
    private Integer publishYear;
    private Integer status;
    private String statusDesc;
    private String statusBadgeClass;
    private String reviewRemark;
    private LocalDateTime createTime;
    private LocalDateTime reviewTime;
}
```

#### CorrectionVO.java

```java
package com.example.literaturesearchsystem.vo;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CorrectionVO {
    private Long id;
    private Long literatureId;
    private String literatureTitle;
    private String correctionData;
    private Integer status;
    private String reviewRemark;
    private LocalDateTime createTime;
    private LocalDateTime reviewTime;
}
```

---

### Mapper 数据访问层

所有 Mapper 继承 `BaseMapper<T>`，自动获得 CRUD 能力。

```java
// UserMapper.java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    int countByRole(@Param("role") int role);
}

// LiteratureMapper.java
@Mapper
public interface LiteratureMapper extends BaseMapper<Literature> {
    void incrementViewCount(@Param("id") Long id);
}

// FavoriteMapper.java
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
    List<Long> selectLiteratureIdsByUserId(@Param("userId") Long userId);
}

// SearchHistoryMapper.java
@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {
    List<SearchHistory> selectByUserId(@Param("userId") Long userId);
    void deleteByUserId(@Param("userId") Long userId);
}

// LiteratureCorrectionMapper.java
@Mapper
public interface LiteratureCorrectionMapper extends BaseMapper<LiteratureCorrection> {
    List<LiteratureCorrection> selectPendingByLiteratureId(@Param("literatureId") Long literatureId);
}
```

---

### Service 接口层

```java
// UserService.java
public interface UserService {
    LoginVO login(LoginDTO loginDTO);
    LoginVO register(RegisterDTO registerDTO);
    LoginVO getCurrentUser(Long userId);
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    UserVO updateProfile(Long userId, String nickname, String email, String avatarUrl);
    UserVO getUserDetail(Long userId);
}

// LiteratureService.java
public interface LiteratureService {
    Map<String, Object> search(LiteratureSearchDTO searchDTO);
    Literature addLiterature(Literature literature, MultipartFile file);
    Literature updateLiterature(Long id, Literature literature, MultipartFile file);
    LiteratureVO getById(Long id);
    void deleteById(Long id);
    void syncToEs();
}

// SearchService.java
public interface SearchService {
    SearchResultVO search(LiteratureSearchDTO searchDTO);
    List<String> suggest(String prefix);
    void syncToEs(Long literatureId);
    void syncAllToEs();
    void deleteFromEs(Long literatureId);
    void batchDeleteFromEs(List<Long> literatureIds);
}

// FavoriteService.java
public interface FavoriteService {
    boolean addFavorite(Long userId, Long literatureId);
    boolean removeFavorite(Long userId, Long literatureId);
    boolean isFavorited(Long userId, Long literatureId);
    List<Literature> getUserFavorites(Long userId);
}

// ContributorService.java
public interface ContributorService {
    Literature uploadLiterature(LiteratureUploadDTO uploadDTO, Long userId);
    Page<ContributionVO> getMyContributions(Long userId, Integer page, Integer size, Integer status);
    Literature resubmitLiterature(Long literatureId, LiteratureUploadDTO uploadDTO, Long userId);
    Long submitCorrection(LiteratureCorrectionDTO correctionDTO, Long userId);
    Page<CorrectionVO> getMyCorrections(Long userId, Integer page, Integer size, Integer status);
    Object getContributionStats(Long userId);
}

// SearchHistoryService.java
public interface SearchHistoryService {
    void saveHistory(Long userId, String keyword);
    List<SearchHistory> getUserHistory(Long userId);
    void clearHistory(Long userId);
}
```

---

### Service 实现层

#### UserServiceImpl.java

```java
package com.example.literaturesearchsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.literaturesearchsystem.dto.LoginDTO;
import com.example.literaturesearchsystem.dto.RegisterDTO;
import com.example.literaturesearchsystem.entity.User;
import com.example.literaturesearchsystem.mapper.UserMapper;
import com.example.literaturesearchsystem.service.UserService;
import com.example.literaturesearchsystem.vo.LoginVO;
import com.example.literaturesearchsystem.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername())
                .eq(User::getDeleted, 0).eq(User::getStatus, 0);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) throw new RuntimeException("用户不存在或已被禁用");

        String encryptedPassword = DigestUtils.md5DigestAsHex(
                loginDTO.getPassword().getBytes(StandardCharsets.UTF_8));
        if (!user.getPassword().equals(encryptedPassword)) throw new RuntimeException("密码错误");

        LoginVO loginVO = new LoginVO();
        loginVO.setId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setEmail(user.getEmail());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setRole(user.getRole());
        return loginVO;
    }

    @Override
    public LoginVO register(RegisterDTO registerDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDTO.getUsername()).eq(User::getDeleted, 0);
        if (userMapper.selectOne(queryWrapper) != null) throw new RuntimeException("用户名已存在");

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(DigestUtils.md5DigestAsHex(
                registerDTO.getPassword().getBytes(StandardCharsets.UTF_8)));
        user.setNickname(registerDTO.getNickname());
        user.setEmail(registerDTO.getEmail());
        user.setRole(1);
        user.setDeleted(0);
        user.setStatus(0);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        LoginVO loginVO = new LoginVO();
        loginVO.setId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setEmail(user.getEmail());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setRole(user.getRole());
        return loginVO;
    }

    @Override
    public LoginVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 0 || user.getDeleted() != 0)
            throw new RuntimeException("用户不存在或已被禁用");
        LoginVO loginVO = new LoginVO();
        loginVO.setId(user.getId()); loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname()); loginVO.setEmail(user.getEmail());
        loginVO.setAvatar(user.getAvatar()); loginVO.setRole(user.getRole());
        return loginVO;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        String encryptedOld = DigestUtils.md5DigestAsHex(oldPassword.getBytes(StandardCharsets.UTF_8));
        if (!user.getPassword().equals(encryptedOld)) return false;
        user.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8)));
        userMapper.updateById(user);
        return true;
    }

    @Override
    public UserVO updateProfile(Long userId, String nickname, String email, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setNickname(nickname);
        if (email != null) user.setEmail(email);
        if (avatarUrl != null) user.setAvatar(avatarUrl);
        userMapper.updateById(user);

        UserVO userVO = new UserVO();
        userVO.setId(user.getId()); userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname()); userVO.setEmail(user.getEmail());
        userVO.setAvatar(user.getAvatar()); userVO.setRole(user.getRole());
        return userVO;
    }

    @Override
    public UserVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 0 || user.getDeleted() != 0)
            throw new RuntimeException("用户不存在或已被禁用");
        UserVO userVO = new UserVO();
        userVO.setId(user.getId()); userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname()); userVO.setEmail(user.getEmail());
        userVO.setAvatar(user.getAvatar()); userVO.setRole(user.getRole());
        return userVO;
    }
}
```

#### SearchServiceImpl.java（核心 ES 搜索实现）

```java
// 关键实现摘要：

@PostConstruct
public void init() {
    syncAllToEs();  // 启动时自动同步 status=1 文献到 ES
}

@Override
public SearchResultVO search(LiteratureSearchDTO searchDTO) {
    // 构建 ES Bool 查询：
    // - keyword → matchPhrase on title / keywords / abstractText (minimumShouldMatch=1)
    // - author  → matchPhrase on authors
    // - journal → matchPhrase on journal
    // - category → matchPhrase on category
    // - year    → range on publishYear
    // - 无条件时 → matchAll
    // 高亮：服务端正则将命中词替换为 <span style='color:red;font-weight:bold'>词</span>
}

@Override
public List<String> suggest(String prefix) {
    // matchPhrasePrefix + match on title/keywords
    // 提取标题（contains匹配）和关键词（按分隔符拆分后contains匹配）
    // 去重、按长度升序、最多返回 5 条
}

@Override
public void syncAllToEs() {
    // 查询所有 status=1 文献，批量保存到 ES
}

private String highlight(String text, String keyword) {
    // 正则 (?i) 忽略大小写替换
    return Pattern.compile("(?i)" + Pattern.quote(keyword))
            .matcher(text)
            .replaceAll(m -> "<span style='color:red;font-weight:bold'>" + m.group() + "</span>");
}
```

#### LiteratureServiceImpl.java（文献 CRUD）

核心逻辑：
- `addLiterature`：status=0（待审核），上传文件后保存 fileUrl，入库
- `updateLiterature`：替换附件时先删旧文件再上传，用 `LambdaUpdateWrapper` 强制全字段更新
- `deleteById`：先删关联文件，再删库记录
- `search`（MySQL 备用）：`LambdaQueryWrapper` like/eq/ge/le 多条件查询，仅查 status=1

#### FavoriteServiceImpl.java（收藏）

- `addFavorite`：先检查是否已收藏，未收藏则插入记录
- `removeFavorite`：按 userId + literatureId 删除
- `isFavorited`：selectCount > 0
- `getUserFavorites`：通过 FavoriteMapper 查出 literatureId 列表，再批量查文献

#### ContributorServiceImpl.java（专员贡献）

- `uploadLiterature`：status=PENDING，copyProperties，入库
- `resubmitLiterature`：校验 status=REJECTED + 是本人，重置 status/reviewRemark/reviewerId/reviewTime
- `submitCorrection`：校验 status=APPROVED，检查无重复待审修正，将修正字段序列化为 JSON 存入 correction_data
- `getContributionStats`：分别 count PENDING / APPROVED / REJECTED

#### SearchHistoryServiceImpl.java（搜索历史）

- `saveHistory`：先删同关键词旧记录（去重），新增，超出 20 条则删最旧的
- `getUserHistory`：按时间倒序查询
- `clearHistory`：deleteByUserId

---

### Controller 控制层

#### UserController.java（`/api/user`）

```java
POST   /register          → userService.register() + jwtUtil.generateToken()
POST   /login             → userService.login() + jwtUtil.generateToken()
GET    /current           → jwtUtil.validateToken() → userService.getCurrentUser()
GET    /detail            → userService.getUserDetail()
GET    /list              → 验证 role=2 → userMapper.selectList()（隐藏密码）
PUT    /profile           → fileUploadUtil.uploadFile(avatarFile) → userService.updateProfile()
PUT    /password          → userService.changePassword()
DELETE /delete            → userMapper.deleteById()（物理删除自己）
PUT    /{id}/role         → 验证 role=2，不能改自己，保留最后一名管理员 → userMapper.updateById()
PUT    /{id}/disable      → 验证 role=2，不能禁用自己，保留最后一名管理员 → setStatus(1)
PUT    /{id}/enable       → 验证 role=2 → setStatus(0)
DELETE /{id}/permanent    → 验证 role=2，不能删自己，保留最后一名管理员 → userMapper.deleteById()
PUT    /{id}/reset-password → 验证 role=2，不能重置自己，encryptPassword("123456") → updateById()
```

#### LiteratureController.java（`/api/literature`）

```java
POST   /search                  → searchService.search(searchDTO)（走 ES）
POST   /add                     → jwtUtil.getUserId() → setContributorId → literatureService.addLiterature()
PUT    /{id}                    → 验证 isAdmin 或 isOwner → literatureMapper.updateById() → searchService.syncToEs()
GET    /{id}                    → literatureService.getById()
DELETE /{id}                    → literatureService.deleteById() → searchService.deleteFromEs()
DELETE /batch                   → 循环 deleteById + deleteFromEs
POST   /sync                    → literatureService.syncToEs()
POST   /parse-pdf               → pdfParserUtil.parsePdf(file)
GET    /favorite/{id}/status    → favoriteService.isFavorited()
POST   /search-history/save     → searchHistoryService.saveHistory()
GET    /search-history          → searchHistoryService.getUserHistory()
DELETE /search-history          → searchHistoryService.clearHistory()
DELETE /search-history/{id}     → searchHistoryMapper.delete(条件)
GET    /pending                 → 验证 role=2 → status=0 文献列表
GET    /reviewed                → 验证 role=2 → status IN(1,2) 文献列表
GET    /my-contributions        → contributorId=userId 文献列表
PUT    /{id}/view               → literatureMapper.incrementViewCount()
PUT    /review/{id}             → 验证 role=2 → 更新 status/remark/reviewerId/reviewTime
```

#### SearchController.java（`/api/search`）

```java
GET    /test        → "搜索服务正常"
GET    /suggest     → searchService.suggest(keyword)
POST   /sync-all    → searchService.syncAllToEs()
DELETE /es/{id}     → searchService.deleteFromEs()
DELETE /es/batch    → searchService.batchDeleteFromEs()
```

#### FavoriteController.java（`/api/literature`）

```java
GET    /favorites          → favoriteService.getUserFavorites()
POST   /favorite/{id}      → favoriteService.addFavorite()
DELETE /favorite/{id}      → favoriteService.removeFavorite()
```

#### ContributorController.java（`/api/contributor`）

```java
POST   /literature                      → contributorService.uploadLiterature()
GET    /my-contributions                → contributorService.getMyContributions()（分页）
PUT    /my-contributions/{id}/resubmit  → contributorService.resubmitLiterature()
POST   /correction                      → contributorService.submitCorrection()
GET    /my-corrections                  → contributorService.getMyCorrections()（分页）
GET    /stats                           → contributorService.getContributionStats()
```

---

## 前端代码

### 入口与路由

#### main.js

```js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

#### App.vue

```vue
<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default { name: 'App' }
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: linear-gradient(135deg, #1a73e8 0%, #0d47a1 100%);
  min-height: 100vh;
}
</style>
```

#### router/index.js

```js
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import HomeView from '../views/HomeView.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Admin from '../views/Admin.vue'
import Profile from '../views/Profile.vue'
import Review from '../views/Review.vue'

const routes = [
  { path: '/',         name: 'home',     component: HomeView,  meta: { requiresAuth: true } },
  { path: '/login',    name: 'login',    component: Login },
  { path: '/register', name: 'register', component: Register },
  { path: '/admin',    name: 'admin',    component: Admin,    meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/profile',  name: 'profile',  component: Profile,  meta: { requiresAuth: true } },
  { path: '/review',   name: 'review',   component: Review,   meta: { requiresAuth: true, requiresAdmin: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }
  if (to.meta.requiresAdmin) {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      if (Number(user.role) !== 2) {
        ElMessage.error('权限不足')
        next('/')
        return
      }
    }
  }
  next()
})

export default router
```

---

### API 封装

#### api/index.js

```js
import axios from 'axios'

const request = axios.create({
    baseURL: '/api',
    timeout: 30000
})

// 请求拦截器：自动附加 token
request.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// 响应拦截器：直接返回 data
request.interceptors.response.use(
    response => response.data,
    error => Promise.reject(error)
)

export const literatureAPI = {
    search: (params) => request.post('/literature/search', params),
    add: (formData) => request.post('/literature/add', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    }),
    getDetail: (id) => request.get(`/literature/${id}`),
    delete: (id) => request.delete(`/literature/${id}`),
    update: (id, data) => request.put(`/literature/${id}`, data),
    getMyContributions: () => request.get('/literature/my-contributions'),
    getFavorites: () => request.get('/literature/favorites'),
    addFavorite: (id) => request.post(`/literature/favorite/${id}`),
    removeFavorite: (id) => request.delete(`/literature/favorite/${id}`),
    getSearchHistory: () => request.get('/literature/search-history'),
    saveSearchHistory: (keyword) => request.post('/literature/search-history/save', { keyword }),
}

export const userAPI = {
    login: (data) => request.post('/user/login', data),
    register: (data) => request.post('/user/register', data),
    getCurrent: () => request.get('/user/current'),
    getUserList: () => request.get('/user/list'),
    updateRole: (userId, role) => request.put(`/user/${userId}/role?role=${role}`),
    disableUser: (userId) => request.put(`/user/${userId}/disable`),
    enableUser: (userId) => request.put(`/user/${userId}/enable`),
    deleteUser: (userId) => request.delete(`/user/${userId}/permanent`),
    changePassword: (data) => request.put('/user/password', data),
    updateProfile: (data) => {
        const formData = new FormData()
        formData.append('nickname', data.nickname)
        if (data.email) formData.append('email', data.email)
        if (data.avatar) formData.append('avatar', data.avatar)
        return request.put('/user/profile', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },
    resetPassword: (userId) => request.put(`/user/${userId}/reset-password`),
}
```

---

### 页面组件

#### Login.vue — 登录页

核心功能：用户名密码表单，调用 `userAPI.login()`，成功后将 `token` 和 `user` 存入 `localStorage`，跳转首页。

#### Register.vue — 注册页

核心功能：用户名、密码、昵称三字段表单，调用 `userAPI.register()`，注册成功后跳转登录。

#### HomeView.vue — 主页（文献搜索）

核心功能：
- **搜索区**：关键词输入框 + 作者/期刊/分类/年份筛选 → `literatureAPI.search()`
- **添加文献表单**：标题/作者/期刊/年份/分类/关键词（分号分隔）/摘要/文件上传
- **PDF 智能解析**：上传 PDF 后调用 `/api/literature/parse-pdf` 自动填充表单字段
- **结果列表**：卡片展示，标题高亮，收藏按钮，浏览量统计
- **详情弹窗**：完整信息展示，编辑功能（管理员或贡献者）
- **搜索历史**：自动保存，可快速重搜

#### Profile.vue — 个人中心

标签页结构：
- **基本信息**：头像预览 + 更换（el-upload before-upload 本地预览），昵称/邮箱修改
- **修改密码**：旧密码验证 → 更新
- **我的贡献**（role=1 可见）：贡献列表 + 状态 + 重新提交/删除
- **我的收藏**：收藏列表 + 取消收藏
- **检索历史**：历史记录 + 快速重搜

#### Admin.vue — 用户管理（管理员）

核心功能：用户列表展示，角色修改（下拉）、禁用/启用、重置密码、永久删除。

#### Review.vue — 文献审核（管理员）

核心功能：待审核文献列表，通过按钮（status → 1）、驳回按钮（弹窗填写原因 → status → 2）。
