# 智能文献检索系统 — 代码文档

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [数据库设计](#数据库设计)
- [后端模块说明](#后端模块说明)
  - [Entity 实体层](#entity-实体层)
  - [DTO / VO 数据传输层](#dto--vo-数据传输层)
  - [Mapper 数据访问层](#mapper-数据访问层)
  - [Service 业务层](#service-业务层)
  - [Controller 接口层](#controller-接口层)
  - [ES 搜索层](#es-搜索层)
  - [工具类](#工具类)
  - [配置类](#配置类)
- [前端模块说明](#前端模块说明)
  - [路由](#路由)
  - [API 封装](#api-封装)
  - [页面说明](#页面说明)
- [接口汇总](#接口汇总)
- [关键流程说明](#关键流程说明)

---

## 项目概述

本系统是一个基于 Elasticsearch 的智能文献检索平台，支持全文搜索、PDF 智能解析、用户权限分级、文献审核工作流和收藏管理等功能。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3.5 |
| 持久层 | MyBatis-Plus 3.5.8 |
| 搜索引擎 | Elasticsearch 8.13.4（Spring Data ES 5.3.5） |
| 数据库 | MySQL 8.x |
| 认证 | JWT（jjwt 0.11.5） |
| PDF 解析 | PDFBox 2.0.33 |
| 工具库 | Hutool 5.8.30、Lombok |
| 前端框架 | Vue 3 + Vue CLI |
| UI 组件库 | Element Plus |
| HTTP 客户端 | Axios |
| 语言版本 | Java 17 |

---

## 项目结构

```
literature-search-system/
├── src/main/java/com/example/literaturesearchsystem/
│   ├── LiteratureSearchSystemApplication.java   # 启动入口
│   ├── common/
│   │   ├── Result.java                          # 统一响应封装
│   │   ├── LiteratureStatusEnum.java            # 文献状态枚举
│   │   └── CorrectionStatusEnum.java            # 修正状态枚举
│   ├── config/
│   │   ├── WebConfig.java                       # CORS + 拦截器注册
│   │   ├── LoginInterceptor.java                # JWT 登录拦截器
│   │   ├── MybatisPlusConfig.java               # MyBatis-Plus 配置
│   │   ├── FileUploadConfig.java                # 静态文件映射
│   │   └── ElasticsearchInitConfig.java         # ES 索引初始化
│   ├── controller/
│   │   ├── UserController.java
│   │   ├── LiteratureController.java
│   │   ├── SearchController.java
│   │   ├── FavoriteController.java
│   │   └── ContributorController.java
│   ├── service/
│   │   ├── UserService.java / impl/UserServiceImpl.java
│   │   ├── LiteratureService.java / impl/LiteratureServiceImpl.java
│   │   ├── SearchService.java / impl/SearchServiceImpl.java
│   │   ├── FavoriteService.java / impl/FavoriteServiceImpl.java
│   │   ├── ContributorService.java / impl/ContributorServiceImpl.java
│   │   └── SearchHistoryService.java / impl/SearchHistoryServiceImpl.java
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   ├── LiteratureMapper.java
│   │   ├── FavoriteMapper.java
│   │   ├── SearchHistoryMapper.java
│   │   └── LiteratureCorrectionMapper.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Literature.java
│   │   ├── Favorite.java
│   │   ├── SearchHistory.java
│   │   └── LiteratureCorrection.java
│   ├── dto/
│   │   ├── LoginDTO.java
│   │   ├── RegisterDTO.java
│   │   ├── LiteratureSearchDTO.java
│   │   ├── LiteratureUploadDTO.java
│   │   ├── LiteratureCorrectionDTO.java
│   │   └── LiteratureVO.java
│   ├── vo/
│   │   ├── LoginVO.java
│   │   ├── UserVO.java
│   │   ├── SearchResultVO.java
│   │   ├── ContributionVO.java
│   │   └── CorrectionVO.java
│   ├── es/
│   │   ├── document/
│   │   │   ├── LiteratureDocument.java          # ES 文档映射
│   │   │   └── Completion.java                  # 自动补全字段
│   │   └── repository/
│   │       └── LiteratureEsRepository.java      # ES Repository
│   └── util/
│       ├── JwtUtil.java                         # JWT 工具
│       ├── FileUploadUtil.java                  # 文件上传工具
│       ├── PdfParserUtil.java                   # PDF 解析工具
│       └── CategoryClassifier.java             # 文献分类器
├── src/main/resources/
│   └── application.yml
├── uploads/                                     # 上传文件存储目录
└── literature-search-frontend/                  # 前端项目
    ├── vue.config.js
    └── src/
        ├── main.js
        ├── App.vue
        ├── router/index.js
        ├── api/index.js
        └── views/
            ├── HomeView.vue
            ├── Login.vue
            ├── Register.vue
            ├── Profile.vue
            ├── Admin.vue
            └── Review.vue
```

---

## 数据库设计

数据库名：`literature_db`

### user 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR | 用户名，唯一 |
| password | VARCHAR | MD5 加密密码 |
| nickname | VARCHAR | 昵称 |
| email | VARCHAR | 邮箱 |
| avatar | VARCHAR | 头像文件路径（如 `/uploads/xxx.jpg`） |
| role | INT | 角色：0普通用户 / 1文献经略专员 / 2管理员 |
| status | INT | 状态：0正常 / 1禁用 |
| deleted | INT | 逻辑删除：0未删 / 1已删 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### literature 文献表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| contributor_id | BIGINT | 上传者 ID |
| title | VARCHAR | 标题 |
| authors | VARCHAR | 作者，多人用逗号分隔 |
| abstract_text | TEXT | 摘要 |
| publish_year | INT | 发表年份 |
| journal | VARCHAR | 期刊名称 |
| keywords | VARCHAR | 关键词，多个用分号分隔 |
| doi | VARCHAR | DOI 编号 |
| category | VARCHAR | 分类 |
| file_url | VARCHAR | 附件访问路径 |
| status | INT | 状态：0待审核 / 1已通过 / 2已驳回 |
| view_count | INT | 浏览量 |
| favorite_count | INT | 收藏数 |
| review_remark | VARCHAR | 审核备注 |
| reviewer_id | BIGINT | 审核人 ID |
| review_time | DATETIME | 审核时间 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### favorite 收藏表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| literature_id | BIGINT | 文献 ID |
| create_time | DATETIME | 收藏时间 |

### search_history 搜索历史表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| keyword | VARCHAR | 搜索关键词 |
| search_time | DATETIME | 搜索时间 |

### literature_correction 修正建议表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| literature_id | BIGINT | 目标文献 ID |
| contributor_id | BIGINT | 提交人 ID |
| correction_data | TEXT | 修正内容（JSON 格式） |
| status | INT | 状态：0待审 / 1通过 / 2驳回 |
| review_remark | VARCHAR | 审核备注 |
| reviewer_id | BIGINT | 审核人 ID |
| review_time | DATETIME | 审核时间 |
| create_time | DATETIME | 创建时间 |

---

## 后端模块说明

### Entity 实体层

**User.java**
- 使用 `@TableLogic` 软删除，`deleted` 字段默认值 0
- 使用 `@TableField(fill = FieldFill.INSERT)` 自动填充 `createTime`
- 密码字段 `@TableField(select = false)` 查询时默认不返回

**Literature.java**
- `status` 字段对应 `LiteratureStatusEnum`：PENDING(0) / APPROVED(1) / REJECTED(2)
- `viewCount` 和 `favoriteCount` 由系统自动维护，不由用户直接写入

**LiteratureCorrection.java**
- `correctionData` 存储 JSON 字符串，记录修改哪些字段及新值
- `status` 对应 `CorrectionStatusEnum`：PENDING(0) / APPROVED(1) / REJECTED(2)

---

### DTO / VO 数据传输层

| 类名 | 方向 | 说明 |
|------|------|------|
| `LoginDTO` | 入参 | username + password |
| `RegisterDTO` | 入参 | username + password + nickname |
| `LiteratureSearchDTO` | 入参 | keyword + author + journal + category + startYear + endYear + page + size |
| `LiteratureUploadDTO` | 入参 | 文献各字段（不含文件） |
| `LiteratureCorrectionDTO` | 入参 | literatureId + correctionData |
| `LiteratureVO`（dto包）| 出参 | 文献展示字段 |
| `LoginVO` | 出参 | token + 用户基本信息 |
| `UserVO` | 出参 | id + username + nickname + email + avatar + role |
| `SearchResultVO` | 出参 | total + list |
| `ContributionVO` | 出参 | 文献 + 审核状态信息 |
| `CorrectionVO` | 出参 | 修正记录 + 文献标题 |

**统一响应格式 `Result<T>`：**
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

---

### Mapper 数据访问层

所有 Mapper 继承 `BaseMapper<T>`，由 MyBatis-Plus 提供 CRUD 能力，无需手写 XML。

| Mapper | 额外方法 |
|--------|----------|
| `UserMapper` | 无自定义方法 |
| `LiteratureMapper` | 无自定义方法 |
| `FavoriteMapper` | 无自定义方法 |
| `SearchHistoryMapper` | 无自定义方法 |
| `LiteratureCorrectionMapper` | 无自定义方法 |

---

### Service 业务层

#### UserService / UserServiceImpl

| 方法 | 说明 |
|------|------|
| `login(LoginDTO)` | 验证用户名密码（MD5 对比），返回 JWT token 和用户信息 |
| `register(RegisterDTO)` | 检查用户名唯一性，MD5 加密密码后入库 |
| `getCurrentUser(Long userId)` | 按 ID 查用户，返回 UserVO |
| `changePassword(Long userId, String oldPwd, String newPwd)` | 验证旧密码后更新新密码 |
| `updateProfile(Long userId, String nickname, String email, String avatarUrl)` | 更新昵称、邮箱、头像路径 |
| `getUserDetail(Long userId)` | 返回完整用户详情 |

> 密码使用 `MD5` 加密（Hutool `SecureUtil.md5()`），**无盐**。

#### LiteratureService / LiteratureServiceImpl

| 方法 | 说明 |
|------|------|
| `addLiterature(LiteratureUploadDTO, MultipartFile, Long userId)` | 保存文献到 MySQL，上传文件，同步到 ES |
| `updateLiterature(Long id, LiteratureUploadDTO, MultipartFile)` | 更新文献，可替换附件，同步更新 ES |
| `getById(Long id)` | 查询文献详情 |
| `deleteById(Long id)` | 删除文献，同步删除文件和 ES 文档 |
| `syncToEs(Long id)` | 将单条文献同步到 ES |

#### SearchService / SearchServiceImpl

| 方法 | 说明 |
|------|------|
| `search(LiteratureSearchDTO)` | ES 全文检索，支持多字段过滤，返回高亮结果 |
| `suggest(String keyword)` | 基于 ES 模糊匹配返回最多 5 条补全建议 |
| `syncToEs(Literature)` | 同步单条文献到 ES |
| `syncAllToEs()` | 同步全部 status=1 的文献到 ES（启动时执行） |
| `deleteFromEs(Long id)` | 从 ES 删除单条文档 |

**搜索实现细节：**
- 关键词字段：`title`、`authors`、`abstractText`、`keywords`（matchPhrase）
- 过滤字段：`journal`（term）、`category`（term）、`publishYear`（range）
- 高亮：服务端用正则将匹配词替换为 `<span style='color:red'>词</span>`
- 启动自动同步：`@PostConstruct` 调用 `syncAllToEs()`

#### FavoriteService / FavoriteServiceImpl

| 方法 | 说明 |
|------|------|
| `addFavorite(Long userId, Long literatureId)` | 添加收藏，同步更新文献 favoriteCount +1 |
| `removeFavorite(Long userId, Long literatureId)` | 取消收藏，同步更新 favoriteCount -1 |
| `isFavorited(Long userId, Long literatureId)` | 查询是否已收藏 |
| `getUserFavorites(Long userId)` | 返回用户全部收藏列表 |

#### ContributorService / ContributorServiceImpl

| 方法 | 说明 |
|------|------|
| `uploadLiterature(...)` | 上传文献（status=0 待审核），仅 role=1 专员可调用 |
| `getMyContributions(Long userId, int page, int size)` | 分页获取自己上传的文献 |
| `resubmitLiterature(Long id, Long userId, LiteratureUploadDTO)` | 重新提交被驳回（status=2）的文献，重置 status=0 |
| `submitCorrection(LiteratureCorrectionDTO, Long userId)` | 提交对已发布文献的修正建议 |
| `getMyCorrections(Long userId, int page, int size)` | 分页获取自己的修正记录 |
| `getContributionStats(Long userId)` | 返回待审核/已通过/已驳回数量统计 |

#### SearchHistoryService / SearchHistoryServiceImpl

| 方法 | 说明 |
|------|------|
| `saveHistory(Long userId, String keyword)` | 保存搜索关键词，去重，保留最近 20 条 |
| `getUserHistory(Long userId)` | 查询用户搜索历史（按时间倒序） |
| `clearHistory(Long userId)` | 清空搜索历史 |

---

### Controller 接口层

#### UserController — `/api/user`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/register` | 否 | 注册，返回 token |
| POST | `/login` | 否 | 登录，返回 token + 用户信息 |
| GET | `/current` | 是 | 获取当前用户基本信息 |
| GET | `/detail` | 是 | 获取当前用户完整信息 |
| GET | `/list` | 是（管理员） | 获取所有用户列表 |
| PUT | `/profile` | 是 | 更新昵称、邮箱、头像（multipart） |
| PUT | `/password` | 是 | 修改密码 |
| DELETE | `/delete` | 是 | 注销自己账号 |
| PUT | `/{id}/role` | 是（管理员） | 修改用户角色 |
| PUT | `/{id}/disable` | 是（管理员） | 禁用用户 |
| PUT | `/{id}/enable` | 是（管理员） | 启用用户 |
| DELETE | `/{id}/permanent` | 是（管理员） | 永久删除用户 |
| PUT | `/{id}/reset-password` | 是（管理员） | 重置密码为 `123456` |

#### LiteratureController — `/api/literature`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/search` | 是 | 全文检索（走 ES） |
| POST | `/add` | 是 | 新增文献（multipart，含附件） |
| PUT | `/{id}` | 是 | 更新文献 |
| GET | `/{id}` | 是 | 获取文献详情 |
| DELETE | `/{id}` | 是 | 删除文献 |
| DELETE | `/batch` | 是（管理员） | 批量删除 |
| POST | `/sync` | 是（管理员） | 全量同步到 ES |
| POST | `/parse-pdf` | 是 | 上传 PDF，解析返回元数据 |
| GET | `/favorite/{id}/status` | 是 | 检查是否已收藏该文献 |
| GET | `/favorites` | 是 | 获取收藏列表 |
| POST | `/favorite/{id}` | 是 | 添加收藏 |
| DELETE | `/favorite/{id}` | 是 | 取消收藏 |
| GET | `/search-history` | 是 | 获取搜索历史 |
| POST | `/search-history/save` | 是 | 保存搜索历史 |
| DELETE | `/search-history` | 是 | 清空搜索历史 |
| DELETE | `/search-history/{id}` | 是 | 删除单条历史 |
| GET | `/pending` | 是（管理员） | 获取待审核文献列表 |
| GET | `/reviewed` | 是（管理员） | 获取已审核文献列表 |
| GET | `/my-contributions` | 是 | 获取我的上传记录 |
| PUT | `/{id}/view` | 是 | 文献浏览量 +1 |
| PUT | `/review/{id}` | 是（管理员） | 审核文献（通过/驳回） |

#### SearchController — `/api/search`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/test` | 否 | 服务连通性测试 |
| GET | `/suggest` | 是 | 搜索关键词自动补全 |
| POST | `/sync-all` | 是（管理员） | 全量同步到 ES |
| DELETE | `/es/{id}` | 是（管理员） | 从 ES 删除单条 |
| DELETE | `/es/batch` | 是（管理员） | 从 ES 批量删除 |

#### ContributorController — `/api/contributor`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/literature` | 是（专员） | 上传文献 |
| GET | `/my-contributions` | 是 | 我的贡献列表（分页） |
| PUT | `/my-contributions/{id}/resubmit` | 是 | 重新提交被驳回文献 |
| POST | `/correction` | 是 | 提交修正建议 |
| GET | `/my-corrections` | 是 | 我的修正记录（分页） |
| GET | `/stats` | 是 | 贡献统计数据 |

---

### ES 搜索层

#### LiteratureDocument.java

```java
@Document(indexName = "literature")
public class LiteratureDocument {
    @Id
    private String id;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String authors;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String abstractText;
    @Field(type = FieldType.Integer)
    private Integer publishYear;
    @Field(type = FieldType.Keyword)
    private String journal;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
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

> ES 地址配置为 `http://localhost:9300`（注意：ES 默认 HTTP 端口是 9200，9300 是节点通信端口，需确认实际配置）

---

### 工具类

#### JwtUtil.java

- 生成 Token：`createToken(Long userId)`，有效期 7 天
- 解析 Token：`getUserId(String token)`，返回用户 ID
- 验证 Token：`validateToken(String token)`

#### FileUploadUtil.java

- 上传路径：`${file.upload.path}`（配置值：`E:/Java project/literature-search-system/uploads/`）
- 文件名策略：UUID + 原始扩展名
- 返回值：访问路径 `/uploads/{uuid}.{ext}`
- 支持删除：`deleteFile(String fileUrl)`

#### PdfParserUtil.java

**使用库：PDFBox 2.0.33（仅 PDFBox，Tika 未实际使用）**

- 读取 PDF 前 5 页文本内容
- 通过正则表达式逐字段提取：

| 提取字段 | 策略 |
|----------|------|
| 标题 | 匹配"题目："标记 → 第一段长中文句 |
| 作者 | 匹配"作者："后的中文姓名序列 |
| 年份 | 正则匹配 2000-2029 年份数字 |
| 摘要 | 匹配"摘要："后中文内容，截取至第一个句号 |
| DOI | 匹配 `10.xxxx/` 格式 |
| 关键词 | 匹配"关键词："后内容，过滤英文 |
| 期刊 | 匹配含"学报/杂志/期刊"等后缀的中文词 |
| 分类 | 调用 `CategoryClassifier.classify()` 自动分类 |

#### CategoryClassifier.java

基于关键词规则的分类器，根据标题、关键词、摘要中的特征词匹配以下分类：

`政治学` / `经济学` / `历史学` / `教育学` / `哲学` / `法学` / `文学` / `计算机科学` / `医学` / `数学` / `物理学` / `化学` / `生物学` / `地理学` / `社会学` / `其他`

---

### 配置类

#### WebConfig.java

- 注册 `LoginInterceptor` 拦截所有 `/api/**` 请求
- 放行路径：`/api/user/login`、`/api/user/register`、`/api/search/test`、`/uploads/**`
- 配置跨域：允许所有来源、所有方法、所有头（开发环境）

#### LoginInterceptor.java

- 从请求头 `Authorization` 取 `Bearer {token}`
- 调用 `JwtUtil.validateToken()` 验证
- 通过后将 userId 存入 `request.setAttribute("userId", userId)`

#### FileUploadConfig.java

将 `/uploads/**` 映射到本地文件目录 `file:{uploadPath}`，使上传文件可通过 HTTP 访问。

#### ElasticsearchInitConfig.java

应用启动时检查 ES 中 `literature` 索引是否存在，不存在则自动创建。

---

## 前端模块说明

### 路由

文件：`src/router/index.js`

| 路径 | 组件 | 访问控制 |
|------|------|----------|
| `/` | HomeView | 需登录（有 token） |
| `/login` | Login | 公开 |
| `/register` | Register | 公开 |
| `/profile` | Profile | 需登录 |
| `/admin` | Admin | 需登录 + role === 2 |
| `/review` | Review | 需登录 + role === 2 |

路由守卫逻辑：读取 `localStorage.token`，无 token 跳转 `/login`；访问管理员路由时额外检查 `localStorage.user` 中的 `role` 字段。

### API 封装

文件：`src/api/index.js`

- baseURL：`/api`，超时：30s
- 请求拦截器：自动附加 `Authorization: Bearer {token}`
- 响应拦截器：直接返回 `response.data`（即后端 `Result` 对象）

**literatureAPI：**

| 方法 | 说明 |
|------|------|
| `search(params)` | POST `/literature/search` |
| `add(formData)` | POST `/literature/add`，multipart |
| `getDetail(id)` | GET `/literature/{id}` |
| `delete(id)` | DELETE `/literature/{id}` |
| `update(id, data)` | PUT `/literature/{id}` |
| `getMyContributions()` | GET `/literature/my-contributions` |
| `getFavorites()` | GET `/literature/favorites` |
| `addFavorite(id)` | POST `/literature/favorite/{id}` |
| `removeFavorite(id)` | DELETE `/literature/favorite/{id}` |
| `getSearchHistory()` | GET `/literature/search-history` |
| `saveSearchHistory(keyword)` | POST `/literature/search-history/save` |

**userAPI：**

| 方法 | 说明 |
|------|------|
| `login(data)` | POST `/user/login` |
| `register(data)` | POST `/user/register` |
| `getCurrent()` | GET `/user/current` |
| `getUserList()` | GET `/user/list` |
| `updateRole(userId, role)` | PUT `/user/{id}/role` |
| `disableUser(userId)` | PUT `/user/{id}/disable` |
| `enableUser(userId)` | PUT `/user/{id}/enable` |
| `deleteUser(userId)` | DELETE `/user/{id}/permanent` |
| `changePassword(data)` | PUT `/user/password` |
| `updateProfile(data)` | PUT `/user/profile`，multipart（含头像） |
| `resetPassword(userId)` | PUT `/user/{id}/reset-password` |

### 页面说明

| 页面 | 功能 |
|------|------|
| `HomeView.vue` | 文献搜索（ES全文）、筛选（作者/期刊/分类/年份）、结果列表、添加文献表单、PDF 上传解析、文献详情弹窗、收藏操作、搜索历史 |
| `Login.vue` | 用户名密码登录，登录成功后跳转首页 |
| `Register.vue` | 用户名、密码、昵称注册 |
| `Profile.vue` | 基本信息修改（昵称/邮箱/头像）、修改密码、我的贡献（role=1）、我的收藏、检索历史 |
| `Admin.vue` | 用户列表管理（角色修改/禁用/启用/重置密码/删除） |
| `Review.vue` | 待审核文献列表、审核通过/驳回操作 |

---

## 接口汇总

所有接口均以 `/api` 为前缀，需认证的接口需在请求头携带：

```
Authorization: Bearer {token}
```

---

## 关键流程说明

### 登录流程

```
用户输入用户名+密码
  → POST /api/user/login
  → UserServiceImpl.login()
  → MD5(password) 比对数据库
  → 生成 JWT（有效期7天）
  → 返回 token + UserVO
  → 前端存入 localStorage
```

### 文献搜索流程

```
用户输入关键词 + 筛选条件
  → POST /api/literature/search
  → SearchServiceImpl.search()
  → 构建 ES 查询（matchPhrase + filter）
  → 返回命中列表 + 总数
  → 服务端正则高亮处理
  → 保存搜索历史（去重，最多20条）
  → 前端渲染结果（支持分页）
```

### PDF 解析流程

```
用户选择 PDF 文件
  → POST /api/literature/parse-pdf（multipart）
  → PdfParserUtil.parsePdf()
  → PDFBox 提取前5页文本
  → 正则提取：标题/作者/年份/摘要/DOI/关键词/期刊
  → CategoryClassifier 自动分类
  → 返回字段 Map
  → 前端自动填充添加表单
```

### 文献审核流程

```
role=1 专员上传文献（status=0 待审核）
  → 管理员在 Review 页查看待审核列表
  → 点击通过 → status=1，同步到 ES，可被搜索
  → 点击驳回 → status=2，附驳回原因
  → 专员在个人中心"我的贡献"查看状态
  → 被驳回后可修改后重新提交
```

### 头像上传流程

```
用户选择图片（jpg/png/gif/webp）
  → 前端 el-upload before-upload 回调
  → 生成本地预览 URL（URL.createObjectURL）
  → 点击保存修改
  → PUT /api/user/profile（multipart：nickname + email + avatar文件）
  → FileUploadUtil 生成 UUID 文件名保存到 uploads/
  → 数据库更新 avatar 字段为 /uploads/{uuid}.{ext}
  → 前端从响应 res.data.avatar 更新显示
```
