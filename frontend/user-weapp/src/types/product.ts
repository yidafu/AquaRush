// Product types for GraphQL operations and frontend data structures
import { ProductVo } from '@aquarush/common'

// Error types for service operations
export interface ProductServiceError {
  message: string
  code?: string
  field?: string
}

// Product list API types
export interface ProductListParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: string
}

export interface ProductListResponse {
  products: ProductVo[]
  pagination: {
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
  }
}

// GraphQL Page response wrapper
export interface ProductPageResponse {
  content: ProductVo[]
  page: {
    number: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
  }
}

// Service method return types
export interface ProductServiceResult<T> {
  success: boolean
  data?: T
  error?: ProductServiceError
}

export { ProductVo }

