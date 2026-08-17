import { networkManager } from '../utils/networkManager'
import {
  Product as GraphQLProduct,
  ProductPriceVo,
  ProductStatus
} from '@aquarush/common'
import { ProductServiceResult } from '../types/product'
import { normalizeCents } from '@aquarush/common'

// 商品成交单价信息(按登录用户解析,R-MKT-02:分组价→特价→新人价→目录价)
export interface ProductPriceInfo {
  productId: string
  // 有效成交单价(分,R-MKT-02 链路解析)
  effectivePriceCents: number
  // 成交单价来源(展示标签依据:GROUP_PRICE/FLASH_SALE_PRICE/NEWCOMER_PRICE/CATALOG_PRICE —— R-MKT-01)
  priceSource?: string
  // 是否命中熟客价(分组价)
  hasGroupPrice: boolean
  // 商品目录价(分)
  catalogPriceCents: number
}

// Extended Product type for homepage compatibility
interface ExtendedProduct {
  id: string
  name: string
  subtitle?: string
  price: number // Convert string price to number for displayCents compatibility
  // 当前用户有效成交单价(分,R-MKT-02 链路解析)
  effectivePrice?: number
  // 成交单价来源(GROUP_PRICE/FLASH_SALE_PRICE/NEWCOMER_PRICE/CATALOG_PRICE —— R-MKT-01)
  priceSource?: string
  // 是否命中熟客价(分组价)
  hasGroupPrice?: boolean
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
  /** 关键词(按名称模糊匹配,服务端过滤) */
  keyword?: string
}

class ProductService {
  private static instance: ProductService

  private constructor() {
    // Use shared networkManager from networkManager.ts
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
            priceCents
            originalPriceCents
            depositPriceCents
            effectivePriceCents
            priceSource
            hasGroupPrice
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
            createdAt
            updatedAt
          }
        }
      `

      const variables = { id }

      console.log('Fetching product detail for ID:', id)

      const response = await networkManager.query<{ product: GraphQLProduct | null }>(query, variables)

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
        sortDirection = "desc",
        keyword
      } = params

      const query = `
query GetActiveProducts(
  $page: Int
  $size: Int
  $sortBy: String
  $sortDirection: String
  $keyword: String
) {
  activeProducts(
    page: $page
    size: $size
    sortBy: $sortBy
    sortDirection: $sortDirection
    keyword: $keyword
  ) {
    list {
      id
      name
      subtitle
      specification
      priceCents
      originalPriceCents
      depositPriceCents
      effectivePriceCents
      priceSource
      hasGroupPrice
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

      const variables = { page, size, sortBy, sortDirection, keyword }

      console.log('Fetching active products with params:', variables)

      const response = await networkManager.query<{ activeProducts: any }>(query, variables)

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
   * Get per-user resolved product price (熟客价/目录价,R-MKT-01)
   * @param productId Product ID to fetch price for
   * @returns Promise with effective price info or error
   */
  public async getProductPrice(productId: string): Promise<ProductServiceResult<ProductPriceInfo>> {
    try {
      const query = `
        query ProductPrice($productId: PrimaryId!) {
          productPrice(productId: $productId) {
            productId
            effectivePriceCents
            priceSource
            hasGroupPrice
            catalogPriceCents
          }
        }
      `

      const variables = { productId }

      const response = await networkManager.query<{ productPrice: ProductPriceVo | null }>(query, variables)

      if (!response?.productPrice) {
        throw new Error('Product price not found')
      }

      const price = response.productPrice

      return {
        success: true,
        data: {
          productId: price.productId,
          effectivePriceCents: normalizeCents(price.effectivePriceCents) ?? 0,
          priceSource: price.priceSource,
          hasGroupPrice: price.hasGroupPrice,
          catalogPriceCents: normalizeCents(price.catalogPriceCents) ?? 0
        }
      }
    } catch (error) {
      console.error('Get product price error:', error)

      let errorMessage = '获取商品价格失败，请重试'
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
   * Transform GraphQL Product to frontend Product format
   * @param product GraphQL Product response
   * @returns Formatted product for frontend use
   */
  private transformProduct(product: GraphQLProduct): ExtendedProduct {
    return {
      id: product.id,
      name: product.name,
      subtitle: product.subtitle ?? undefined,
      price: normalizeCents(product.priceCents) ?? 0,
      effectivePrice: normalizeCents(product.effectivePriceCents) ?? undefined,
      priceSource: product.priceSource,
      hasGroupPrice: product.hasGroupPrice,
      originalPrice: normalizeCents(product.originalPriceCents) ?? undefined,
      depositPrice: normalizeCents(product.depositPriceCents) ?? undefined,
      coverImageUrl: product.coverImageUrl,
      image: product.coverImageUrl, // Map coverImageUrl to image for homepage compatibility
      imageGallery: product.imageGallery ?? undefined,
      specification: product.specification,
      waterSource: product.waterSource ?? undefined,
      mineralContent: product.mineralContent ?? undefined,
      stock: product.stock,
      salesVolume: product.salesVolume,
      status: product.status,
      sortOrder: product.sortOrder,
      tags: product.tags ?? undefined,
      detailContent: product.detailContent ?? undefined,
      certificateImages: product.certificateImages ?? undefined,
      createdAt: product.createdAt,
      updatedAt: product.updatedAt,
      // Add description field for homepage compatibility
      description: product.subtitle || product.specification || ''
    }
  }
}

export default ProductService
