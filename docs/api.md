# HTTP API

接口统一以 `/api` 开头，正常响应直接返回 JSON。错误响应为 `{ "message": "错误说明" }`，使用 400/401/403/404/409/422/429/502/503 等 HTTP 状态码。除健康检查和登录注册外，需要 `Authorization: Bearer <token>`。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/health` | 服务状态 |
| POST | `/auth/register` | `{email,password,name}` 注册 |
| POST | `/auth/login` | `{email,password}` 登录 |
| GET / POST | `/auth/me` / `/auth/logout` | 当前账号 / 撤销会话 |
| GET | `/topics` | 已启用内置主题 |
| GET | `/ai/status` | AI 是否已配置 |
| POST | `/ai/extract` | `{text}` 提取 1–3 个 Topic 候选 |
| GET / POST | `/trees` | 列出自己的树 / 创建 `{name,description,topicId?}` |
| GET / DELETE | `/trees/{id}` | 详情 / 删除，删除需 `?revision=N` |
| POST | `/trees/{id}/copy` | 复制完整结构，生成新 ID |
| PATCH | `/trees/{id}/nodes/{nodeId}` | `{revision,name,description,status}` 编辑节点 |
| POST | `/trees/{id}/nodes/{nodeId}/children` | `{revision,name,description}` 手动添加 |
| DELETE | `/trees/{id}/nodes/{nodeId}?revision=N` | 删除子树；禁止删除根节点 |
| POST | `/trees/{id}/nodes/{nodeId}/expand` | `{revision,instruction,count}` 生成下一层建议；count 2–10 |
| GET | `/trees/{id}/suggestions` | 当前版本未采纳建议 |
| POST | `/trees/{id}/suggestions/{suggestionId}/accept` | `{revision,children:[{name,description}]}` 采纳选中/编辑的建议 |
| GET | `/trees/{id}/export-preview` | 预览干净的下游 JSON |
| GET / POST | `/trees/{id}/exports` | 历史列表 / 保存版本并导出，POST 需 `?revision=N` |
| GET | `/trees/{id}/exports/{versionId}` | 下载历史版本 |
| GET / POST | `/admin/topics` | 所有主题 / 新建主题 |
| PUT | `/admin/topics/{id}` | 编辑主题（含 enabled 和 sortOrder） |
| GET | `/admin/users` | 脱敏用户列表，不返回密码和 token |
| PATCH | `/admin/users/{id}` | `{enabled}` 启停普通账号 |
| GET | `/admin/stats` | 用户数、树数、调用总数、最近 100 条 AI 使用日志 |

节点状态 `DRAFT`（待完善）、`CONFIRMED`（用户确认）、`REVIEW`（祖先范围修改后待复核）仅用于工作台，不进入下游 JSON。

认证为账号级隔离，没有企业组织、邀请成员、共享树或多人同时协作编辑。`revision` 防止同一账号多个标签页覆盖。返回 409 时应保留当前输入并提示刷新，禁止静默覆盖。

新建/编辑管理员 Topic 字段：`name, description, icon, color, sortOrder, enabled`。禁用 Topic 不删除用户已创建的知识树。
