<script setup lang="ts">
import { ref } from 'vue'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import type { ChatMessage } from '@/stores/query'

const props = defineProps<{
  message: ChatMessage
  index: number
}>()

/** 引用折叠状态 */
const refsExpanded = ref(false)

function toggleRefs() {
  refsExpanded.value = !refsExpanded.value
}

/** 格式化时间戳 */
function formatTime(ts: number): string {
  const d = new Date(ts)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

/** 格式化相似度分数 */
function formatScore(score: number): string {
  return (score * 100).toFixed(1) + '%'
}
</script>

<template>
  <div
    class="message-row"
    :class="{
      'message-row--user': props.message.role === 'user',
      'message-row--assistant': props.message.role === 'assistant'
    }"
  >
    <!-- 头像 -->
    <div
      class="avatar"
      :class="props.message.role === 'user' ? 'avatar--user' : 'avatar--assistant'"
    >
      {{ props.message.role === 'user' ? '我' : 'AI' }}
    </div>

    <!-- 消息内容 -->
    <div
      class="bubble"
      :class="props.message.role === 'user' ? 'bubble--user' : 'bubble--assistant'"
    >
      <div class="bubble-content">{{ props.message.content }}</div>

      <!-- 引用来源（仅助手消息） -->
      <div
        v-if="
          props.message.role === 'assistant' &&
          props.message.references &&
          props.message.references.length > 0
        "
        class="references-section"
      >
        <div class="references-toggle" @click="toggleRefs()">
          <el-icon :size="14">
            <ArrowDown v-if="!refsExpanded" />
            <ArrowUp v-else />
          </el-icon>
          <span>引用来源（{{ props.message.references.length }} 条）</span>
        </div>

        <div v-if="refsExpanded" class="references-list">
          <div
            v-for="(ref, refIdx) in props.message.references"
            :key="refIdx"
            class="reference-item"
          >
            <div class="reference-header">
              <span class="reference-doc">{{ ref.documentName }}</span>
              <el-tag size="small" type="info">相似度 {{ formatScore(ref.score) }}</el-tag>
            </div>
            <div class="reference-content">{{ ref.content }}</div>
          </div>
        </div>
      </div>

      <!-- 时间和响应时间 -->
      <div class="bubble-meta">
        <span>{{ formatTime(props.message.timestamp) }}</span>
        <span v-if="props.message.responseTimeMs">
          · 耗时 {{ props.message.responseTimeMs }}ms
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 消息行 */
.message-row {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.message-row--user {
  flex-direction: row-reverse;
}

/* 头像 */
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

.avatar--user {
  background: #409eff;
  margin-left: 10px;
}

.avatar--assistant {
  background: #67c23a;
  margin-right: 10px;
}

/* 气泡 */
.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.bubble--user {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 2px;
}

.bubble--assistant {
  background: #fff;
  color: #303133;
  border-top-left-radius: 2px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.bubble-meta {
  margin-top: 6px;
  font-size: 11px;
  opacity: 0.7;
}

.bubble--user .bubble-meta {
  text-align: right;
}

/* 引用来源 */
.references-section {
  margin-top: 10px;
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}

.references-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #909399;
  user-select: none;
}

.references-toggle:hover {
  color: #409eff;
}

.references-list {
  margin-top: 8px;
}

.reference-item {
  padding: 8px;
  margin-bottom: 6px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
}

.reference-item:last-child {
  margin-bottom: 0;
}

.reference-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.reference-doc {
  font-weight: 600;
  color: #606266;
}

.reference-content {
  color: #909399;
  line-height: 1.5;
  white-space: pre-wrap;
}

/* Responsive: adjust bubble width on small screens */
@media (max-width: 768px) {
  .bubble {
    max-width: 85%;
    padding: 8px 12px;
    font-size: 13px;
  }

  .avatar {
    width: 30px;
    height: 30px;
    font-size: 11px;
  }

  .reference-item {
    font-size: 11px;
  }
}
</style>
