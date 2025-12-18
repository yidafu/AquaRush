import NetworkManager from '../utils/network'
import apiConfig from '../config/api'
import {
  Product as GraphQLProduct,
  ProductStatus
} from '@aquarush/common'
import {
  ProductServiceResult,
  ProductServiceError
} from '../types/product'
import { parseMoneyString } from '../utils/graphql-money'

// Extended Product type for homepage compatibility
interface ExtendedProduct {
  id: string
  name: string
  subtitle?: string
  price: number // Convert string price to number for displayCents compatibility
  originalPrice?: number
  depositPrice?: number
  coverImageUrl: string
  imageGallery?: any[]
  specification: string
  waterSource?: string
  mineralContent?: string
  stock: number
  salesVolume: number
  status: ProductStatus
  sortOrder: number
  tags?: any[]
  detailContent?: string
  certificateImages?: any[]
  deliverySettings?: Record<string, any>
  isDeleted: boolean
  createdAt: string
  updatedAt: string
  image: string // Mapped from coverImageUrl for homepage compatibility
  description: string // Additional field for homepage compatibility
}

// Extended ProductListResponse type for homepage compatibility
interface ExtendedProductListResponse {
  products: ExtendedProduct[]
  pagination: {
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
  }
}

// Product list parameters for API calls
interface ProductListParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: string
}

class ProductService {
  private static instance: ProductService
  private networkManager: NetworkManager

  private constructor() {
    // Use centralized API configuration
    this.networkManager = NetworkManager.getInstance({
      baseURL: apiConfig.getGraphqlUrl(),
      timeout: apiConfig.getTimeout(),
      headers: apiConfig.getHeaders()
    })
  }

  public static getInstance(): ProductService {
    if (!ProductService.instance) {
      ProductService.instance = new ProductService()
    }
    return ProductService.instance
  }

  /**
   * Get product details by ID
   * @param id Product ID to fetch
   * @returns Promise with product details or error
   */
  public async getProductDetail(id: string): Promise<ProductServiceResult<GraphQLProduct>> {
    try {
      const query = `
        query GetProduct($id: PrimaryId!) {
          product(id: $id) {
            id
            name
            subtitle
            price
            originalPrice
            depositPrice
            coverImageUrl
            imageGallery
            specification
            waterSource
            mineralContent
            stock
            salesVolume
            status
            sortOrder
            tags
            detailContent
            certificateImages
            deliverySettings
            isDeleted
            createdAt
            updatedAt
          }
        }
      `

      const variables = { id }

      console.log('Fetching product detail for ID:', id)

      const response = await this.networkManager.query<{ product: GraphQLProduct | null }>(query, variables)

      if (!response?.product) {
        throw new Error('Product not found')
      }

      console.log('Product detail fetched successfully:', response.product)

      return {
        success: true,
        data: response.product
      }
    } catch (error) {
      console.error('Get product detail error:', error)

      let errorMessage = '获取产品详情失败，请重试'
      let errorCode: string | undefined

      // Handle GraphQL errors
      if (error && typeof error === 'object' && 'errors' in error) {
        const graphqlErrors = (error as any).errors
        if (Array.isArray(graphqlErrors) && graphqlErrors.length > 0) {
          const firstError = graphqlErrors[0]
          errorMessage = firstError.message || errorMessage
          errorCode = firstError.extensions?.code
        }
      } else if (error instanceof Error) {
        errorMessage = error.message
      }

      return {
        success: false,
        error: {
          message: errorMessage,
          code: errorCode
        }
      }
    }
  }

  /**
   * Get active products with pagination
   * @param params Query parameters for pagination and sorting
   * @returns Promise with product list and pagination metadata
   */
  public async getActiveProducts(params: ProductListParams = {}): Promise<ProductServiceResult<ExtendedProductListResponse>> {
    try {
      const {
        page = 0,
        size = 20,
        sortBy = "createdAt",
        sortDirection = "desc"
      } = params

      const query = `
query GetActiveProducts(
  $page: Int
  $size: Int
  $sortBy: String
  $sortDirection: String
) {
  activeProducts(
    page: $page
    size: $size
    sortBy: $sortBy
    sortDirection: $sortDirection
  ) {
    list {
      id
      name
      subtitle
      price
      originalPrice
      depositPrice
      coverImageUrl
      stock
      status
      salesVolume
      createdAt
    }
    pageInfo {
      total
      pageSize
      pageNum
    }
  }
}

      `

      const variables = { page, size, sortBy, sortDirection }

      console.log('Fetching active products with params:', variables)

      const response = await this.networkManager.query<{ activeProducts: any }>(query, variables)

      if (!response?.activeProducts) {
        throw new Error('Failed to fetch products')
      }

      const { list, pageInfo } = response.activeProducts

      // Transform the data to match our expected response format
      const transformedProducts = list.map((product: any) => this.transformProduct(product))

      const result: ExtendedProductListResponse = {
        products: transformedProducts,
        pagination: {
          page: pageInfo.pageNum,
          size: pageInfo.pageSize,
          totalElements: pageInfo.total,
          totalPages: Math.ceil(pageInfo.total / pageInfo.pageSize),
          first: pageInfo.pageNum === 0,
          last: pageInfo.pageNum >= Math.ceil(pageInfo.total / pageInfo.pageSize) - 1
        }
      }

      console.log('Active products fetched successfully:', {
        productCount: result.products.length,
        currentPage: result.pagination.page,
        totalPages: result.pagination.totalPages
      })

      return {
        success: true,
        data: result
      }
    } catch (error) {
      console.error('Get active products error:', error)

      let errorMessage = '获取产品列表失败，请重试'
      let errorCode: string | undefined

      // Handle GraphQL errors
      if (error && typeof error === 'object' && 'errors' in error) {
        const graphqlErrors = (error as any).errors
        if (Array.isArray(graphqlErrors) && graphqlErrors.length > 0) {
          const firstError = graphqlErrors[0]
          errorMessage = firstError.message || errorMessage
          errorCode = firstError.extensions?.code
        }
      } else if (error instanceof Error) {
        errorMessage = error.message
      }

      return {
        success: false,
        error: {
          message: errorMessage,
          code: errorCode
        }
      }
    }
  }

  /**
   * Load more products for pagination
   * @param page The page number to load
   * @param params Additional query parameters
   * @returns Promise with product list and pagination metadata
   */
  public async loadMoreProducts(page: number, params: Omit<ProductListParams, 'page'> = {}): Promise<ProductServiceResult<ExtendedProductListResponse>> {
    return this.getActiveProducts({ ...params, page })
  }

  /**
   * Transform GraphQL Product to frontend Product format
   * @param product GraphQL Product response
   * @returns Formatted product for frontend use
   */
  private transformProduct(product: GraphQLProduct): ExtendedProduct {
    return {
      id: product.id,
      name: product.name,
      subtitle: product.subtitle,
      price: parseMoneyString(product.price), // Use parseMoneyString to handle Money scalar properly
      originalPrice: product.originalPrice ? parseMoneyString(product.originalPrice) : undefined,
      depositPrice: product.depositPrice ? parseMoneyString(product.depositPrice) : undefined,
      coverImageUrl: product.coverImageUrl,
      image: product.coverImageUrl, // Map coverImageUrl to image for homepage compatibility
      imageGallery: product.imageGallery,
      specification: product.specification,
      waterSource: product.waterSource,
      mineralContent: product.mineralContent,
      stock: product.stock,
      salesVolume: product.salesVolume,
      status: product.status,
      sortOrder: product.sortOrder,
      tags: product.tags,
      detailContent: product.detailContent,
      certificateImages: product.certificateImages,
      deliverySettings: product.deliverySettings,
      isDeleted: product.isDeleted,
      createdAt: product.createdAt,
      updatedAt: product.updatedAt,
      // Add description field for homepage compatibility
      description: product.subtitle || product.specification || ''
    }
  }
}

export default ProductService
