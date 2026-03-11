import { networkManager } from '../utils/networkManager'
import apiConfig from '../config/api'
import { authService } from '../utils/auth'

// Favorite product type from GraphQL schema
export interface FavoriteProduct {
  id: string
  name: string
  subtitle?: string
  price: string
  originalPrice?: string
  coverImageUrl: string
  stock: number
  salesVolume: number
  status: string
  addedAt: string
}

// Pagination info
export interface PageInfo {
  total: number
  pageSize: number
  pageNum: number
  hasNext: boolean
  hasPrevious: boolean
  totalPages: number
}

// Favorite products response
export interface FavoriteProductsResponse {
  list: FavoriteProduct[]
  pageInfo: PageInfo
}

class FavoriteService {
  private static instance: FavoriteService

  static getInstance(): FavoriteService {
    if (!FavoriteService.instance) {
      FavoriteService.instance = new FavoriteService()
    }
    return FavoriteService.instance
  }

  constructor() {
    // Use shared networkManager from networkManager.ts
  }

  /**
   * Toggle product favorite status (add if not favorited, remove if favorited)
   */
  async toggleProductFavorites(productId: string): Promise<boolean> {
    try {
      const mutation = `
        mutation ToggleProductFavorites($productId: PrimaryId!) {
          toggleProductFavorites(productId: $productId)
        }
      `

      const response = await networkManager.mutate(
        mutation, { productId }
      )

      return response.toggleProductFavorites
    } catch (error) {
      console.error('Failed to toggle favorite status:', error)
      throw error
    }
  }


  /**
   * Get favorite products with pagination
   */
  async getFavoriteProducts(page: number = 0, size: number = 20): Promise<FavoriteProductsResponse> {
    try {
      const query = `
        query FavoriteProducts($page: Int, $size: Int) {
          favoriteProducts(page: $page, size: $size) {
            list {
              id
              name
              subtitle
              price
              originalPrice
              coverImageUrl
              stock
              salesVolume
              status
              addedAt
            }
            pageInfo {
              total
              pageSize
              pageNum
              hasNext
              hasPrevious
              totalPages
            }
          }
        }
      `

      const response = await networkManager.query(
        query,
        { page, size }
      )

      return response.favoriteProducts
    } catch (error) {
      console.error('Failed to get favorite products:', error)
      throw error
    }
  }

  /**
   * Check if product is favorited
   */
  async isProductFavorited(productId: string): Promise<boolean> {
    try {
      const query = `
        query IsProductFavorited($productId: PrimaryId!) {
          isProductFavorited(productId: $productId)
        }
      `

      const response = await networkManager.query(
        query,
        { productId }
        )

      return response.isProductFavorited
    } catch (error) {
      console.error('Failed to check favorite status:', error)
      return false
    }
  }

  /**
   * Get favorites count
   */
  async getFavoritesCount(): Promise<number> {
    try {
      const query = `
        query FavoritesCount {
          favoritesCount
        }
      `

      const response = await networkManager.query(query)

      return response.favoritesCount
    } catch (error) {
      console.error('Failed to get favorites count:', error)
      return 0
    }
  }

  /**
   * Toggle favorite status (alias for toggleProductFavorites)
   */
  async toggleFavorite(productId: string): Promise<boolean> {
    return await this.toggleProductFavorites(productId)
  }

  /**
   * Get all favorite products (no pagination, for small lists)
   */
  async getAllFavoriteProducts(): Promise<FavoriteProduct[]> {
    try {
      // Start with page 0, size 100 (should be enough for most users)
      const response = await this.getFavoriteProducts(0, 100)
      return response.list
    } catch (error) {
      console.error('Failed to get all favorite products:', error)
      return []
    }
  }
}

export default FavoriteService
