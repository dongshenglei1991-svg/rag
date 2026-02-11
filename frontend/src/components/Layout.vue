<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, Document, Fold, Expand } from '@element-plus/icons-vue'

const route = useRoute()
const sidebarCollapsed = ref(false)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <el-button
        class="sidebar-toggle"
        :icon="sidebarCollapsed ? Expand : Fold"
        text
        @click="toggleSidebar"
      />
      <div class="header-title">RAG 智能问答系统</div>
    </el-header>
    <el-container>
      <el-aside
        :width="sidebarCollapsed ? '0px' : '200px'"
        class="layout-aside"
        :class="{ 'layout-aside--collapsed': sidebarCollapsed }"
      >
        <el-menu
          :default-active="route.path"
          router
        >
          <el-menu-item index="/query">
            <el-icon><ChatDotRound /></el-icon>
            <span>智能问答</span>
          </el-menu-item>
          <el-menu-item index="/documents">
            <el-icon><Document /></el-icon>
            <span>文档管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-header {
  background-color: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 16px;
}

.sidebar-toggle {
  color: #fff;
  font-size: 20px;
  margin-right: 8px;
}

.sidebar-toggle:hover {
  color: #e6e6e6;
}

.header-title {
  font-size: 20px;
  font-weight: bold;
}

.layout-aside {
  border-right: 1px solid #e6e6e6;
  transition: width 0.3s ease;
  overflow: hidden;
}

.layout-aside--collapsed {
  border-right: none;
}

.layout-main {
  background-color: #f5f7fa;
}

/* Responsive: auto-collapse sidebar on small screens */
@media (max-width: 768px) {
  .header-title {
    font-size: 16px;
  }
}
</style>
