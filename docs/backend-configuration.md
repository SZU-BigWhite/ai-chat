# 后端配置与固定边界

运行参数使用 Spring Boot `@ConfigurationProperties` 绑定并在启动时校验。默认值位于 `ai-chat-server/src/main/resources/application.yml`，部署时可以通过环境变量、外部 Spring 配置文件或启动参数覆盖。使用 `start.ps1` 时，也可以在本地 `ai-chat-server/.env` 中设置下面的变量，修改后重启后端。

完整示例见 `ai-chat-server/.env.example`。不要用示例文件覆盖已有 `.env` 中的密钥和数据库凭证。直接运行 JAR 或 IDE 不会自动加载 `.env`，需要设置运行环境变量或使用外部配置文件。

## 运行策略

| 环境变量 | 默认值 | 用途及边界 |
| --- | --- | --- |
| `AI_DAILY_LIMIT` | `100` | 每用户每日调用额度，至少 1 |
| `AI_QUOTA_ZONE` | `Asia/Shanghai` | 每日额度的日期边界，使用合法时区 ID |
| `AI_CONNECT_TIMEOUT` | `15s` | AI HTTP 连接超时，至少 1ms |
| `AI_READ_TIMEOUT` | `120s` | AI HTTP 读取超时，至少 1ms |
| `AI_EXTRACT_CANDIDATES` | `3` | 提取根主题的候选上限，1～10；同时作用于提示词和解析结果 |
| `DEEPSEEK_MAX_TOKENS` | `2400` | 单次模型输出 token 上限 |
| `AI_RETRY_MAX_ATTEMPTS` | `2` | Spring AI 总尝试次数（含首次） |
| `AI_RETRY_INITIAL_INTERVAL` / `AI_RETRY_MULTIPLIER` / `AI_RETRY_MAX_INTERVAL` | `1s` / `2` / `5s` | Spring AI 重试退避参数 |
| `AUTH_SESSION_TTL` | `7d` | 新签发会话的有效期，至少 1s；现有会话保留原到期时间 |
| `AUTH_LOGIN_WINDOW` | `15m` | 单个来源地址的登录、注册共用限流窗口，至少 1s |
| `AUTH_LOGIN_MAX_ATTEMPTS` | `30` | 每窗口允许的请求次数，至少 1 |
| `TREE_MAX_NODES` | `3000` | 每棵树节点总数上限（含根），至少 1 |
| `TREE_MAX_DEPTH` | `80` | 树层数上限（根为第 1 层），1～80 |
| `ADMIN_DISPLAY_NAME` | `管理员` | 首次创建管理员的显示名，不改已有账号 |
| `ADMIN_RECENT_USAGE_LIMIT` | `100` | 管理端最近 AI 调用条数，1～1000 |
| `TOPIC_DEFAULT_COLOR` | `#548b9d` | 新增/更新主题未提供颜色时的默认色，六位十六进制颜色 |
| `LOG_MAX_FILE_SIZE` / `LOG_MAX_HISTORY` / `LOG_TOTAL_SIZE_CAP` | `10MB` / `30` / `200MB` | 日志归档大小、天数与总容量 |

默认行为保持一致。缩小树容量后不会截断已保存的树，但会阻止超过新上限的新增节点。深度 80 是当前递归遍历及 JSON 结构的安全边界，不能仅通过部署参数继续加深，需要先评估遍历、解析与前端渲染。前端请求超时目前为 145 秒，调大模型超时或重试次数时，需要同步评估前端及反向代理超时。

## 预置主题

12 个默认主题从 `ai-chat-server/src/main/resources/seed/topics.json` 加载，ID、名称、描述、图标、颜色及排序与原版本一致。

- `TOPIC_SEED_ENABLED=true`：启动时补齐缺少的主题；设为 `false` 后不读取种子文件，也不删除已有主题。
- `TOPIC_SEED_LOCATION=classpath:seed/topics.json`：可以改为 `file:E:/config/topics.json`。
- 文件必须是 JSON 数组，每项包含 `id`、`name`、`description`、`icon`、`color`、`sortOrder`。加载前校验字段、重复 ID 和多余内容，失败时停止初始化。
- 已有主题以数据库为准。修改种子文件不会覆盖管理员的编辑或停用状态；已有内容请在管理页面维护。

## 提示词

`AI_EXTRACT_PROMPT` 和 `AI_EXPAND_PROMPT` 默认分别指向 `classpath:prompts/extract.txt`、`classpath:prompts/expand.txt`，支持使用 `file:` 覆盖。资源在启动时读取；缺失或空白会阻止启动。

模板支持三个明确的占位符：

- `{{maxCandidates}}`：本次提取或拆分的候选数量上限。
- `{{maxNameLength}}`：节点名称的固定长度边界。
- `{{maxDescriptionLength}}`：范围说明的固定长度边界。

普通 JSON 大括号保持原样。自定义提示词仍须遵守纯 JSON 输出协议；服务端继续验证结构、字段、长度和重复名称，不接受 Markdown 代码块。

## 固定协议与可调策略的分工

`BusinessConstants` 只保存 API/数据库字段长度、候选数组上限、密码算法限制、认证标识及导出协议版本等固定约束。DTO 校验、数据库列定义和 AI 解析共用这些常量，避免各处数字漂移。修改字段边界时必须同时评估数据库迁移、前端表单和外部消费者，不能用运行配置绕过。

节点状态通过 `NodeStatusEnum` 解析，拒绝非法字符串和枚举数字序号；有效 JSON 字段及状态字符串保持兼容。HTTP 状态码使用 Spring `HttpStatus`，标准请求头使用 `HttpHeaders`。固定路由、JSON 字段名、提示文案和密码摘要算法保留在对应代码中，不作为运营配置暴露。

新增可调策略时，须同时添加配置绑定、合法性约束、调用方消费、示例和非默认值测试。配置类中的默认值支持脱离主 YAML 的测试场景，修改默认值时应同步更新 YAML 和示例文档。
