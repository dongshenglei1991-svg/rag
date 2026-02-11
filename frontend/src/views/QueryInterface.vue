<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, Delete } from '@element-plus/icons-vue'
import { useQueryStore } from '@/stores/query'
import ChatMessage from '@/components/ChatMessage.vue'

const store = useQueryStore()
const inputQuery = ref('')
const chatContainerRef = ref<HTMLElement | null>(null)

/** 滚动到底部 */
function scrollToBottom() {
  nextTick(() => {
    if (chatContainerRef.value) {
      chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight
    }
  })
}

/** 提交查询 */
async function handleSubmit() {
  const query = inputQuery.value.trim()
  if (!query) {
    ElMessage.warning('请输入查询内容')
    return
  }
  if (store.loading) return

  inputQuery.value = ''
  scrollToBottom()

  try {
    await store.submitQuery(query)
    scrollToBottom()
  } catch {
    ElMessage.error('查询失败，请稍后重试')
  }
}

/** 按 Enter 提交（Shift+Enter 换行） */
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSubmit()
  }
}

/** 清空对话 */
function handleClear() {
  store.clearHistory()
}

onMounted(() => {
  scrollToBottom()
})
</script>

<template>
  <div class="query-interface">
    <!-- 顶部标题栏 -->
    <div class="header">
      <h2>智能问答</h2>
      <el-button
        v-if="store.messages.length > 0"
        :icon="Delete"
        size="small"
        text
        @click="handleClear"
      >
        清空对话
      </el-button>
    </div>

    <!-- 聊天历史区域 -->
    <div ref="chatContainerRef" class="chat-container">
      <!-- 空状态 -->
      <div v-if="store.messages.length === 0 && !store.loading" class="empty-state">
        <el-icon :size="48" color="#c0c4cc"><Promotion /></el-icon>
        <p>请输入您的问题，开始智能问答</p>
      </div>

      <!-- 消息列表 -->
      <ChatMessage
        v-for="(msg, index) in store.messages"
        :key="index"
        :message="msg"
        :index="index"
      />

      <!-- 加载指示器 -->
      <div v-if="store.loading" class="message-row message-row--assistant">
        <div class="avatar avatar--assistant">AI</div>
        <div class="bubble bubble--assistant">
          <div class="loading-dots">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        v-model="inputQuery"
        type="textarea"
        :rows="2"
        placeholder="请输入您的问题..."
        :disabled="store.loading"
        resize="none"
        @keydown="handleKeydown"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        :loading="store.loading"
        :disabled="!inputQuery.trim()"
        @click="handleSubmit"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.query-interface {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.header h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

/* 聊天区域 */
.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

/* 加载指示器的消息行样式（保留用于 loading 状态） */
.message-row {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  color: #fff;
}

.avatar--assistant {
  background: #67c23a;
  margin-right: 10px;
}

.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.bubble--assistant {
  background: #fff;
  color: #303133;
  border-top-left-radius: 2px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

/* 加载动画 */
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: dot-bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: 0s;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 输入区域 */
.input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.input-area .el-textarea {
  flex: 1;
}

.input-area .el-button {
  height: 54px;
}

/* Responsive: adjust chat bubbles and input area on small screens */
@media (max-width: 768px) {
  .header {
    padding: 10px 12px;
  }

  .header h2 {
    font-size: 16px;
  }

  .chat-container {
    padding: 12px;
  }

  .bubble {
    max-width: 85%;
  }

  .input-area {
    padding: 10px 12px;
    gap: 8px;
  }

  .input-area .el-button {
    height: 44px;
    padding: 0 12px;
  }
}
</style>
