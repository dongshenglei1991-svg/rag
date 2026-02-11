import request, { type ApiResponse } from '@/utils/request'

// --- Type Definitions ---

export interface QueryRequest {
  query: string
  topK?: number
}

export interface ChunkReference {
  documentId: number
  documentName: string
  content: string
  score: number
}

export interface QueryResponseVO {
  query: string
  answer: string
  references: ChunkReference[]
  responseTimeMs: number
}

export interface QueryHistoryVO {
  id: number
  queryText: string
  answer: string
  queryTime: string
  responseTimeMs: number
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

// --- API Functions ---

export const queryApi = {
  /**
   * Submit a query
   * POST /api/query
   */
  submit(query: string, topK: number = 5) {
    return request.post<ApiResponse<QueryResponseVO>>('/query', {
      query,
      topK
    } as QueryRequest)
  },

  /**
   * Get query history with pagination
   * GET /api/query/history?page=1&size=20
   */
  getHistory(page: number = 1, size: number = 20) {
    return request.get<ApiResponse<PageResult<QueryHistoryVO>>>('/query/history', {
      params: { page, size }
    })
  }
}
