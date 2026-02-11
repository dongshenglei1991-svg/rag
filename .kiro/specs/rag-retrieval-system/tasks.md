# 实现计划：RAG 检索系统

## 概述

本实现计划将 RAG 检索系统的设计转换为可执行的开发任务。系统采用前后端分离架构，后端使用 Spring Boot 3.x + MyBatis-Plus + PostgreSQL + Qdrant，前端使用 Vue3 + Element Plus。

实现顺序遵循从核心功能到外围功能的原则，每个任务都包含具体的实现步骤和需求引用。

## 任务列表

### 第一阶段：项目初始化和基础设施

- [x] 1. 创建 Spring Boot 后端项目结构
  - 使用 Spring Initializr 创建项目（Spring Boot 3.2.x）
  - 添加依赖：Spring Web、MyBatis-Plus、PostgreSQL Driver、Lombok、Hutool
  - 配置 pom.xml，添加 Apache Tika、WebClient、Qdrant 客户端依赖
  - 创建基础包结构：controller、service、mapper、entity、vo、config、util
  - _需求：8.1, 8.2, 8.5_

- [x] 2. 配置数据库连接和 MyBatis-Plus
  - 创建 application.yml 配置文件，配置 PostgreSQL 连接（192.168.14.128:5432）
  - 配置 MyBatis-Plus（mapper 位置、驼峰命名转换）
  - 创建 MyBatis-Plus 配置类（分页插件、自动填充）
  - 测试数据库连接
  - _需求：8.5_

- [x] 3. 创建实体类和 Mapper
  - 创建 Document 实体类（使用 Lombok @Data 注解）
  - 创建 DocumentChunk 实体类
  - 创建 QueryHistory 实体类（可选）
  - 创建对应的 Mapper 接口（继承 BaseMapper）
  - _需求：1.3, 2.4_

- [x] 4. 创建统一响应格式和异常处理
  - 创建 ApiResponse 通用响应类
  - 创建自定义业务异常类 BusinessException
  - 创建全局异常处理器 GlobalExceptionHandler（@ControllerAdvice）
  - 实现统一的错误响应格式
  - _需求：7.5, 7.6, 9.3_

- [x] 5. 配置 Qdrant 客户端
  - 创建 Qdrant 配置类，读取连接信息（192.168.14.128:6333）
  - 创建 Qdrant 客户端 Bean（使用 REST API）
  - 实现应用启动时自动创建 Collection（document_chunks，维度 1536，余弦相似度）
  - 测试 Qdrant 连接
  - _需求：3.4, 8.5_

- [x] 6. 配置 OpenRouter 客户端
  - 创建 OpenRouter 配置类，读取 API Key 和端点
  - 创建 WebClient Bean 用于调用 OpenRouter API
  - 配置请求头（Authorization、HTTP-Referer、X-Title）
  - 配置超时和重试策略
  - _需求：3.1, 5.2, 8.1, 8.2_

- [x] 7. 检查点 - 确保基础设施正常
  - 确保所有配置正确，数据库和 Qdrant 连接成功
  - 确保所有测试通过，如有问题请询问用户

### 第二阶段：文档管理功能

- [ ] 8. 实现文档上传功能
  - [x] 8.1 创建 DocumentService 接口和实现类
    - 实现 uploadDocument 方法（接收 MultipartFile）
    - 验证文件格式（PDF、TXT、DOCX、MD）
    - 验证文件大小（最大 50MB）
    - 使用 Hutool FileUtil 保存文件到本地
    - 创建 Document 记录，状态设为 PROCESSING
    - 返回 DocumentVO
    - _需求：1.1, 1.2, 1.3_
  
  - [ ]* 8.2 编写文档上传的属性测试
    - **属性 1：支持格式文档上传成功**
    - **验证需求：1.1, 1.3**
  
  - [ ]* 8.3 编写文档上传的属性测试
    - **属性 2：不支持格式文档被拒绝**
    - **验证需求：1.2**
  
  - [ ]* 8.4 编写文档上传的单元测试
    - 测试成功上传 PDF 文件
    - 测试上传不支持格式被拒绝
    - 测试文件过大被拒绝
    - _需求：1.1, 1.2_

- [ ] 9. 实现文档列表查询功能
  - [x] 9.1 实现 listDocuments 方法
    - 使用 MyBatis-Plus 分页查询
    - 按上传时间倒序排列
    - 返回 PageResult<DocumentVO>
    - _需求：1.4_
  
  - [ ]* 9.2 编写文档列表的属性测试
    - **属性 3：文档列表完整性**
    - **验证需求：1.4**
  
  - [ ]* 9.3 编写文档列表的单元测试
    - 测试分页查询
    - 测试空列表情况
    - _需求：1.4_

- [ ] 10. 实现文档删除功能
  - [x] 10.1 实现 deleteDocument 方法
    - 查询文档是否存在
    - 删除文件系统中的文件（使用 Hutool FileUtil）
    - 删除 Qdrant 中的向量（通过 document_id 过滤）
    - 删除数据库中的 Document 记录（级联删除 DocumentChunk）
    - _需求：1.5_
  
  - [ ]* 10.2 编写文档删除的属性测试
    - **属性 4：文档删除完整性**
    - **验证需求：1.5**
  
  - [ ]* 10.3 编写文档删除的单元测试
    - 测试成功删除文档
    - 测试删除不存在的文档
    - _需求：1.5_

- [x] 11. 实现 DocumentController
  - 创建 DocumentController 类（@RestController）
  - 实现 POST /api/documents（上传文档）
  - 实现 GET /api/documents（获取文档列表）
  - 实现 DELETE /api/documents/{id}（删除文档）
  - 实现 GET /api/documents/{id}（获取文档详情）
  - 添加参数验证和错误处理
  - _需求：7.1, 7.2, 7.3_

- [x] 12. 检查点 - 确保文档管理功能正常
  - 测试文档上传、列表查询、删除功能
  - 确保所有测试通过，如有问题请询问用户

### 第三阶段：文档处理和向量化

- [ ] 13. 实现文档文本提取功能
  - [x] 13.1 创建 DocumentProcessor 接口和实现类
    - 使用 Apache Tika 提取文本内容
    - 支持 PDF、TXT、DOCX、MD 格式
    - 实现 extractText 方法
    - 处理提取失败的情况
    - _需求：2.1_
  
  - [ ]* 13.2 编写文本提取的单元测试
    - 测试提取 PDF 文本
    - 测试提取 TXT 文本
    - 测试提取失败情况
    - _需求：2.1_

- [ ] 14. 实现文档分割功能
  - [x] 14.1 实现 splitDocument 方法
    - 使用 Hutool StrUtil 处理文本
    - 按配置的 chunk-size（800 字符）分割
    - 保留 chunk-overlap（150 字符）重叠
    - 返回文档片段列表
    - _需求：2.2, 2.3_
  
  - [ ]* 14.2 编写文档分割的属性测试
    - **属性 5：文档分割大小约束**
    - **验证需求：2.2**
  
  - [ ]* 14.3 编写文档分割的属性测试
    - **属性 6：文档片段重叠**
    - **验证需求：2.3**
  
  - [ ]* 14.4 编写文档分割的单元测试
    - 测试正常分割
    - 测试短文本分割
    - 测试空文本处理
    - _需求：2.2, 2.3_

- [ ] 15. 实现向量化服务
  - [x] 15.1 创建 EmbeddingService 接口和实现类
    - 实现 embed 方法（调用 OpenRouter Embeddings API）
    - 使用 WebClient 发送请求到 https://openrouter.ai/api/v1/embeddings
    - 模型使用 openai/text-embedding-3-small
    - 实现重试机制（最多 3 次，指数退避）
    - 实现 embedBatch 方法（批量向量化）
    - _需求：3.1, 3.2, 3.3_
  
  - [ ]* 15.2 编写向量化的属性测试
    - **属性 8：向量维度一致性**
    - **验证需求：3.3**
  
  - [ ]* 15.3 编写向量化的单元测试
    - 测试单个文本向量化
    - 测试批量向量化
    - 测试 API 调用失败重试
    - _需求：3.1, 3.2, 3.3_

- [ ] 16. 实现向量存储服务
  - [x] 16.1 创建 VectorStoreService 接口和实现类
    - 实现 storeVector 方法（存储单个向量到 Qdrant）
    - 实现 storeVectorBatch 方法（批量存储）
    - 实现 deleteByDocumentId 方法（删除文档的所有向量）
    - Payload 包含：document_id、chunk_id、chunk_index、content、document_name
    - _需求：3.4_
  
  - [ ]* 16.2 编写向量存储的属性测试
    - **属性 9：向量存储往返一致性**
    - **验证需求：3.4**
  
  - [ ]* 16.3 编写向量存储的单元测试
    - 测试存储向量
    - 测试批量存储
    - 测试删除向量
    - _需求：3.4_

- [ ] 17. 实现异步文档处理流程
  - [x] 17.1 创建 DocumentProcessingService
    - 使用 @Async 注解实现异步处理
    - 实现 processDocument 方法
    - 流程：提取文本 → 分割片段 → 向量化 → 存储向量 → 更新文档状态
    - 为每个片段分配唯一 ID 并关联到文档
    - 处理成功后更新文档状态为 COMPLETED
    - 处理失败后更新状态为 FAILED，记录错误信息
    - _需求：2.1, 2.2, 2.3, 2.4, 3.1, 3.4, 10.2_
  
  - [ ]* 17.2 编写文档处理的属性测试
    - **属性 7：片段标识唯一性**
    - **验证需求：2.4**
  
  - [ ]* 17.3 编写文档处理的属性测试
    - **属性 15：文档上传异步响应**
    - **验证需求：10.2**
  
  - [ ]* 17.4 编写文档处理的集成测试
    - 测试完整的文档处理流程
    - 测试处理失败情况
    - _需求：2.1, 2.2, 2.3, 2.4, 3.1, 3.4_

- [x] 18. 检查点 - 确保文档处理功能正常
  - 上传文档并验证异步处理
  - 检查数据库中的片段记录
  - 检查 Qdrant 中的向量
  - 确保所有测试通过，如有问题请询问用户

### 第四阶段：检索和生成功能

- [ ] 19. 实现检索服务
  - [x] 19.1 创建 RetrievalService 接口和实现类
    - 实现 retrieve 方法
    - 调用 EmbeddingService 向量化查询
    - 在 Qdrant 中执行相似度搜索（使用配置的 top-k，默认 5）
    - 根据 vector_id 查询 DocumentChunk 获取完整信息
    - 返回 List<DocumentChunk> 和相似度分数
    - _需求：4.1, 4.2, 4.3, 4.4_
  
  - [ ]* 19.2 编写检索的属性测试
    - **属性 10：检索结果数量限制**
    - **验证需求：4.3**
  
  - [ ]* 19.3 编写检索的属性测试
    - **属性 11：检索结果格式完整性**
    - **验证需求：4.4**
  
  - [ ]* 19.4 编写检索的单元测试
    - 测试正常检索
    - 测试空结果情况
    - 测试 top-k 限制
    - _需求：4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 20. 实现 LLM 服务
  - [x] 20.1 创建 LLMService 接口和实现类
    - 实现 buildPrompt 方法（构建提示词）
    - 提示词格式：系统提示 + 检索内容 + 用户查询
    - 实现 generateAnswer 方法（调用 OpenRouter Chat API）
    - 使用 WebClient 发送请求到 https://openrouter.ai/api/v1/chat/completions
    - 模型使用配置的 chat-model（默认 openai/gpt-4）
    - 处理 API 调用失败情况
    - _需求：5.1, 5.2, 5.4_
  
  - [ ]* 20.2 编写 LLM 服务的单元测试
    - 测试提示词构建
    - 测试答案生成
    - 测试 API 调用失败
    - _需求：5.1, 5.2, 5.4_

- [ ] 21. 实现查询服务
  - [x] 21.1 创建 QueryService
    - 实现 query 方法
    - 流程：检索相关片段 → 构建提示词 → 生成答案
    - 记录查询历史到数据库（可选）
    - 返回 QueryResponseVO（包含答案和引用）
    - 记录响应时间
    - _需求：4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3_
  
  - [ ]* 21.2 编写查询服务的属性测试
    - **属性 12：答案响应格式完整性**
    - **验证需求：5.3**
  
  - [ ]* 21.3 编写查询服务的集成测试
    - 测试完整的查询流程
    - 测试无相关文档情况
    - _需求：4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3_

- [x] 22. 实现 QueryController
  - 创建 QueryController 类（@RestController）
  - 实现 POST /api/query（提交查询）
  - 实现 GET /api/query/history（获取查询历史）
  - 添加参数验证和错误处理
  - _需求：7.4_

- [x] 23. 检查点 - 确保检索和生成功能正常
  - 测试完整的问答流程
  - 验证答案质量和引用来源
  - 确保所有测试通过，如有问题请询问用户

### 第五阶段：日志和错误处理

- [ ] 24. 完善日志记录
  - [x] 24.1 配置 Logback
    - 配置日志格式和输出位置
    - 配置日志级别（开发环境 DEBUG，生产环境 INFO）
    - 配置日志文件滚动策略
    - _需求：9.5_
  
  - [x] 24.2 添加关键操作日志
    - 在文档上传、删除、查询等关键操作添加日志
    - 使用 SLF4J Logger（手动 LoggerFactory.getLogger()）
    - 记录操作参数和结果
    - _需求：9.4_
  
  - [ ]* 24.3 编写日志记录的属性测试
    - **属性 16：错误日志完整性**
    - **验证需求：9.1**
  
  - [ ]* 24.4 编写日志记录的属性测试
    - **属性 18：关键操作审计日志**
    - **验证需求：9.4**

- [ ] 25. 完善错误处理
  - [x] 25.1 实现详细的错误日志
    - 在 GlobalExceptionHandler 中记录详细错误信息
    - 包含时间戳、错误类型、错误消息、堆栈跟踪
    - 记录外部 API 调用失败的请求和响应
    - _需求：9.1, 9.2_
  
  - [x] 25.2 实现用户友好的错误消息
    - 确保返回给用户的错误消息不包含技术细节
    - 为常见错误提供友好提示
    - _需求：9.3_
  
  - [ ]* 25.3 编写错误处理的属性测试
    - **属性 14：API 错误响应正确性**
    - **验证需求：7.6**
  
  - [ ]* 25.4 编写错误处理的属性测试
    - **属性 17：用户错误消息友好性**
    - **验证需求：9.3**

- [ ] 26. 实现 API 响应格式统一性测试
  - [ ]* 26.1 编写 API 响应的属性测试
    - **属性 13：API 响应格式统一性**
    - **验证需求：7.5**

### 第六阶段：前端开发

- [x] 27. 创建 Vue3 前端项目
  - 使用 Vite 创建 Vue3 项目
  - 安装依赖：Vue Router、Pinia、Axios、Element Plus
  - 配置 Vite（代理、环境变量）
  - 创建基础目录结构：views、components、stores、api、utils
  - _需求：6.1_

- [x] 28. 配置 Axios 和 API 封装
  - 创建 Axios 实例，配置 baseURL（http://localhost:8080）
  - 配置请求拦截器（添加 token 等）
  - 配置响应拦截器（统一错误处理）
  - 封装 documentApi（upload、list、delete、getDetail）
  - 封装 queryApi（submit、getHistory）
  - _需求：7.1, 7.2, 7.3, 7.4_

- [x] 29. 创建 Pinia Stores
  - 创建 documentStore（管理文档状态）
  - 创建 queryStore（管理查询状态）
  - 实现状态管理和 actions
  - _需求：6.2, 6.3_

- [ ] 30. 实现文档管理页面
  - [x] 30.1 创建 DocumentManagement.vue
    - 实现文档列表展示（使用 el-table）
    - 实现分页功能（使用 el-pagination）
    - 实现文档删除功能（使用 el-popconfirm）
    - 显示文档状态（PROCESSING、COMPLETED、FAILED）
    - _需求：6.2_
  
  - [x] 30.2 创建 DocumentUploader.vue
    - 实现文件上传组件（使用 el-upload）
    - 支持拖拽上传
    - 显示上传进度
    - 文件格式验证（PDF、TXT、DOCX、MD）
    - 文件大小验证（最大 50MB）
    - _需求：6.2_
  
  - [ ]* 30.3 编写文档管理页面的单元测试
    - 测试组件渲染
    - 测试文件上传
    - 测试文档删除
    - _需求：6.2_

- [ ] 31. 实现问答页面
  - [x] 31.1 创建 QueryInterface.vue
    - 实现查询输入框（使用 el-input）
    - 实现对话历史显示（聊天气泡样式）
    - 实现加载状态显示（使用 el-loading）
    - 实现答案和引用来源展示
    - _需求：6.3, 6.4, 6.5_
  
  - [x] 31.2 创建 ChatMessage.vue
    - 实现消息组件（用户消息和系统消息）
    - 显示时间戳
    - 显示引用来源（可折叠）
    - 支持 Markdown 渲染（可选）
    - _需求：6.5_
  
  - [ ]* 31.3 编写问答页面的单元测试
    - 测试组件渲染
    - 测试查询提交
    - 测试消息显示
    - _需求：6.3, 6.4, 6.5_

- [x] 32. 实现路由和布局
  - 创建 Layout.vue（包含 Header、Sidebar、Main）
  - 配置 Vue Router（/documents、/query）
  - 实现导航菜单
  - _需求：6.1_

- [x] 33. 样式优化和响应式设计
  - 使用 Element Plus 主题定制
  - 实现响应式布局
  - 优化移动端显示
  - _需求：6.1_

- [x] 34. 检查点 - 确保前端功能正常
  - 测试前后端交互
  - 测试所有页面功能
  - 确保 UI 友好美观
  - 如有问题请询问用户

### 第七阶段：集成测试和优化

- [ ] 35. 端到端集成测试
  - [ ]* 35.1 编写完整流程的集成测试
    - 测试文档上传 → 处理 → 查询 → 生成答案的完整流程
    - 测试多文档场景
    - 测试并发查询
    - _需求：1.1, 2.1, 2.2, 3.1, 3.4, 4.1, 4.2, 5.1, 5.2_

- [x] 36. 性能优化
  - 优化数据库查询（添加必要的索引）
  - 优化向量检索性能
  - 实现连接池配置
  - 实现缓存机制（可选）
  - _需求：10.4, 10.5_

- [x] 37. 文档和部署准备
  - 编写 README.md（项目介绍、安装步骤、使用说明）
  - 编写 API 文档（可使用 Swagger）
  - 准备部署脚本（Docker Compose 或启动脚本）
  - 编写配置说明文档

- [x] 38. 最终检查点
  - 运行所有测试，确保全部通过
  - 测试完整的用户场景
  - 检查日志记录是否完整
  - 检查错误处理是否友好
  - 如有问题请询问用户

## 注意事项

- 任务标记 `*` 的为可选测试任务，可以跳过以加快 MVP 开发
- 每个任务都引用了具体的需求编号，便于追溯
- 检查点任务用于确保增量验证，及时发现问题
- 所有配置信息已在设计文档中定义，实现时直接使用
- 属性测试使用 jqwik 框架，每个测试至少运行 100 次迭代
- 前端测试使用 Vitest 和 Vue Test Utils

## 技术要点

- **后端**：Spring Boot 3.2.x + MyBatis-Plus + Lombok + Hutool
- **数据库**：PostgreSQL 18.1 (192.168.14.128:5432)
- **向量数据库**：Qdrant 1.16.3 (192.168.14.128:6333)
- **外部服务**：OpenRouter API（统一调用 Embeddings 和 Chat）
- **前端**：Vue3 + Element Plus + Pinia + Axios
- **测试**：JUnit 5 + jqwik (属性测试) + Vitest (前端测试)
