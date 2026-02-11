import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/components/Layout.vue'

const routes: RouteRecordRaw[] = [
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
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - RAG 智能问答系统`
  }
  next()
})

export default router
