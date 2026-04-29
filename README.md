# 智能文献检索系统

> 基于 Elasticsearch + SpringBoot + Vue 3 的智能文献检索平台 | 本科毕业设计

[![SpringBoot](https://img.shields.io/badge/SpringBoot-3.3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.13.x-blue)](https://www.elastic.co/)
[![Vue](https://img.shields.io/badge/Vue-3.x-green)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 📖 项目简介

本系统是一个面向中小规模学术文献库的**智能检索与管理平台**，解决传统 MySQL 模糊查询在文献检索中**相关性差、排序不合理、无法支持搜索建议**等问题。

系统采用**前后端分离架构**，以 Elasticsearch 作为全文检索引擎，SpringBoot 提供 RESTful API，Vue 3 构建响应式前端，实现了一套完整的**文献上传 → 审核 → 检索 → 收藏 → 历史记录**闭环流程。

---

## 🎯 核心痛点与解决方案

| 痛点 | 传统方案 | 本系统方案 |
|------|----------|------------|
| 检索范围有限 | MySQL `LIKE '%keyword%'` 单字段 | ES 多字段联合检索（标题/作者/摘要/关键词） |
| 结果排序不合理 | 按时间或随机排序 | BM25 相关性评分 + 自定义权重 |
| 无搜索建议 | 用户需完整输入 | Completion Suggester 实时前缀提示 |
| 文献录入繁琐 | 手动填写所有字段 | PDFBox 自动解析元数据并预填 |
| 审核流程不透明 | 无状态跟踪 | 三级状态（待审核/已通过/已驳回）+ 驳回原因 |

---

## 🛠 技术栈

### 后端

| 技术 | 版本 | 作用 |
|------|------|------|
| SpringBoot | 3.3.5 | Web 框架 + RESTful API |
| Elasticsearch | 8.13.x | 全文检索 + 搜索建议 + 高亮 |
| MyBatis-Plus | 3.5.8 | ORM + 分页 + 条件构造器 |
| MySQL | 8.x | 结构化业务数据存储 |
| JWT | 0.11.5 | 无状态身份认证 + 角色权限 |
| PDFBox | 2.0.33 | PDF 文本解析与元数据提取 |
| Lombok | - | 代码简化 |

### 前端

| 技术 | 作用 |
|------|------|
| Vue 3 | 响应式前端框架 |
| Element Plus | UI 组件库 |
| Axios | HTTP 请求封装 |
| Vue Router | 路由守卫 + 权限控制 |

---

## ✨ 核心功能

### 普通用户
- 🔍 **多字段全文检索**：支持标题、作者、摘要、关键词联合检索
- 💡 **实时搜索建议**：输入前缀时自动补全文献标题/关键词
- 🎯 **关键词高亮**：命中词在搜索结果中红色加粗显示
- 📚 **文献收藏**：收藏/取消收藏，个人中心统一管理
- 📜 **搜索历史**：自动记录最近 20 条搜索关键词

### 文献贡献者（普通用户 +）
- 📤 **文献上传**：填写元数据 + 上传 PDF
- 🤖 **PDF 智能解析**：自动提取标题、作者、年份、摘要、关键词、DOI、期刊
- 📋 **贡献记录**：查看已提交文献的审核状态
- 🔄 **重新提交**：被驳回文献修改后再次提交
- ✏️ **修正建议**：对已发布文献提出信息修正

### 系统管理员
- ✅ **文献审核**：通过/驳回，记录审核意见
- 👥 **用户管理**：修改角色、禁用/启用、重置密码、删除用户
- 📊 **审核记录**：查看所有已处理文献的审核历史

---

## 🤖 检索 Agent 逻辑流程

本系统内置一个**轻量级智能检索 Agent**，核心流程如下：
用户输入自然语言查询
↓
意图解析：拆分为 keyword / author / journal / category / year 条件
↓
构建 Bool Query：title / authors / abstractText / keywords 多字段 match
↓
相关性计算：基于 BM25 算法计算每篇文献得分
↓
搜索建议触发：Completion Suggester 从前缀匹配 title/keywords
↓
高亮标记：命中词替换为 <span style='color:red;font-weight:bold'>
↓
结果排序：按 _score 倒序 + 近 3 年文献轻微提权
↓
返回分页结果至前端


---

## 🚀 快速启动

### 环境要求

| 软件 | 版本 |
|------|------|
| JDK | 17+ |
| MySQL | 8.x |
| Elasticsearch | 8.13.x |
| Node.js | 16+ |
| npm | 8+ |

### 启动步骤

#### 1. 准备数据库
```sql
CREATE DATABASE literature_db DEFAULT CHARACTER SET utf8mb4;

2.启动elasticsearch
# Windows
bin\elasticsearch.bat
# Linux/macOS
bin/elasticsearch
验证：浏览器访问 http://localhost:9200

3.启动后端
cd backend
mvn spring-boot:run

4.启动前端
cd frontend
npm install
npm run serve

验证：打开浏览器访问http://localhost:8081

