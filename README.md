# KnowHeart · 知意

基于 **Spring AI** 与 **RAG** 的智能恋爱顾问 Web 应用。结合经典情感心理学知识库、用户画像与多轮对话记忆，提供温暖、可落地的恋爱与亲密关系建议。

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-green)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M6-blue)

---

## 功能特性

- **RAG 知识增强**：从 PostgreSQL + pgvector 向量库检索《爱的艺术》《亲密关系》《幸福的婚姻》等文档片段，回答有据可依
- **多轮对话记忆**：登录用户对话持久化至 MySQL，支持最近 **10 条** 对话的创建、恢复与删除
- **用户画像**：登录后 AI 会从对话中自动提取昵称、年龄、城市、恋爱状态等信息，并用于个性化回复
- **工具调用**：集成高德地图 API，可查询约会地点与天气（需配置 `AMAP_API_KEY`）
- **流式输出**：SSE 流式聊天，前端 DeepSeek 式布局 + 简洁苹果风格 UI
- **访客模式**：未登录也可聊天，但不会保存历史记录

---

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                     浏览器 (index.html)                      │
└───────────────────────────┬─────────────────────────────────┘
                            │ REST / SSE
┌───────────────────────────▼─────────────────────────────────┐
│              Spring Boot 3.4 + Spring AI 1.0                 │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │ ChatClient  │  │ RAG Advisor  │  │ HybridChatMemory    │ │
│  │ (DeepSeek)  │  │ (Question    │  │ MySQL / InMemory    │ │
│  │             │  │  Answer)     │  │                     │ │
│  └─────────────┘  └──────┬───────┘  └──────────┬──────────┘ │
└──────────────────────────┼─────────────────────┼──────────────┘
                           │                     │
              ┌────────────▼────────┐   ┌────────▼────────┐
              │ PostgreSQL          │   │ MySQL           │
              │ + pgvector          │   │ 用户 / 画像 /   │
              │ love_knowledge 表   │   │ 对话历史        │
              └────────────┬────────┘   └─────────────────┘
                           │
              ┌────────────▼────────┐
              │ 阿里云百炼 Embedding │
              │ text-embedding-v2   │
              └─────────────────────┘
```

| 组件 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.4.5、Java 21 |
| AI 对话 | DeepSeek API（`deepseek-v4-flash`） |
| 向量化 | 阿里云百炼 DashScope（`text-embedding-v2`） |
| 向量存储 | PostgreSQL + pgvector（`love_knowledge` 表） |
| 业务数据 | MySQL 8.x |
| 前端 | 原生 HTML / CSS / JavaScript（单页应用） |

---

## 环境要求

- **JDK 21**
- **Maven 3.9+**
- **MySQL 8.x**（业务库 `knowheart`）
- **PostgreSQL 14+**（向量库 `knowheart_vector`，需安装 [pgvector](https://github.com/pgvector/pgvector) 扩展）
- **DeepSeek API Key**（对话）
- **阿里云百炼 API Key**（Embedding，RAG 必需）
- **高德 API Key**（可选，地点/天气工具）

---

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/querri1/KnowHeart.git
cd KnowHeart
```

### 2. 配置环境变量

在系统中设置以下变量（或在 IDE 运行配置中填写）：

| 变量名 | 说明 | 必需 |
|--------|------|------|
| `DEEPSEEK_API_KEY` | DeepSeek 对话 API | 是 |
| `DASHSCOPE_API_KEY` | 阿里云百炼 Embedding | 是 |
| `MYSQL_PASSWORD` | MySQL root 密码 | 是 |
| `POSTGRES_PASSWORD` | PostgreSQL 密码 | 是 |
| `AMAP_API_KEY` | 高德地图（约会地点/天气） | 否 |


### 3. 初始化数据库

**MySQL** — 创建库并执行建表脚本：

```bash
mysql -u root -p < sql/init.sql
```

**PostgreSQL** — 创建向量库并启用 pgvector：

```bash
psql -U postgres -f sql/init_pgvector.sql
```

首次启动时，应用会自动将 `src/main/resources/documents/` 下的 Markdown 文档向量化并写入 `love_knowledge` 表（若表为空）。

### 4. 修改连接配置（如需要）

编辑 `src/main/resources/application.yml`，按需调整 MySQL 与 PostgreSQL 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/knowheart?...
    username: root

knowheart:
  vector:
    datasource:
      url: jdbc:postgresql://localhost:5432/knowheart_vector
      username: postgres
```

### 5. 启动应用

```bash
./mvnw spring-boot:run
```

Windows：

```cmd
mvnw.cmd spring-boot:run
```

浏览器访问：**http://localhost:8080**

---

## 知识库文档

RAG 知识来源位于 `src/main/resources/documents/`，默认包含：

| 文件 | 说明 |
|------|------|
| `爱的艺术.md` | 埃里希·弗洛姆 |
| `亲密关系（第5版).md` | 罗兰·米勒 |
| `幸福的婚姻.md` | 约翰·戈特曼 |
| `恋爱常见问题-单身篇.md` | 常见问题 FAQ |

可自行增删 Markdown 文件；清空 `love_knowledge` 表后重启应用即可重新导入。

---

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/knowheart/auth/register` | 注册 |
| `POST` | `/api/knowheart/auth/login` | 登录 |
| `POST` | `/api/knowheart/auth/logout` | 退出 |
| `GET` | `/api/knowheart/auth/me` | 当前用户与画像 |
| `GET` | `/api/knowheart/profile?userId=` | 查询用户画像 |
| `GET` | `/api/knowheart/conversations` | 对话列表（需登录） |
| `POST` | `/api/knowheart/conversations` | 新建对话 |
| `DELETE` | `/api/knowheart/conversations/{id}` | 删除对话 |
| `GET` | `/api/knowheart/conversations/{id}/messages` | 加载消息 |
| `GET` | `/api/knowheart/chat/stream?msg=&conversationId=` | SSE 流式聊天（主入口） |
| `GET` | `/api/knowheart/health` | 健康检查 |

登录后请求需在 Header 中携带：`Authorization: Bearer <token>`。  
未登录时前端会生成本地 `userId` 传参，可聊天但不会写入 MySQL 对话历史。

---

## 配置说明

`application.yml` 中 `knowheart` 节点：

```yaml
knowheart:
  chat:
    memory:
      retrieve-size: 10        # 每次带入模型的历史消息条数
  max-conversations: 10        # 每用户最多保留的对话数
  rag:
    top-k: 6                   # RAG 检索条数
    similarity-threshold: 0.0  # 相似度阈值（中文建议偏低）
    chunk-size: 500            # 文档切分大小
    chunk-overlap: 100         # 切分重叠长度
  vector:
    table-name: love_knowledge # pgvector 表名
    dimensions: 1536           # 与 embedding 模型维度一致
    datasource:                # PostgreSQL 向量库连接
      url: jdbc:postgresql://localhost:5432/knowheart_vector
      username: postgres
      password: ${POSTGRES_PASSWORD}
```

---

## 项目结构

```
KnowHeart/
├── sql/
│   ├── init.sql                 # MySQL 建表
│   └── init_pgvector.sql        # PostgreSQL / pgvector 说明
├── src/main/
│   ├── java/com/practice/knowheart/
│   │   ├── config/              # 数据源、RAG、Embedding、向量库
│   │   ├── controller/          # REST 接口
│   │   ├── entity/              # JPA 实体
│   │   ├── memory/              # HybridChatMemory
│   │   ├── repository/
│   │   ├── service/             # 业务与 AI 服务
│   │   └── tool/                # 高德地图工具
│   └── resources/
│       ├── application.yml
│       ├── documents/           # RAG 知识库 Markdown
│       └── static/index.html    # 前端单页
└── pom.xml
```

---

## 常见问题

**Q: 启动后 RAG 检索不到内容？**  
确认 `DASHSCOPE_API_KEY` 有效，PostgreSQL 已安装 pgvector，且 `love_knowledge` 表中有数据。可查看启动日志中的文档加载条数。

**Q: 用户数据写进了 PostgreSQL 而不是 MySQL？**  
本项目使用双数据源：MySQL 为主库（`@Primary`），PostgreSQL 仅用于向量存储。若遇异常，请确认 `DataSourceConfig` 与 `application.yml` 未被改动。

**Q: 对话历史没有保存？**  
仅**登录用户**的对话会持久化到 MySQL；访客使用本地 UUID，刷新后仍可继续聊天，但不会出现在侧栏「最近对话」中。

---

## 免责声明

本项目仅供学习与技术演示。AI 生成的恋爱与情感建议**不能替代**专业心理咨询或医疗诊断。如遇严重情感或心理健康问题，请寻求合格专业人士帮助。

---

## 致谢

- [Spring AI](https://spring.io/projects/spring-ai)
- [DeepSeek](https://www.deepseek.com/)
- [阿里云百炼](https://help.aliyun.com/zh/model-studio/)
- [pgvector](https://github.com/pgvector/pgvector)

---

如有问题或改进建议，欢迎提交 Issue 或 Pull Request。
