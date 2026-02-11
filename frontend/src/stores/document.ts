import { defineStore } from 'pinia'
import { documentApi, type DocumentVO } from '@/api/document'

export const useDocumentStore = defineStore('document', {
  state: () => ({
    documents: [] as DocumentVO[],
    loading: false,
    currentPage: 1,
    pageSize: 10,
    total: 0
  }),

  actions: {
    async uploadDocument(file: File) {
      this.loading = true
      try {
        const res = await documentApi.upload(file)
        const doc = res.data.data
        this.documents.unshift(doc)
        this.total++
        return doc
      } finally {
        this.loading = false
      }
    },

    async fetchDocuments(page: number = 1, size: number = 10) {
      this.loading = true
      try {
        const res = await documentApi.list(page, size)
        const pageResult = res.data.data
        this.documents = pageResult.records
        this.total = pageResult.total
        this.currentPage = page
        this.pageSize = size
      } finally {
        this.loading = false
      }
    },

    async deleteDocument(id: number) {
      this.loading = true
      try {
        await documentApi.delete(id)
        this.documents = this.documents.filter((d) => d.id !== id)
        this.total--
      } finally {
        this.loading = false
      }
    }
  }
})
