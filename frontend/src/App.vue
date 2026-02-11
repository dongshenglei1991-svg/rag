<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style>
/* Basic CSS reset / normalization */
*,
*::before,
*::after {
  box-sizing: border-box;
}

html {
  -webkit-text-size-adjust: 100%;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  line-height: 1.5;
  color: #303133;
  background-color: #f5f7fa;
}

#app {
  width: 100%;
  height: 100vh;
}

/* Minimal Element Plus theme overrides */
:root {
  --el-color-primary: #409eff;
  --el-border-radius-base: 6px;
}

/* Ensure images and media are responsive */
img,
video {
  max-width: 100%;
  height: auto;
}

/* Smooth scrolling */
html {
  scroll-behavior: smooth;
}
</style>
