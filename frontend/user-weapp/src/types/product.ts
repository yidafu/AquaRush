// Product types for GraphQL operations and frontend data structures
import { Product } from '@aquarush/common'

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
  products: Product[]
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
  content: Product[]
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

export { Product }

