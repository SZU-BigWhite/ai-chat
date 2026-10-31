# 后端开发规范与分层

本项目依据阿里巴巴官方 [P3C 仓库](https://github.com/alibaba/p3c) 的 Java 开发手册与规约组织代码。规范查阅日期：2026-11-01。

参考来源：

- [Java 开发手册（黄山版）](https://github.com/alibaba/p3c/blob/master/Java%E5%BC%80%E5%8F%91%E6%89%8B%E5%86%8C%28%E9%BB%84%E5%B1%B1%E7%89%88%29.pdf)
- [命名风格](https://github.com/alibaba/p3c/blob/master/p3c-gitbook/编程规约/命名风格.md)、[应用分层](https://github.com/alibaba/p3c/blob/master/p3c-gitbook/工程结构/应用分层.md)、[日志规约](https://github.com/alibaba/p3c/blob/master/p3c-gitbook/异常日志/日志规约.md)
- [官方 Eclipse 格式配置](https://github.com/alibaba/p3c/blob/master/p3c-formatter/eclipse-codestyle.xml)，已保存到 `ai-chat-server/config/alibaba-eclipse.xml`，由 Spotless 执行。

## 目录职责

| 包 | 职责 |
| --- | --- |
| `controller` | HTTP 路由、参数校验及响应；只依赖服务契约，不直接操作 Repository，不返回数据库实体 |
| `service` / `service.impl` | 业务接口及实现；维护用户所有权、版本检查和写事务 |
| `manager` | DeepSeek/Spring AI 调用、严格 JSON 解析、树遍历与序列化 |
| `repository` | Spring Data JPA 数据访问接口 |
| `model.entity` | 表对应的 DO，字段私有，显式保留原表名 |
| `model.dto` | 输入及服务间传递的数据，按用途独立定义 |
| `model.vo` | 对外响应；导出模型不包含编辑状态及账号信息 |
| `model.domain` / `model.enums` | 树节点和节点状态、账号角色、AI 操作类型 |
| `converter` | DO 到公开 VO 的显式映射 |
| `security` | 登录限流、会话认证和管理员鉴权 |
| `exception` | 业务异常、统一错误响应、日志异常脱敏 |
| `config` / `constant` | 配置、启动初始化和稳定业务常量 |

运行参数与固定协议分开管理，新增参数的配置方式、校验范围、主题资源与提示词覆盖方式见 [后端配置说明](backend-configuration.md)。

## 代码约束

- 类型与方法使用清晰英文命名；数据库对象使用 `DO` 后缀，输入使用 `DTO`，对外视图使用 `VO`，服务实现使用 `Impl`。
- POJO 数据字段采用包装类型，私有可变字段通过访问方法操作；输入的空值在 DTO 校验或构造时处理。
- DTO/VO 使用 Java 17 record 表达不可变数据；这属于本项目的 Java 版本约定。包含密码或令牌的 record 覆盖 `toString()`，不输出凭证。
- 不使用星号导入；不在一行声明多个变量；条件和循环必须使用大括号。
- 公共类、服务接口及关键业务约束写清注释，不为简单 getter/setter 堆砌重复说明。
- 多表修改在服务层建立事务，写操作显式设置 `rollbackFor = Exception.class`；只读查询与写事务区分。
- AI 网络调用期间不持有知识树事务。模型回答仅接受一个纯 JSON 对象，不恢复 Markdown 兼容逻辑；仍校验字段、长度、数量和重复名称。
- 保留接口 URL、有效输入的返回结构、表名、账号哈希算法和历史树 JSON 字段，不统一包裹响应，以免破坏已有前端和导出消费者。
- `revision` 使用包装类型后通过值比较，避免超过整数缓存范围时错误判断建议版本。
- 树所有权必须由服务端判断；管理员只管理账号状态、主题和调用统计。

## 异常、日志与配置

业务异常保留内部原因，由统一处理器转换为原有 `message` 响应。JSON 解析等可预期异常使用具体类型捕获；HTTP 顶层保留兜底异常处理。

日志使用 SLF4J 和占位符，记录请求编号、资源 ID、状态、耗时及 token 统计。异常输出保留堆栈及原因类型，移除可能包含 SQL 参数、凭证或模型正文的异常消息。日志归档最多保留 30 天，同时受总大小限制；高流量场景应按容量调整归档策略。数据库凭证和模型密钥从环境变量或本机忽略提交的 `.env` 注入。

## 自动检查与人工检查边界

在 `ai-chat-server` 目录运行：

```powershell
mvn.cmd spotless:apply
mvn.cmd verify
```

`verify` 执行测试、打包、Spotless 格式检查和 Checkstyle 规约检查。Checkstyle 覆盖命名、字段可见性、显式导入、大括号、声明分行、类注释等可稳定检查的规则。架构测试防止控制器重新依赖 Repository/DO，并检查服务接口与实体封装。

没有声称通过全部 P3C 插件规则。官方 P3C PMD 构建使用较旧的 PMD 基线；本项目采用能解析 Java 17 record 的 Checkstyle 引擎检查选定规则。业务合理性、事务边界、并发正确性、SQL 索引和注释质量仍需要代码审查及场景测试。

测试使用本地模拟模型及独立数据库，不调用收费 DeepSeek，也不修改业务数据库。旧版本到新版本的兼容验证应覆盖账号、会话、节点、主题种子及历史导出。
