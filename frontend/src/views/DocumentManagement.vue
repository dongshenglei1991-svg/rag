<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { useDocumentStore } from '@/stores/document'
import { formatFileSize } from '@/utils'
import DocumentUploader from '@/components/DocumentUploader.vue'

const store = useDocumentStore()

/** 获取文档列表 */
function loadDocuments(page?: number) {
  store.fetchDocuments(page ?? store.currentPage, store.pageSize)
}

/** 删除文档 */
async function handleDelete(id: number) {
  try {
    await store.deleteDocument(id)
    ElMessage.success('文档删除成功')
    // 如果当前页没有数据了，回到上一页
    if (store.documents.length === 0 && store.currentPage > 1) {
      loadDocuments(store.currentPage - 1)
    }
  } catch {
    ElMessage.error('删除失败，请稍后重试')
  }
}

/** 上传成功后刷新列表 */
function handleUploadSuccess() {
  loadDocuments(1)
}

/** 分页变化 */
function handlePageChange(page: number) {
  loadDocuments(page)
}

/** 每页条数变化 */
function handleSizeChange(size: number) {
  store.pageSize = size
  loadDocuments(1)
}

/** 状态标签类型映射 */
function statusTagType(status: string): '' | 'success' | 'warning' | 'danger' {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'PROCESSING':
      return 'warning'
    case 'FAILED':
      return 'danger'
    default:
      return ''
  }
}

/** 状态中文标签 */
function statusLabel(status: string): string {
  switch (status) {
    case 'COMPLETED':
      return '已完成'
    case 'PROCESSING':
      return '处理中'
    case 'FAILED':
      return '失败'
    default:
      return status
  }
}

onMounted(() => {
  loadDocuments(1)
})
</script>

<template>
  <div class="document-management">
    <h2>文档管理</h2>

    <!-- 上传组件 -->
    <DocumentUploader @upload-success="handleUploadSuccess" />

    <!-- 文档列表 -->
    <el-table :data="store.documents" v-loading="store.loading" stripe style="width: 100%">
      <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />

      <el-table-column label="文件大小" width="120" align="center">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>

      <el-table-column prop="fileType" label="文件类型" width="140" align="center" />

      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="uploadTime" label="上传时间" width="180" align="center" />

      <el-table-column prop="chunkCount" label="片段数" width="90" align="center" />

      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-popconfirm
            title="确定删除该文档吗？"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleDelete(row.id)"
          >
            <template #reference>
              <el-button type="danger" :icon="Delete" size="small" text>
                删除
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="store.currentPage"
        v-model:page-size="store.pageSize"
        :total="store.total"
        :page-sizes="[5, 10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.document-management {
  padding: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* Responsive: make table scrollable and adjust padding on small screens */
@media (max-width: 768px) {
  .document-management {
    padding: 12px;
  }

  .document-management h2 {
    font-size: 18px;
  }

  .document-management :deep(.el-table) {
    overflow-x: auto;
  }

  .pagination-wrapper {
    justify-content: center;
  }

  .pagination-wrapper :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
