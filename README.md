# RAG 检索增强生成系统

基于文档检索和大语言模型的智能问答系统。上传文档后，系统自动分割、向量化并存储；用户提问时，系统检索相关文档片段，结合 LLM 生成有依据的答案。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.1 |
| ORM | MyBatis-Plus | 3.5.16 |
| 关系数据库 | PostgreSQL | 18.1 |
| 向量数据库 | Qdrant | 1.16.3 |
| 文档解析 | Apache Tika | 2.9.1 |
| HTTP 客户端 | Spring WebClient | - |
| 工具库 | Lombok, Hutool | 5.8.25 |
| 前端框架 | Vue 3 (Composition API) | 3.4.x |
| UI 组件库 | Element Plus | 2.5.x |
| 状态管理 | Pinia | 2.1.x |
| HTTP 客户端 | Axios | 1.6.x |
| 构建工具 | Vite | 5.x |
| AI 服务 | OpenRouter API | - |

## 环境要求

- **JDK** 17+
- **Maven** 3.8+
- **Node.js** 18+（npm 或 pnpm）
- **PostgreSQL** 15+（当前使用 18.1）
- **Qdrant** 1.7+（当前使用 1.16.3）

## 项目结构

```
rag-retrieval-system/
├── src/main/java/com/example/rag/
│   ├── controller/          # REST API 控制器
│   │   ├── DocumentController.java
│   │   └── QueryController.java
│   ├── service/             # 业务逻辑接口
│   │   └── impl/            # 业务逻辑实现
│   ├── mapper/              # MyBatis-Plus Mapper
│   ├── entity/              # 数据库实体类
│   ├── vo/                  # 视图对象（请求/响应）
│   ├── config/              # 配置类
│   ├── exception/           # 异常处理
│   └── util/                # 工具类
├── src/main/resources/
│   ├── application.yml      # 应用配置
│   └── logback-spring.xml   # 日志配置
├── src/test/                # 测试代码
├── frontend/                # Vue3 前端项目
│   ├── src/
│   │   ├── views/           # 页面组件
│   │   ├── components/      # 通用组件
│   │   ├── stores/          # Pinia 状态管理
│   │   ├── api/             # API 封装
│   │   ├── router/          # 路由配置
│   │   └── utils/           # 工具函数
│   ├── package.json
│   └── vite.config.ts
├── docker-compose.yml       # 基础设施服务
├── pom.xml
└── README.md
```

## 快速开始

### 1. 启动基础设施

使用 Docker Compose 启动 PostgreSQL 和 Qdrant：

```bash
docker-compose up -d
```

或手动确保以下服务可用：
- PostgreSQL：`192.168.14.128:5432`
- Qdrant：`192.168.14.128:6333`

### 2. 初始化数据库

连接 PostgreSQL 并执行：

```sql
CREATE DATABASE rag_system;

\c rag_system

CREATE TABLE document (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    upload_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_time TIMESTAMP,
    error_message TEXT,
    chunk_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    vector_id VARCHAR(100) NOT NULL,
    char_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);

CREATE TABLE query_history (
    id BIGSERIAL PRIMARY KEY,
    query_text TEXT NOT NULL,
    answer TEXT NOT NULL,
    retrieved_chunks JSONB,
    query_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time_ms INTEGER
);

CREATE INDEX idx_document_status ON document(status);
CREATE INDEX idx_document_upload_time ON document(upload_time);
CREATE INDEX idx_chunk_document_id ON document_chunk(document_id);
CREATE INDEX idx_chunk_vector_id ON document_chunk(vector_id);
CREATE INDEX idx_query_time ON query_history(query_time);
```

Qdrant 的 Collection（`document_chunks`）会在应用启动时自动创建。

### 3. 配置环境变量

```bash
# Windows
set OPENROUTER_API_KEY=your_api_key_here

# Linux/Mac
export OPENROUTER_API_KEY=your_api_key_here
```

### 4. 启动后端

```bash
mvn clean compile
mvn spring-boot:run
```

后端启动在 `http://localhost:8082`。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器启动在 `http://localhost:3000`，API 请求自动代理到后端。

## 配置说明

### application.yml 核心配置

```yaml
# 服务端口
server:
  port: 8082

# 数据库连接
spring:
  datasource:
    url: jdbc:postgresql://192.168.14.128:5432/rag_system
    username: postgres
    password: root@Ubuntu123

# Qdrant 向量数据库
qdrant:
  host: 192.168.14.128
  port: 6333
  collection-name: document_chunks

# OpenRouter API（AI 服务）
openrouter:
  api-key: ${OPENROUTER_API_KEY}       # 从环境变量读取
  base-url: https://openrouter.ai/api/v1
  embedding-model: openai/text-embedding-3-small
  chat-model: openai/gpt-4             # 可切换为其他模型
  timeout: 30000
  max-retries: 3

# RAG 参数
rag:
  document:
    chunk-size: 800                     # 文档片段大小（字符）
    chunk-overlap: 150                  # 片段重叠大小（字符）
    supported-formats: pdf,txt,docx,md
    max-file-size: 52428800             # 50MB
    upload-dir: ./uploads
  retrieval:
    top-k: 5                            # 检索返回片段数
    min-score: 0.7                      # 最小相似度阈值
```

### 日志配置

日志通过 `logback-spring.xml` 配置，支持 Spring Profile：

| Profile | 应用日志级别 | Root 级别 |
|---------|-------------|-----------|
| dev | DEBUG | INFO |
| prod | INFO | WARN |
| default | DEBUG | INFO |

日志文件位置：
- 全量日志：`logs/rag-system.log`
- 错误日志：`logs/rag-system-error.log`
- 滚动策略：单文件 10MB，保留 30 天，总上限 1GB

### 前端配置

前端代理配置在 `frontend/vite.config.ts`：

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

> **注意**：如果后端端口修改为非 8080，需同步修改此代理配置。

## API 接口

所有接口返回统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1705287000000
}
```

### 文档管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/documents` | 上传文档（multipart/form-data，字段名 `file`） |
| GET | `/api/documents?page=1&size=10` | 获取文档列表（分页） |
| GET | `/api/documents/{id}` | 获取文档详情（含片段列表） |
| DELETE | `/api/documents/{id}` | 删除文档（同时删除向量和文件） |

**上传文档示例**：

```bash
curl -X POST http://localhost:8082/api/documents \
  -F "file=@/path/to/document.pdf"
```

**支持的文件格式**：PDF、TXT、DOCX、MD（最大 50MB）

### 智能问答

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/query` | 提交查询 |
| GET | `/api/query/history?page=1&size=20` | 获取查询历史（分页） |

**查询示例**：

```bash
curl -X POST http://localhost:8082/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "什么是民事权利能力？", "topK": 5}'
```

**查询响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "query": "什么是民事权利能力？",
    "answer": "民事权利能力是指...",
    "references": [
      {
        "documentId": 1,
        "documentName": "民法典.pdf",
        "content": "第十三条 自然人从出生时起...",
        "score": 0.92
      }
    ],
    "responseTimeMs": 1500
  }
}
```

### 错误响应

| HTTP 状态码 | 说明 |
|-------------|------|
| 400 | 请求参数错误（格式不支持、参数缺失等） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 运行测试

```bash
# 后端测试
mvn test

# 运行指定测试类
mvn test -Dtest=DocumentServiceImplTest
```

## 许可证

MIT License
