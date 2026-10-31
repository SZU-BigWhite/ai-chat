# Topic Atlas · 科普知识结构工作台

面向 B 端用户，从 Topic 构建知识树：用户点击一个节点，DeepSeek 生成下一层建议；用户编辑并采纳后进入正式树。可以继续深入任意分支，自主决定颗粒度，最后导出 JSON 供下游系统生成科普内容。

## 项目

| 目录 | 内容 |
| --- | --- |
| `ai-chat-web` | Vue 3、TypeScript、Vite、Vue Router 前端 |
| `ai-chat-server` | Java 17、Spring Boot 3.5.7、Spring AI 1.1.8、JPA 后端 |
| `docs/export.schema.json` | 下游 JSON 数据契约 |

后端保留参考项目的 Spring Boot、JPA 和 Spring AI `ChatClient` 技术栈，按阿里巴巴规约重构为 `controller → service/impl → manager/repository`，并独立划分 DTO、VO、DO、转换器和认证组件。目录职责与检查规则见 [后端开发规范](docs/backend-standards.md)。本机复用参考项目的 MySQL 连接参数，使用独立的 `ai_chat` 数据库；敏感连接信息仅写入被 Git 忽略的后端 `.env`。通用配置默认使用 H2 文件数据库，本机 `.env` 通过 `SPRING_PROFILES_ACTIVE=mysql` 启用 MySQL。

## 本地启动

需要 Java 17、Maven 3.8+、Node.js 22.12+。打开两个 PowerShell 终端：

```powershell
# 终端一：后端
cd E:\ai-chat\ai-chat-server
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
# 编辑 .env，填写 DEEPSEEK_API_KEY；保留已配置的数据库连接参数。
.\start.ps1
```

```powershell
# 终端二：前端
cd E:\ai-chat\ai-chat-web
.\start.ps1
```

访问 http://127.0.0.1:5173 ，注册自己的账号。后端默认 http://127.0.0.1:8080 。前端开发服务器代理 `/api`，不需要配置跨域。

也可分别执行 `mvn spring-boot:run` 和 `npm ci` / `npm run dev`。直接运行 Maven 不会自动加载 `.env`，请提前设置进程环境变量。Windows 如果脚本执行策略阻止运行，可使用 `powershell -NoProfile -ExecutionPolicy Bypass -File .\start.ps1`，仅作用于该进程。

缺少 DeepSeek 密钥时，注册、主题库、知识树手动编辑、保存、复制、版本导出均可使用；AI 操作会明确提示未配置，不会用伪造内容冒充模型响应。

### 模型配置

AI 超时、额度时区、候选数量、会话有效期、登录限流、树容量及初始化资源均支持配置覆盖，完整变量表和使用方式见 [后端配置说明](docs/backend-configuration.md)。修改本地 `.env` 后重启后端生效。

`DEEPSEEK_API_KEY` 必填才可启用 AI，`DEEPSEEK_BASE_URL` 默认 `https://api.deepseek.com`，`DEEPSEEK_MODEL` 默认 `deepseek-flash`。模型名可按账户支持情况调整。参考项目使用的旧名称为 `deepseek-chat`；这里按 [DeepSeek 官方接入文档](https://api-docs.deepseek.com/) 设置可配置默认值。

本机 `.env` 中的模型、温度和密钥配置优先于 YAML 中的默认值；实际采用哪个模型以部署环境为准。`${DEEPSEEK_API_KEY:not-configured}` 是读取规则，`not-configured` 不是有效密钥。请在本地配置中维护真实密钥，不要写入版本控制文件。

提示词位于 `ai-chat-server/src/main/resources/prompts/`。模型负责推荐结构，后端负责格式、长度、重复项、节点归属和版本校验。每用户同一时间仅允许一个 AI 请求，每日调用默认 100 次，可由 `AI_DAILY_LIMIT` 调整；失败调用也记录。超时、上游错误、空结果和不合法 JSON 都不会修改正式知识树。

### 管理员

在后端 `.env` 配置 `ADMIN_EMAIL` 和 `ADMIN_PASSWORD`（至少 12 个字符，UTF-8 不超过 72 字节），重启后创建该管理员。没有默认管理员和默认密码。仅当邮箱不存在时创建，已注册普通账号不会被自动升级；请使用独立管理员邮箱。启动后可以从管理中心维护主题、启停普通用户、查看调用量、耗时和 token 统计。管理员不查看或编辑其他用户的知识树。

### MySQL

创建空数据库：

```sql
CREATE DATABASE ai_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

在 `.env` 设置 `SPRING_PROFILES_ACTIVE=mysql`、`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。首次启动会建表并初始化 12 个主题。H2 与 MySQL 数据独立，切换配置不会自动迁移已有数据。当前使用 Hibernate `ddl-auto=update`，正式运营前应采用受控迁移并备份数据库。

参考服务器实际运行 MySQL 5.7，本机 `.env` 已设置 `DB_DIALECT=org.hibernate.community.dialect.MySQLLegacyDialect`，并添加对应社区方言依赖；MySQL 8 可保持默认方言。参见 [Hibernate 方言说明](https://docs.hibernate.org/orm/6.6/dialect/)。健康接口在连接和内置主题初始化全部完成前返回 503，完成后返回 200。

## 使用

1. 注册登录，选择内置科普方向，或输入文字让 AI 提取 Topic；也可直接填写自定义主题。
2. 创建根节点，调整范围说明。
3. 选中节点，补充本轮拆分要求，生成下一层建议。
4. 勾选、修改建议，点击采纳；未采纳的建议不会写入正式树。
5. 对任意节点继续细分，或手动增删、编辑、确认节点。
6. 任意分支都可停止在不同深度，叶子不会被自动排除；用户可随时导出已保存的结构。
7. JSON 预览可复制；下载时记录不可变导出版本，后续修改不影响旧版本。

内置主题：医疗与健康、营养与食品科学、心理与认知科学、生命科学、动物与植物、地球与自然环境、天文与宇宙、物理与日常现象、化学与材料、人工智能与数字技术、工程与能源、科学方法与科学史。

## 数据与权限

- BCrypt 哈希保存密码，随机高熵 bearer token 登录；数据库仅保存 token 的 SHA-256 哈希，7 天失效，退出立即撤销。前端 token 保存在当前标签页 sessionStorage。
- 多用户账号独立保存数据；每个树、节点、建议和历史导出接口先校验 ownerId，不接受前端指定所有者。
- 节点 ID 稳定；复制树生成全新树和节点 ID。
- 树的变更通过 revision 乐观锁防止旧页面覆盖。修改父节点名称或范围会将后代标记为 REVIEW，提示逐个复核。
- AI 建议绑定生成时的树版本；结构变化后旧建议不能采纳，可重新生成。每轮采纳后只更新当前节点的子节点，既有分支继续保留。
- 单树上限 3000 节点、80 层，属于系统容量边界，最终业务颗粒度仍由用户决定。
- 只记录导出版本，不提供任意编辑操作的撤销历史；删除分支/整树前会要求在界面确认。

## 构建与测试

### 格式与注释规范

前端使用 Prettier，统一两空格缩进、单引号和 Vue 模板换行；后端使用 Spotless 执行阿里巴巴官方 Eclipse 格式规则（四空格、120 列），同时格式化 `pom.xml`。Checkstyle 检查命名、字段封装、显式导入及控制结构等规约。项目根目录 `.editorconfig` 统一 UTF-8 和 LF。业务注释重点说明数据归属、版本冲突、建议采纳和导出快照等约束。

在各自项目目录执行：

```powershell
cd E:\ai-chat\ai-chat-web
npm.cmd run format
npm.cmd run format:check

cd E:\ai-chat\ai-chat-server
mvn.cmd spotless:apply
mvn.cmd spotless:check
```

`mvn.cmd verify` 会执行后端测试、打包、格式检查和 Checkstyle。切换到本次重构版本后，首次使用 `mvn.cmd clean verify` 清除旧包路径下的编译产物。PowerShell 下使用 `npm.cmd`，避免触发 `npm.ps1` 的脚本执行策略限制。

### 运行日志

后端日志同时输出到控制台和 `ai-chat-server/logs/topic-atlas.log`（相对后端启动目录），按日期和文件大小滚动，单文件 10 MB、最多保留 30 天历史归档，总归档上限 200 MB；容量达到上限时会提前清理旧归档。可用环境变量 `LOG_FILE` 指定文件位置，`APP_LOG_LEVEL` 调整本项目日志级别，默认 `INFO`。

- 启动：记录预置主题初始化与应用就绪状态。
- HTTP：记录方法、路由模板、状态码、耗时；正常健康检查仅在 `DEBUG` 打印，避免轮询刷屏。
- 业务：记录账号登录、树写入、建议采纳和导出快照的内部 ID。事务内日志表示操作已执行或已安排，最终接口成功仍应结合请求结束日志判断。
- AI：记录操作类型、模型、结果、耗时和 token 用量；失败记录异常类型与应用代码位置，不输出外部异常原文。

每次 API 请求由后端生成 `requestId`，通过响应头 `X-Request-Id` 返回，并写入该请求的日志上下文。前端浏览器控制台的 `[API]` 日志含相同编号，可据此搜索后端日志。前端开发模式下成功请求使用 `console.debug`（浏览器需开启“详细/Verbose”级别）；失败请求和未处理的页面异常在生产环境也会记录。

日志不记录密码、密钥、Authorization 头、请求体、查询参数或 AI 提示词/回答。浏览器日志仅保存在浏览器控制台，不自动上传。

```powershell
Get-Content E:\ai-chat\ai-chat-server\logs\topic-atlas.log -Tail 50 -Wait
```

### 验证命令

```powershell
cd E:\ai-chat\ai-chat-server
mvn.cmd verify

cd E:\ai-chat\ai-chat-web
npm.cmd ci
npm.cmd run build
npx.cmd playwright install chromium
npm.cmd run test:e2e
```

后端集成测试通过本地 HTTP 上游模拟服务验证真实 Spring AI 请求/响应链路，不调用付费模型。端到端测试使用独立测试端口和内存数据库，验证真实前后端交互。真实 DeepSeek 账号调用需配置你自己的密钥后进行。

打包后还可在项目根目录运行 `node scripts/smoke-persistence.mjs`，使用独立测试数据库验证服务重启后账号、会话、节点和版本仍然保留，同时验证缺少 AI 密钥时的明确错误提示。本地 H2 配置 `WRITE_DELAY=0`，关闭提交后的延迟写入。验证记录见 `docs/verification.md`。

Maven 的启动与测试参数将 Unix-domain socket 临时目录设为项目 `target`，避免当前 Windows 打包应用环境默认临时目录导致的 Java 回环连接异常；不修改系统级 Java 设置。

## 部署

前端 `dist/` 交给 Nginx 等静态服务，配置 history fallback 到 `index.html`，将 `/api` 反向代理到后端。后端可运行 `start.ps1 -Build` 或打包 JAR。部署时使用 HTTPS，通过环境变量注入密钥，采用独立 MySQL 账号。登录限流与 AI 并发保护当前为单实例内存状态；多实例部署需要替换为共享限流设施。

开发服务器仅监听本机。API 使用 bearer token，不使用跨站 Cookie 认证；默认不开放跨域。不要将前端开发服务器作为正式线上服务器。

API 概览见 `docs/api.md`。
