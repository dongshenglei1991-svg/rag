<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, type UploadProps, type UploadRawFile } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { documentApi } from '@/api/document'

const emit = defineEmits<{ 'upload-success': [] }>()

const ACCEPT_EXTENSIONS = ['.pdf', '.txt', '.docx', '.md']
const MAX_FILE_SIZE = 52428800 // 50MB

const uploading = ref(false)
const progressPercent = ref(0)

/** 文件格式校验 */
function getExtension(name: string): string {
  const idx = name.lastIndexOf('.')
  return idx >= 0 ? name.slice(idx).toLowerCase() : ''
}

/** el-upload before-upload 钩子：格式 + 大小校验 */
const beforeUpload: UploadProps['beforeUpload'] = (rawFile: UploadRawFile) => {
  const ext = getExtension(rawFile.name)
  if (!ACCEPT_EXTENSIONS.includes(ext)) {
    ElMessage.error(`不支持的文件格式，仅支持 ${ACCEPT_EXTENSIONS.join('、')}`)
    return false
  }
  if (rawFile.size > MAX_FILE_SIZE) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
}

/** 自定义上传处理 */
const handleUpload: UploadProps['httpRequest'] = async (options) => {
  const file = options.file
  uploading.value = true
  progressPercent.value = 0

  try {
    await documentApi.upload(file, (event) => {
      if (event.total && event.total > 0) {
        progressPercent.value = Math.round((event.loaded * 100) / event.total)
      }
    })
    ElMessage.success('文档上传成功')
    emit('upload-success')
  } catch {
    ElMessage.error('文档上传失败，请稍后重试')
  } finally {
    uploading.value = false
    progressPercent.value = 0
  }
}
</script>

<template>
  <div class="document-uploader">
    <el-upload
      drag
      :before-upload="beforeUpload"
      :http-request="handleUpload"
      :show-file-list="false"
      :disabled="uploading"
      accept=".pdf,.txt,.docx,.md"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 PDF、TXT、DOCX、MD 格式，单个文件不超过 50MB
        </div>
      </template>
    </el-upload>

    <el-progress
      v-if="uploading"
      :percentage="progressPercent"
      :stroke-width="8"
      style="margin-top: 12px"
    />
  </div>
</template>

<style scoped>
.document-uploader {
  margin-bottom: 20px;
}
</style>
