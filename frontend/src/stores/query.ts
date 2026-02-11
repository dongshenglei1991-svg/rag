import { defineStore } from 'pinia'
import { queryApi, type ChunkReference } from '@/api/query'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  references?: ChunkReference[]
  timestamp: number
  responseTimeMs?: number
}

export const useQueryStore = defineStore('query', {
  state: () => ({
    messages: [] as ChatMessage[],
    loading: false,
    currentQuery: ''
  }),

  actions: {
    async submitQuery(query: string) {
      this.currentQuery = query
      this.loading = true

      // Add user message
      this.messages.push({
        role: 'user',
        content: query,
        timestamp: Date.now()
      })

      try {
        const res = await queryApi.submit(query)
        const data = res.data.data

        // Add assistant message
        this.messages.push({
          role: 'assistant',
          content: data.answer,
          references: data.references,
          timestamp: Date.now(),
          responseTimeMs: data.responseTimeMs
        })

        return data
      } catch (error) {
        // Remove the user message on failure so the conversation stays clean
        this.messages.pop()
        throw error
      } finally {
        this.loading = false
        this.currentQuery = ''
      }
    },

    async fetchHistory() {
      this.loading = true
      try {
        const res = await queryApi.getHistory(1, 50)
        const records = res.data.data.records

        // Convert history records to chat messages (oldest first)
        this.messages = records
          .slice()
          .reverse()
          .flatMap((record) => [
            {
              role: 'user' as const,
              content: record.queryText,
              timestamp: new Date(record.queryTime).getTime()
            },
            {
              role: 'assistant' as const,
              content: record.answer,
              timestamp: new Date(record.queryTime).getTime(),
              responseTimeMs: record.responseTimeMs
            }
          ])
      } finally {
        this.loading = false
      }
    },

    clearHistory() {
      this.messages = []
      this.currentQuery = ''
    }
  }
})
