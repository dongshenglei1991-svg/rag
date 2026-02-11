# 设计文档：RAG 检索系统

## 概述

本系统是一个基于检索增强生成（RAG）技术的智能问答系统，专门用于民法典知识问答。系统采用前后端分离架构，后端使用 Spring Boot 3.x 提供 RESTful API，前端使用 Vue3 构建交互界面。

核心功能包括：
- 文档上传和管理（支持 PDF、TXT、DOCX、MD 格式）
- 文档自动分割和向量化
- 基于语义相似度的智能检索
- 结合大语言模型的答案生成
- 友好的前端交互界面

系统通过将文档转换为向量表示并存储在向量数据库中，实现高效的语义检索。当用户提问时，系统检索最相关的文档片段，并结合大语言模型生成准确、有依据的答案。

## 技术栈选择

### 后端技术栈
- **框架**: Spring Boot 3.2.x
  - 选择理由：成熟稳定，生态丰富，支持响应式编程
- **ORM**: MyBatis-Plus 3.5.x
  - 选择理由：简化 CRUD 操作，提供强大的条件构造器
- **数据库**: PostgreSQL 15.x+ (支持 15、16、17、18 等更高版本)
  - 选择理由：支持 JSON 类型，性能优秀，开源免费，向后兼容性好
  - 当前使用版本：**PostgreSQL 18.1**
- **向量数据库**: Qdrant 1.7.x+
  - 选择理由：易于部署，支持 REST API，性能优秀，支持过滤查询
  - 当前使用版本：**Qdrant 1.16.3**
- **文档解析**: Apache Tika 2.9.x
  - 选择理由：支持多种文档格式，自动检测文件类型
- **HTTP 客户端**: Spring WebClient
  - 选择理由：响应式，支持异步调用
- **工具库**: 
  - **Lombok**: 简化 Java 代码，自动生成 getter/setter、构造函数等
  - **Hutool 5.8.x**: 强大的 Java 工具类库，提供文件、字符串、日期等常用工具
- **配置管理**: Spring Boot Configuration Properties
- **日志**: SLF4J + Logback

### 前端技术栈
- **框架**: Vue 3.4.x (Composition API)
  - 选择理由：现代化，性能优秀，TypeScript 支持好
- **UI 组件库**: Element Plus 2.5.x
  - 选择理由：组件丰富，文档完善，适合后台系统
- **HTTP 客户端**: Axios 1.6.x
  - 选择理由：易用，支持拦截器，Promise 风格
- **状态管理**: Pinia 2.1.x
  - 选择理由：Vue 3 官方推荐，API 简洁
- **路由**: Vue Router 4.x
- **构建工具**: Vite 5.x
  - 选择理由：快速的开发服务器，优秀的构建性能

### 外部服务
- **向量化模型**: OpenRouter API (调用 OpenAI text-embedding-3-small)
  - 选择理由：OpenRouter 支持调用 OpenAI Embeddings，统一使用一个 API 密钥
  - 端点：`https://openrouter.ai/api/v1/embeddings`
  - 备选方案：本地部署的开源模型（如 BGE-M3）
- **大语言模型**: OpenRouter API
  - 选择理由：支持多种模型（GPT-4、Claude、Gemini 等），统一接口，灵活切换
  - 兼容 OpenAI API 格式，易于集成
  - 端点：`https://openrouter.ai/api/v1/chat/completions`
  - 备选方案：直接使用 OpenAI API、Azure OpenAI、本地部署的模型

**注意**：使用 OpenRouter 可以统一管理 API 密钥，同时调用 Embeddings 和 Chat Completions 服务。

### 开发工具
- **Java 版本**: JDK 17+
- **构建工具**: Maven 3.6.3+ (推荐 3.8.x 或更高版本)
- **Node 版本**: Node.js 18+
- **包管理器**: npm 或 pnpm

### 环境配置信息

#### 数据库连接信息

**PostgreSQL 18.1**:
- 地址：`192.168.14.128:5432`
- 数据库：`rag_system`
- 用户名：`postgres`
- 密码：`root@Ubuntu123`
- JDBC URL：`jdbc:postgresql://192.168.14.128:5432/rag_system`

**Qdrant 1.16.3**:
- 地址：`192.168.14.128`
- REST API 端口：`6333`
- gRPC 端口：`6334`
- REST API URL：`http://192.168.14.128:6333`
- Web UI：`http://192.168.14.128:6333/dashboard`

**OpenRouter API**:
- 端点：`https://openrouter.ai/api/v1`
- API Key：需要在配置文件中设置
- 向量化模型：`openai/text-embedding-3-small`
- 对话模型：可配置（如 `openai/gpt-4`、`anthropic/claude-3-sonnet` 等）

## 系统架构

### 整体架构

系统采用三层架构设计：

```
┌─────────────────────────────────────────────────────────┐
│                      前端层 (Vue3)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ 文档管理页面  │  │  问答页面    │  │  配置页面    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          │ HTTP/REST
┌─────────────────────────────────────────────────────────┐
│                   后端层 (Spring Boot)                   │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Controller 层                        │  │
│  │  DocumentController  │  QueryController          │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Service 层                           │  │
│  │  DocumentService  │  EmbeddingService            │  │
│  │  RetrievalService │  LLMService                  │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Repository 层                        │  │
│  │  DocumentMapper  │  ChunkMapper                  │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
┌───────────────┐  ┌──────────────┐  ┌─────────────┐
│  PostgreSQL   │  │   Qdrant     │  │  OpenAI API │
│  (元数据存储)  │  │ (向量存储)   │  │ (AI 服务)   │
└───────────────┘  └──────────────┘  └─────────────┘
```

### 核心流程

#### 文档上传流程
```
用户上传文档
    ↓
验证文件格式
    ↓
保存文件到本地/对象存储
    ↓
提取文本内容 (Apache Tika)
    ↓
分割文档为片段 (500-1000 字符，重叠 100-200 字符)
    ↓
调用 Embedding API 向量化每个片段
    ↓
存储向量到 Qdrant
    ↓
存储元数据到 PostgreSQL
    ↓
返回成功响应
```

#### 查询问答流程
```
用户提交查询
    ↓
调用 Embedding API 向量化查询
    ↓
在 Qdrant 中执行相似度搜索 (Top-K)
    ↓
获取最相关的文档片段
    ↓
构建提示词 (查询 + 检索内容)
    ↓
调用 LLM API 生成答案
    ↓
返回答案和引用来源
```

## 组件和接口

### 后端组件

#### 1. DocumentController
负责处理文档相关的 HTTP 请求。

**接口**:
```java
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    // 上传文档
    @PostMapping
    ResponseEntity<ApiResponse<DocumentVO>> uploadDocument(
        @RequestParam("file") MultipartFile file
    );
    
    // 获取文档列表
    @GetMapping
    ResponseEntity<ApiResponse<List<DocumentVO>>> listDocuments(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    );
    
    // 删除文档
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<Void>> deleteDocument(
        @PathVariable Long id
    );
    
    // 获取文档详情
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<DocumentDetailVO>> getDocument(
        @PathVariable Long id
    );
}
```

#### 2. QueryController
负责处理查询问答请求。

**接口**:
```java
@RestController
@RequestMapping("/api/query")
public class QueryController {
    
    // 提交查询
    @PostMapping
    ResponseEntity<ApiResponse<QueryResponseVO>> query(
        @RequestBody QueryRequest request
    );
    
    // 获取查询历史
    @GetMapping("/history")
    ResponseEntity<ApiResponse<List<QueryHistoryVO>>> getHistory(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    );
}
```

#### 3. DocumentService
负责文档管理的业务逻辑。

**接口**:
```java
public interface DocumentService {
    
    // 处理文档上传
    DocumentVO uploadDocument(MultipartFile file) throws IOException;
    
    // 获取文档列表
    PageResult<DocumentVO> listDocuments(int page, int size);
    
    // 删除文档
    void deleteDocument(Long id);
    
    // 获取文档详情
    DocumentDetailVO getDocument(Long id);
    
    // 处理文档（提取文本、分割、向量化）
    void processDocument(Long documentId) throws Exception;
}
```

#### 4. EmbeddingService
负责文本向量化。

**接口**:
```java
public interface EmbeddingService {
    
    // 向量化单个文本
    float[] embed(String text) throws Exception;
    
    // 批量向量化
    List<float[]> embedBatch(List<String> texts) throws Exception;
    
    // 获取向量维度
    int getDimension();
}
```

**OpenRouter 集成说明**:
- 使用 OpenRouter API 端点：`https://openrouter.ai/api/v1/embeddings`
- 请求格式与 OpenAI Embeddings API 兼容
- 模型选择：`openai/text-embedding-3-small` 或 `openai/text-embedding-3-large`
- 需要在请求头中添加 `Authorization: Bearer YOUR_API_KEY`

#### 5. VectorStoreService
负责向量存储和检索。

**接口**:
```java
public interface VectorStoreService {
    
    // 存储向量
    void storeVector(String id, float[] vector, Map<String, Object> metadata);
    
    // 批量存储向量
    void storeVectorBatch(List<VectorPoint> points);
    
    // 相似度搜索
    List<SearchResult> search(float[] queryVector, int topK);
    
    // 删除向量
    void deleteVector(String id);
    
    // 删除文档的所有向量
    void deleteByDocumentId(Long documentId);
}
```

#### 6. RetrievalService
负责检索相关文档片段。

**接口**:
```java
public interface RetrievalService {
    
    // 检索相关文档片段
    List<DocumentChunk> retrieve(String query, int topK) throws Exception;
    
    // 检索并过滤
    List<DocumentChunk> retrieveWithFilter(
        String query, 
        int topK, 
        Map<String, Object> filters
    ) throws Exception;
}
```

#### 7. LLMService
负责调用大语言模型生成答案。

**接口**:
```java
public interface LLMService {
    
    // 生成答案
    String generateAnswer(String query, List<DocumentChunk> context) throws Exception;
    
    // 构建提示词
    String buildPrompt(String query, List<DocumentChunk> context);
    
    // 支持指定模型
    String generateAnswer(String query, List<DocumentChunk> context, String model) throws Exception;
}
```

**OpenRouter 集成说明**:
- 使用 OpenRouter API 端点：`https://openrouter.ai/api/v1/chat/completions`
- 兼容 OpenAI API 格式，可以使用相同的请求/响应结构
- 支持多种模型选择（通过配置文件指定默认模型）
- 需要在请求头中添加 `HTTP-Referer` 和 `X-Title` 用于 OpenRouter 统计

#### 8. DocumentProcessor
负责文档处理（文本提取、分割）。

**接口**:
```java
public interface DocumentProcessor {
    
    // 提取文本内容
    String extractText(File file) throws Exception;
    
    // 分割文档
    List<String> splitDocument(String text, int chunkSize, int overlap);
    
    // 支持的文件类型
    boolean isSupported(String mimeType);
}
```

### 前端组件

#### 1. DocumentManagement.vue
文档管理页面组件。

**功能**:
- 文档上传（拖拽上传、点击上传）
- 文档列表展示（表格形式）
- 文档删除
- 上传进度显示
- 文档处理状态显示

#### 2. QueryInterface.vue
问答界面组件。

**功能**:
- 查询输入框
- 对话历史显示（聊天气泡样式）
- 答案展示
- 引用来源展示
- 加载状态显示

#### 3. DocumentUploader.vue
文档上传子组件。

**功能**:
- 文件选择
- 拖拽上传
- 上传进度条
- 文件格式验证

#### 4. ChatMessage.vue
聊天消息子组件。

**功能**:
- 消息展示（用户/系统）
- 引用来源展示
- 时间戳显示
- Markdown 渲染

## 数据模型

### 数据库初始化

#### PostgreSQL 初始化

**1. 创建数据库和用户**

```sql
-- 创建数据库
CREATE DATABASE rag_system;

-- 创建用户（可选，如果不使用默认用户）
CREATE USER rag_user WITH PASSWORD 'your_password';

-- 授权
GRANT ALL PRIVILEGES ON DATABASE rag_system TO rag_user;

-- 连接到数据库
\c rag_system

-- 授予 schema 权限
GRANT ALL ON SCHEMA public TO rag_user;
```

**2. 创建表结构**

```sql
-- 创建 Document 表
CREATE TABLE document (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    -- PROCESSING, COMPLETED, FAILED
    upload_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_time TIMESTAMP,
    error_message TEXT,
    chunk_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_document_status ON document(status);
CREATE INDEX idx_document_upload_time ON document(upload_time);

-- 添加注释
COMMENT ON TABLE document IS '文档元数据表';
COMMENT ON COLUMN document.status IS '文档处理状态：PROCESSING-处理中, COMPLETED-已完成, FAILED-失败';

-- 创建 DocumentChunk 表
CREATE TABLE document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    vector_id VARCHAR(100) NOT NULL,
    -- Qdrant 中的向量 ID
    char_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);

-- 创建索引
CREATE INDEX idx_chunk_document_id ON document_chunk(document_id);
CREATE INDEX idx_chunk_vector_id ON document_chunk(vector_id);

-- 添加注释
COMMENT ON TABLE document_chunk IS '文档片段表';
COMMENT ON COLUMN document_chunk.vector_id IS 'Qdrant 向量数据库中的向量 ID';

-- 创建 QueryHistory 表（可选）
CREATE TABLE query_history (
    id BIGSERIAL PRIMARY KEY,
    query_text TEXT NOT NULL,
    answer TEXT NOT NULL,
    retrieved_chunks JSONB,
    -- 存储检索到的片段信息
    query_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time_ms INTEGER
);

-- 创建索引
CREATE INDEX idx_query_time ON query_history(query_time);

-- 添加注释
COMMENT ON TABLE query_history IS '查询历史表';
COMMENT ON COLUMN query_history.retrieved_chunks IS '检索到的文档片段信息（JSON 格式）';
```

**3. 验证表创建**

```sql
-- 查看所有表
\dt

-- 查看表结构
\d document
\d document_chunk
\d query_history
```

#### Qdrant 初始化

Qdrant 不需要预先创建表结构，但需要创建 Collection。可以通过以下方式初始化：

**方式 1：使用 REST API（推荐在应用启动时自动创建）**

```bash
# 创建 Collection
curl -X PUT 'http://localhost:6333/collections/document_chunks' \
  -H 'Content-Type: application/json' \
  -d '{
    "vectors": {
      "size": 1536,
      "distance": "Cosine"
    }
  }'

# 验证 Collection 创建
curl 'http://localhost:6333/collections/document_chunks'
```

**方式 2：在 Java 代码中自动创建（推荐）**

应用启动时自动检查并创建 Collection：

```java
@Component
public class QdrantInitializer implements ApplicationRunner {
    
    @Autowired
    private QdrantClient qdrantClient;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        String collectionName = "document_chunks";
        
        // 检查 Collection 是否存在
        if (!qdrantClient.collectionExists(collectionName)) {
            // 创建 Collection
            qdrantClient.createCollection(collectionName, 1536, "Cosine");
            log.info("Created Qdrant collection: {}", collectionName);
        } else {
            log.info("Qdrant collection already exists: {}", collectionName);
        }
    }
}
```

**Qdrant Collection 配置说明**：
- **name**: `document_chunks` - Collection 名称
- **vectors.size**: `1536` - 向量维度（与 OpenAI text-embedding-3-small 匹配）
- **vectors.distance**: `Cosine` - 相似度计算方法（余弦相似度）

**验证 Qdrant 运行**：
```bash
# 检查 Qdrant 是否运行
curl http://localhost:6333/

# 查看所有 Collections
curl http://localhost:6333/collections
```

### 验证 Qdrant 运行**：
```bash
# 检查 Qdrant 是否运行
curl http://192.168.14.128:6333/

# 查看所有 Collections
curl http://192.168.14.128:6333/collections

# 访问 Web UI
# 浏览器打开：http://192.168.14.128:6333/dashboard
```

### Spring Boot 配置文件示例

**application.yml**:
```yaml
spring:
  application:
    name: rag-retrieval-system
  
  # 数据源配置
  datasource:
    url: jdbc:postgresql://192.168.14.128:5432/rag_system
    username: postgres
    password: root@Ubuntu123
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  
  # 文件上传配置
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.example.rag.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

# Qdrant 配置
qdrant:
  host: 192.168.14.128
  port: 6333
  grpc-port: 6334
  collection-name: document_chunks
  use-grpc: false  # 使用 REST API

# OpenRouter 配置
openrouter:
  api-key: ${OPENROUTER_API_KEY}  # 从环境变量读取
  base-url: https://openrouter.ai/api/v1
  embedding-model: openai/text-embedding-3-small
  chat-model: openai/gpt-4  # 可配置为其他模型
  timeout: 30000  # 30 秒超时
  max-retries: 3

# RAG 系统配置
rag:
  document:
    chunk-size: 800  # 文档片段大小（字符数）
    chunk-overlap: 150  # 片段重叠大小（字符数）
    supported-formats: pdf,txt,docx,md
    max-file-size: 52428800  # 50MB
    upload-dir: ./uploads  # 文件上传目录
  
  retrieval:
    top-k: 5  # 检索返回的文档片段数量
    min-score: 0.7  # 最小相似度分数阈值

# 日志配置
logging:
  level:
    root: INFO
    com.example.rag: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/rag-system.log
    max-size: 10MB
    max-history: 30

# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /
```

**application-dev.yml** (开发环境):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://192.168.14.128:5432/rag_system
    username: postgres
    password: root@Ubuntu123

qdrant:
  host: 192.168.14.128
  port: 6333

logging:
  level:
    com.example.rag: DEBUG
```

**application-prod.yml** (生产环境):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://192.168.14.128:5432/rag_system
    username: postgres
    password: ${DB_PASSWORD}  # 从环境变量读取

qdrant:
  host: 192.168.14.128
  port: 6333

openrouter:
  api-key: ${OPENROUTER_API_KEY}

logging:
  level:
    root: INFO
    com.example.rag: INFO
```

### 环境变量设置

**开发环境**:
```bash
# Linux/Mac
export OPENROUTER_API_KEY=your_api_key_here
export DB_PASSWORD=root@Ubuntu123

# Windows
set OPENROUTER_API_KEY=your_api_key_here
set DB_PASSWORD=root@Ubuntu123
```

**IDEA 运行配置**:
在 Run/Debug Configurations 中添加环境变量：
```
OPENROUTER_API_KEY=your_api_key_here
```

### 关系数据库模型 (PostgreSQL)

#### Document 表
存储文档元数据。

```sql
CREATE TABLE document (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    -- PROCESSING, COMPLETED, FAILED
    upload_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_time TIMESTAMP,
    error_message TEXT,
    chunk_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_status ON document(status);
CREATE INDEX idx_document_upload_time ON document(upload_time);
```

#### DocumentChunk 表
存储文档片段信息。

```sql
CREATE TABLE document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    vector_id VARCHAR(100) NOT NULL,
    -- Qdrant 中的向量 ID
    char_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);

CREATE INDEX idx_chunk_document_id ON document_chunk(document_id);
CREATE INDEX idx_chunk_vector_id ON document_chunk(vector_id);
```

#### QueryHistory 表
存储查询历史（可选）。

```sql
CREATE TABLE query_history (
    id BIGSERIAL PRIMARY KEY,
    query_text TEXT NOT NULL,
    answer TEXT NOT NULL,
    retrieved_chunks JSONB,
    -- 存储检索到的片段信息
    query_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time_ms INTEGER
);

CREATE INDEX idx_query_time ON query_history(query_time);
```

### 向量数据库模型 (Qdrant)

#### Collection: document_chunks

**配置**:
```json
{
  "name": "document_chunks",
  "vectors": {
    "size": 1536,
    "distance": "Cosine"
  }
}
```

**Payload 结构**:
```json
{
  "document_id": 123,
  "chunk_id": 456,
  "chunk_index": 0,
  "content": "文档片段的文本内容...",
  "document_name": "民法典.pdf",
  "char_count": 800
}
```

### Java 实体类

#### Document 实体
```java
@Data
@TableName("document")
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String filePath;
    private String status;
    private LocalDateTime uploadTime;
    private LocalDateTime processTime;
    private String errorMessage;
    private Integer chunkCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

#### DocumentChunk 实体
```java
@Data
@TableName("document_chunk")
public class DocumentChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private String vectorId;
    private Integer charCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

### VO (View Object) 类

#### DocumentVO
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVO {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String status;
    private LocalDateTime uploadTime;
    private Integer chunkCount;
}
```

#### QueryResponseVO
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponseVO {
    private String query;
    private String answer;
    private List<ChunkReference> references;
    private Long responseTimeMs;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkReference {
    private Long documentId;
    private String documentName;
    private String content;
    private Float score;
}
```

### 使用 Hutool 的工具类示例

#### 文件工具类
```java
@Component
@Slf4j
public class FileUtils {
    
    /**
     * 保存上传的文件
     */
    public String saveUploadFile(MultipartFile file, String uploadDir) throws IOException {
        // 使用 Hutool 的 FileUtil
        String fileName = IdUtil.simpleUUID() + "_" + file.getOriginalFilename();
        String filePath = FileUtil.normalize(uploadDir + File.separator + fileName);
        
        // 确保目录存在
        FileUtil.mkdir(uploadDir);
        
        // 保存文件
        file.transferTo(new File(filePath));
        
        log.info("File saved: {}", filePath);
        return filePath;
    }
    
    /**
     * 获取文件 MIME 类型
     */
    public String getMimeType(File file) {
        return FileTypeUtil.getType(file);
    }
    
    /**
     * 删除文件
     */
    public boolean deleteFile(String filePath) {
        return FileUtil.del(filePath);
    }
}
```

#### 字符串工具类
```java
@Component
public class TextUtils {
    
    /**
     * 分割文本为片段
     */
    public List<String> splitText(String text, int chunkSize, int overlap) {
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        
        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += (chunkSize - overlap);
        }
        
        return chunks;
    }
    
    /**
     * 清理文本
     */
    public String cleanText(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        
        // 移除多余空白
        return StrUtil.cleanBlank(text);
    }
}
```

#### JSON 工具类
```java
@Component
public class JsonUtils {
    
    /**
     * 对象转 JSON
     */
    public String toJson(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }
    
    /**
     * JSON 转对象
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        return JSONUtil.toBean(json, clazz);
    }
    
    /**
     * JSON 转 List
     */
    public <T> List<T> fromJsonArray(String json, Class<T> clazz) {
        return JSONUtil.toList(json, clazz);
    }
}
```

## API 接口设计

### 统一响应格式

```java
@Data
public class ApiResponse<T> {
    private Integer code;      // 状态码：200 成功，400 客户端错误，500 服务器错误
    private String message;    // 消息
    private T data;           // 数据
    private Long timestamp;   // 时间戳
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}
```

### 文档管理 API

#### 1. 上传文档
```
POST /api/documents
Content-Type: multipart/form-data

Request:
- file: MultipartFile (文档文件)

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fileName": "民法典.pdf",
    "fileSize": 1048576,
    "fileType": "application/pdf",
    "status": "PROCESSING",
    "uploadTime": "2024-01-15T10:30:00",
    "chunkCount": 0
  },
  "timestamp": 1705287000000
}
```

#### 2. 获取文档列表
```
GET /api/documents?page=1&size=10

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 25,
    "page": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "fileName": "民法典.pdf",
        "fileSize": 1048576,
        "fileType": "application/pdf",
        "status": "COMPLETED",
        "uploadTime": "2024-01-15T10:30:00",
        "chunkCount": 150
      }
    ]
  },
  "timestamp": 1705287000000
}
```

#### 3. 删除文档
```
DELETE /api/documents/{id}

Response:
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1705287000000
}
```

#### 4. 获取文档详情
```
GET /api/documents/{id}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fileName": "民法典.pdf",
    "fileSize": 1048576,
    "fileType": "application/pdf",
    "status": "COMPLETED",
    "uploadTime": "2024-01-15T10:30:00",
    "processTime": "2024-01-15T10:32:00",
    "chunkCount": 150,
    "chunks": [
      {
        "id": 1,
        "chunkIndex": 0,
        "content": "第一条 为了保护民事主体的合法权益...",
        "charCount": 800
      }
    ]
  },
  "timestamp": 1705287000000
}
```

### 查询问答 API

#### 1. 提交查询
```
POST /api/query
Content-Type: application/json

Request:
{
  "query": "什么是民事权利能力？",
  "topK": 5
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "query": "什么是民事权利能力？",
    "answer": "民事权利能力是指民事主体依法享有民事权利和承担民事义务的资格。根据《民法典》第十三条规定，自然人从出生时起到死亡时止，具有民事权利能力，依法享有民事权利，承担民事义务。",
    "references": [
      {
        "documentId": 1,
        "documentName": "民法典.pdf",
        "content": "第十三条 自然人从出生时起到死亡时止，具有民事权利能力，依法享有民事权利，承担民事义务。",
        "score": 0.92
      }
    ],
    "responseTimeMs": 1500
  },
  "timestamp": 1705287000000
}
```

#### 2. 获取查询历史
```
GET /api/query/history?page=1&size=20

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 1,
        "queryText": "什么是民事权利能力？",
        "answer": "民事权利能力是指...",
        "queryTime": "2024-01-15T10:35:00",
        "responseTimeMs": 1500
      }
    ]
  },
  "timestamp": 1705287000000
}
```

### 错误响应示例

```json
{
  "code": 400,
  "message": "不支持的文件格式",
  "data": null,
  "timestamp": 1705287000000
}
```

```json
{
  "code": 500,
  "message": "向量化服务调用失败",
  "data": null,
  "timestamp": 1705287000000
}
```

## 前端组件设计

### 页面结构

```
App.vue
├── Layout.vue
    ├── Header.vue (顶部导航)
    ├── Sidebar.vue (侧边栏菜单)
    └── Main.vue (主内容区)
        ├── DocumentManagement.vue (文档管理页)
        │   ├── DocumentUploader.vue (上传组件)
        │   └── DocumentList.vue (文档列表)
        └── QueryInterface.vue (问答页)
            ├── QueryInput.vue (查询输入)
            ├── ChatHistory.vue (对话历史)
            └── ChatMessage.vue (消息组件)
```

### 状态管理 (Pinia)

#### documentStore
```typescript
export const useDocumentStore = defineStore('document', {
  state: () => ({
    documents: [] as Document[],
    loading: false,
    currentPage: 1,
    pageSize: 10,
    total: 0
  }),
  
  actions: {
    async uploadDocument(file: File) {
      // 上传文档逻辑
    },
    
    async fetchDocuments(page: number, size: number) {
      // 获取文档列表
    },
    
    async deleteDocument(id: number) {
      // 删除文档
    }
  }
});
```

#### queryStore
```typescript
export const useQueryStore = defineStore('query', {
  state: () => ({
    messages: [] as ChatMessage[],
    loading: false,
    currentQuery: ''
  }),
  
  actions: {
    async submitQuery(query: string) {
      // 提交查询
    },
    
    async fetchHistory() {
      // 获取历史记录
    },
    
    clearHistory() {
      this.messages = [];
    }
  }
});
```

### 路由配置

```typescript
const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/query',
    children: [
      {
        path: '/documents',
        name: 'DocumentManagement',
        component: () => import('@/views/DocumentManagement.vue'),
        meta: { title: '文档管理' }
      },
      {
        path: '/query',
        name: 'QueryInterface',
        component: () => import('@/views/QueryInterface.vue'),
        meta: { title: '智能问答' }
      }
    ]
  }
];
```

### HTTP 请求封装

```typescript
// api/document.ts
export const documentApi = {
  upload: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return axios.post('/api/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  
  list: (page: number, size: number) => {
    return axios.get('/api/documents', { params: { page, size } });
  },
  
  delete: (id: number) => {
    return axios.delete(`/api/documents/${id}`);
  },
  
  getDetail: (id: number) => {
    return axios.get(`/api/documents/${id}`);
  }
};

// api/query.ts
export const queryApi = {
  submit: (query: string, topK: number = 5) => {
    return axios.post('/api/query', { query, topK });
  },
  
  getHistory: (page: number, size: number) => {
    return axios.get('/api/query/history', { params: { page, size } });
  }
};
```


## 正确性属性

属性是一个特征或行为，应该在系统的所有有效执行中保持为真——本质上是关于系统应该做什么的形式化陈述。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。

### 属性反思

在分析了所有验收标准后，我识别出以下可以合并或简化的冗余属性：

1. **文档上传和元数据存储**：需求 1.1（上传成功）和 1.3（存储元数据）可以合并为一个属性，因为成功上传必然包含元数据存储
2. **向量化和存储**：需求 3.1（向量化）和 3.4（存储向量）可以合并，因为向量化的目的就是存储
3. **查询处理流程**：需求 4.1（查询向量化）和 4.2（执行搜索）是查询处理的连续步骤，可以合并为一个端到端属性
4. **LLM 服务流程**：需求 5.1（构建提示词）和 5.2（调用 API）可以合并为一个完整的生成流程属性

经过反思，我将重点关注以下核心属性，避免冗余：

### 文档管理属性

**属性 1：支持格式文档上传成功**
*对于任何*支持的文档格式（PDF、TXT、DOCX、MD），上传该格式的文档应该返回成功状态，并在数据库中创建包含完整元数据（文件名、大小、上传时间、文档 ID）的记录。
**验证需求：1.1, 1.3**

**属性 2：不支持格式文档被拒绝**
*对于任何*不支持的文档格式，上传尝试应该被拒绝并返回明确的错误信息。
**验证需求：1.2**

**属性 3：文档列表完整性**
*对于任何*已上传的文档集合，查询文档列表应该返回所有文档的元数据，且返回的文档数量等于实际上传的文档数量。
**验证需求：1.4**

**属性 4：文档删除完整性**
*对于任何*已上传的文档，删除操作后，该文档的元数据应该从关系数据库中移除，且所有相关的向量应该从向量数据库中移除。
**验证需求：1.5**

### 文档处理属性

**属性 5：文档分割大小约束**
*对于任何*文本内容，分割后的每个文档片段的字符数应该在 500-1000 字符范围内（最后一个片段除外）。
**验证需求：2.2**

**属性 6：文档片段重叠**
*对于任何*分割后的相邻文档片段，后一个片段的开头应该与前一个片段的结尾有 100-200 字符的重叠。
**验证需求：2.3**

**属性 7：片段标识唯一性**
*对于任何*文档的所有片段，每个片段应该有唯一的标识符，且所有片段都应该正确关联到原始文档 ID。
**验证需求：2.4**

### 向量化属性

**属性 8：向量维度一致性**
*对于任何*文本片段，向量化后生成的向量维度应该是固定的（例如 1536 维）。
**验证需求：3.3**

**属性 9：向量存储往返一致性**
*对于任何*文档片段，向量化并存储到向量数据库后，应该能够通过向量 ID 检索到相同的片段内容和元数据。
**验证需求：3.4**

### 检索属性

**属性 10：检索结果数量限制**
*对于任何*查询，检索返回的文档片段数量应该不超过配置的 K 值（默认为 5）。
**验证需求：4.3**

**属性 11：检索结果格式完整性**
*对于任何*检索返回的文档片段，每个结果都应该包含相似度分数和原始文本内容。
**验证需求：4.4**

### 生成属性

**属性 12：答案响应格式完整性**
*对于任何*成功的查询请求，返回的响应应该同时包含生成的答案和引用的文档片段列表。
**验证需求：5.3**

### API 接口属性

**属性 13：API 响应格式统一性**
*对于任何*API 请求，无论成功或失败，响应都应该包含统一的格式：状态码（code）、消息（message）、数据（data）和时间戳（timestamp）字段。
**验证需求：7.5**

**属性 14：API 错误响应正确性**
*对于任何*导致错误的 API 请求，响应应该包含适当的 HTTP 状态码（4xx 或 5xx）和错误详情。
**验证需求：7.6**

### 异步处理属性

**属性 15：文档上传异步响应**
*对于任何*文档上传请求，系统应该立即返回上传成功状态（状态为 PROCESSING），而不等待文档处理（分割、向量化）完成。
**验证需求：10.2**

### 日志记录属性

**属性 16：错误日志完整性**
*对于任何*系统错误，应该记录包含时间戳、错误类型、错误消息和堆栈跟踪的完整日志。
**验证需求：9.1**

**属性 17：用户错误消息友好性**
*对于任何*用户操作导致的错误，返回给用户的错误消息应该是友好的描述，不应该包含技术细节（如堆栈跟踪、内部类名）。
**验证需求：9.3**

**属性 18：关键操作审计日志**
*对于任何*关键操作（文档上传、删除、查询请求），系统应该记录审计日志，包含操作类型、时间戳和相关参数。
**验证需求：9.4**


## 错误处理

### 错误分类

系统错误分为以下几类：

1. **客户端错误（4xx）**
   - 400 Bad Request：请求参数错误、文件格式不支持
   - 404 Not Found：文档不存在
   - 413 Payload Too Large：文件过大

2. **服务器错误（5xx）**
   - 500 Internal Server Error：系统内部错误
   - 502 Bad Gateway：外部 API 调用失败
   - 503 Service Unavailable：服务暂时不可用

### 错误处理策略

#### 文档上传错误
- **文件格式不支持**：返回 400 错误，提示支持的格式列表
- **文件过大**：返回 413 错误，提示最大文件大小限制
- **文件解析失败**：返回 500 错误，记录详细日志，更新文档状态为 FAILED

#### 向量化服务错误
- **API 调用失败**：实现重试机制（最多 3 次，指数退避）
- **API 配额超限**：返回 503 错误，提示稍后重试
- **网络超时**：重试后仍失败则记录错误，通知用户

#### 数据库错误
- **连接失败**：使用连接池自动重连
- **查询超时**：记录慢查询日志，返回 500 错误
- **数据完整性错误**：回滚事务，返回 400 错误

#### LLM 服务错误
- **API 调用失败**：返回 502 错误，提示稍后重试
- **响应超时**：设置合理的超时时间（30 秒），超时后返回错误
- **内容过滤**：返回友好提示，说明内容不符合使用政策

### 全局异常处理

使用 Spring Boot 的 `@ControllerAdvice` 实现全局异常处理：

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
            .status(e.getHttpStatus())
            .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, "系统错误，请稍后重试"));
    }
}
```

### 重试机制

对于外部 API 调用，实现指数退避重试：

```java
@Retryable(
    value = {ApiException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public float[] callEmbeddingApi(String text) throws ApiException {
    // API 调用逻辑
}
```

### 日志记录

- **DEBUG**：详细的调试信息（方法参数、中间结果）
- **INFO**：关键操作日志（文档上传、查询请求）
- **WARN**：警告信息（重试、降级）
- **ERROR**：错误信息（异常堆栈、失败原因）

日志格式：
```
[时间戳] [级别] [线程] [类名] - [消息]
2024-01-15 10:30:00.123 INFO [http-nio-8080-exec-1] DocumentService - Document uploaded: id=1, name=民法典.pdf
```

## 测试策略

### 测试方法

系统采用双重测试方法：

1. **单元测试**：验证特定示例、边缘情况和错误条件
2. **属性测试**：验证所有输入的通用属性

两种测试方法是互补的，都是全面覆盖所必需的：
- 单元测试捕获具体的错误
- 属性测试验证一般正确性

### 单元测试

使用 JUnit 5 和 Mockito 进行单元测试。

**测试重点**：
- 特定示例：验证已知输入的预期输出
- 边缘情况：空文件、超大文件、特殊字符
- 错误条件：无效输入、API 失败、数据库错误
- 集成点：组件之间的交互

**示例**：
```java
@Test
void testUploadDocument_Success() {
    // 测试成功上传 PDF 文件
    MultipartFile file = createMockPdfFile();
    DocumentVO result = documentService.uploadDocument(file);
    
    assertNotNull(result.getId());
    assertEquals("test.pdf", result.getFileName());
    assertEquals("PROCESSING", result.getStatus());
}

@Test
void testUploadDocument_UnsupportedFormat() {
    // 测试上传不支持的格式
    MultipartFile file = createMockFile("test.exe");
    
    assertThrows(BusinessException.class, () -> {
        documentService.uploadDocument(file);
    });
}
```

### 属性测试

使用 jqwik 进行基于属性的测试。

**配置**：
- 每个属性测试最少运行 100 次迭代
- 每个测试必须引用设计文档中的属性
- 标签格式：`@Tag("Feature: rag-retrieval-system, Property N: 属性文本")`

**示例**：
```java
@Property
@Tag("Feature: rag-retrieval-system, Property 5: 文档分割大小约束")
void documentChunkSizeConstraint(@ForAll("texts") String text) {
    List<String> chunks = documentProcessor.splitDocument(text, 800, 150);
    
    // 验证每个片段（除最后一个）的大小在 500-1000 范围内
    for (int i = 0; i < chunks.size() - 1; i++) {
        int size = chunks.get(i).length();
        assertTrue(size >= 500 && size <= 1000);
    }
}

@Property
@Tag("Feature: rag-retrieval-system, Property 8: 向量维度一致性")
void vectorDimensionConsistency(@ForAll String text) {
    float[] vector = embeddingService.embed(text);
    
    // 验证向量维度固定为 1536
    assertEquals(1536, vector.length);
}

@Property
@Tag("Feature: rag-retrieval-system, Property 13: API 响应格式统一性")
void apiResponseFormatUniformity(@ForAll("apiRequests") ApiRequest request) {
    ApiResponse<?> response = executeApiRequest(request);
    
    // 验证响应包含所有必需字段
    assertNotNull(response.getCode());
    assertNotNull(response.getMessage());
    assertNotNull(response.getTimestamp());
}
```

### 前端测试

使用 Vitest 和 Vue Test Utils 进行前端测试。

**测试重点**：
- 组件渲染：验证组件正确渲染
- 用户交互：验证按钮点击、表单提交
- API 集成：使用 Mock 验证 API 调用
- 状态管理：验证 Pinia store 的状态变化

**示例**：
```typescript
describe('DocumentUploader', () => {
  it('should upload file successfully', async () => {
    const wrapper = mount(DocumentUploader);
    const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
    
    await wrapper.vm.handleFileUpload(file);
    
    expect(wrapper.emitted('upload-success')).toBeTruthy();
  });
  
  it('should reject unsupported file format', async () => {
    const wrapper = mount(DocumentUploader);
    const file = new File(['content'], 'test.exe', { type: 'application/exe' });
    
    await wrapper.vm.handleFileUpload(file);
    
    expect(wrapper.emitted('upload-error')).toBeTruthy();
  });
});
```

### 集成测试

使用 Spring Boot Test 进行集成测试。

**测试重点**：
- API 端到端测试
- 数据库集成测试（使用 Testcontainers）
- 外部服务集成测试（使用 Mock Server）

**示例**：
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class DocumentApiIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Test
    void testDocumentUploadFlow() {
        // 测试完整的文档上传流程
        // 1. 上传文档
        // 2. 验证数据库记录
        // 3. 验证文档状态变化
    }
}
```

### 测试覆盖率目标

- **单元测试覆盖率**：≥ 80%
- **属性测试**：覆盖所有核心属性
- **集成测试**：覆盖所有 API 端点
- **前端测试覆盖率**：≥ 70%

### 持续集成

在 CI/CD 流程中自动运行所有测试：
1. 代码提交触发测试
2. 单元测试和属性测试并行运行
3. 集成测试在单元测试通过后运行
4. 生成测试报告和覆盖率报告
5. 测试失败则阻止合并

