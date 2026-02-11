import request, { type ApiResponse } from '@/utils/request'
import type { AxiosProgressEvent } from 'axios'

// --- Type Definitions ---

export interface DocumentVO {
  id: number
  fileName: string
  fileSize: number
  fileType: string
  status: string
  uploadTime: string
  chunkCount: number
}

export interface DocumentChunkVO {
  id: number
  chunkIndex: number
  content: string
  charCount: number
}

export interface DocumentDetailVO extends DocumentVO {
  processTime: string | null
  errorMessage: string | null
  chunks: DocumentChunkVO[]
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

// --- API Functions ---

export const documentApi = {
  /**
   * Upload a document (multipart/form-data)
   * POST /api/documents
   */
  upload(file: File, onProgress?: (event: AxiosProgressEvent) => void) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<ApiResponse<DocumentVO>>('/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
    })
  },

  /**
   * Get document list with pagination
   * GET /api/documents?page=1&size=10
   */
  list(page: number = 1, size: number = 10) {
    return request.get<ApiResponse<PageResult<DocumentVO>>>('/documents', {
      params: { page, size }
    })
  },

  /**
   * Delete a document by ID
   * DELETE /api/documents/{id}
   */
  delete(id: number) {
    return request.delete<ApiResponse<null>>(`/documents/${id}`)
  },

  /**
   * Get document detail by ID
   * GET /api/documents/{id}
   */
  getDetail(id: number) {
    return request.get<ApiResponse<DocumentDetailVO>>(`/documents/${id}`)
  }
}
