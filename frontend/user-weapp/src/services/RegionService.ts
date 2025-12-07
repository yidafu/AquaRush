import NetworkManager from '../utils/network'
import {
  Region,
  RegionCacheItem,
  RegionsResponse,
  RegionVariables,
  RegionHierarchy,
  DefaultRegionResponse
} from '../types/region'

class RegionService {
  private static instance: RegionService
  private networkManager: NetworkManager
  private cache: Map<string, RegionCacheItem> = new Map()

  // Cache configuration
  private readonly CACHE_TTL = {
    provinces: 24 * 60 * 60 * 1000, // 24 hours for provinces
    cities: 60 * 60 * 1000, // 1 hour for cities
    districts: 30 * 60 * 1000 // 30 minutes for districts
  }

  private readonly MAX_CACHE_SIZE = 100 // Maximum number of cache entries

  private constructor() {
    // Use the same configuration as the auth service
    this.networkManager = NetworkManager.getInstance({
      baseURL: 'http://localhost:8080/graphql',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    })
  }

  public static getInstance(): RegionService {
    if (!RegionService.instance) {
      RegionService.instance = new RegionService()
    }
    return RegionService.instance
  }

  private getCacheKey(level: number, parentCode?: string): string {
    return `level_${level}_${parentCode || 'root'}`
  }

  private getFromCache(key: string): Region[] | null {
    const cached = this.cache.get(key)
    if (!cached) {
      return null
    }

    const now = Date.now()
    if (now > cached.timestamp + cached.ttl) {
      this.cache.delete(key)
      return null
    }

    return cached.data
  }

  private setCache(key: string, data: Region[], ttl: number): void {
    // Clean up old cache entries if cache is full
    if (this.cache.size >= this.MAX_CACHE_SIZE) {
      const oldestKey = this.cache.keys().next().value
      if (oldestKey) {
        this.cache.delete(oldestKey)
      }
    }

    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl
    })
  }

  private async fetchRegions(level: number, parentCode?: string): Promise<Region[]> {
    const query = `
      query GetRegions($level: Int, $parentCode: String) {
        regions(level: $level, parentCode: $parentCode) {
          name
          code
          level
        }
      }
    `

    const variables: RegionVariables = {
      level,
      parentCode
    }

    try {
      const response = await this.networkManager.query<RegionsResponse>(query, variables, {})
      console.log('response', response)
      return response?.regions || []
    } catch (error) {
      console.error(`Failed to fetch regions (level: ${level}, parentCode: ${parentCode}):`, error)
      throw new Error(`Failed to load region data: ${error instanceof Error ? error.message : 'Unknown error'}`)
    }
  }

  public async getProvinces(): Promise<Region[]> {
    const cacheKey = this.getCacheKey(1)

    // Check cache first
    const cached = this.getFromCache(cacheKey)
    if (cached) {
      return cached
    }

    // Fetch from API
    const provinces = await this.fetchRegions(1)
    console.log('fetchRegions', provinces)
    // Cache the result
    this.setCache(cacheKey, provinces, this.CACHE_TTL.provinces)

    return provinces
  }

  public async getCities(parentCode: string): Promise<Region[]> {
    if (!parentCode) {
      throw new Error('Parent code is required for fetching cities')
    }

    const cacheKey = this.getCacheKey(2, parentCode)

    // Check cache first
    const cached = this.getFromCache(cacheKey)
    if (cached) {
      return cached
    }

    // Fetch from API
    const cities = await this.fetchRegions(2, parentCode)

    // Cache the result
    this.setCache(cacheKey, cities, this.CACHE_TTL.cities)

    return cities
  }

  public async getDistricts(parentCode: string): Promise<Region[]> {
    if (!parentCode) {
      throw new Error('Parent code is required for fetching districts')
    }

    const cacheKey = this.getCacheKey(3, parentCode)

    // Check cache first
    const cached = this.getFromCache(cacheKey)
    if (cached) {
      return cached
    }

    // Fetch from API
    const districts = await this.fetchRegions(3, parentCode)

    // Cache the result
    this.setCache(cacheKey, districts, this.CACHE_TTL.districts)

    return districts
  }

  public async getRegionByCode(code: string): Promise<Region | null> {
    const query = `
      query GetRegion($code: String!) {
        region(code: $code) {
          name
          code
          level
        }
      }
    `

    try {
      const response = await this.networkManager.query<{ region: Region | null }>(
        query,
        { code },
        {}
      )
      return response?.data?.region || null
    } catch (error) {
      console.error(`Failed to fetch region by code (${code}):`, error)
      return null
    }
  }

  public clearCache(): void {
    this.cache.clear()
  }

  public getCacheSize(): number {
    return this.cache.size
  }

  public preloadCommonData(): Promise<void> {
    // Preload provinces for faster initial loading
    return this.getProvinces().then(() => {
      console.log('Region data preloaded successfully')
    }).catch(error => {
      console.warn('Failed to preload region data:', error)
    })
  }

  /**
   * Get default region hierarchy from backend configuration
   */
  public async getDefaultRegionHierarchy(): Promise<RegionHierarchy | null> {
    const query = `
      query GetDefaultRegionHierarchy {
        defaultRegionHierarchy {
          province {
            name
            code
          }
          city {
            name
            code
          }
          district {
            name
            code
          }
          provinces {
            name
            code
          }
          cities {
            name
            code
          }
          districts {
            name
            code
          }
        }
      }
    `

    try {
      const response = await this.networkManager.query<DefaultRegionResponse>(query, {}, {})
      console.log('Default region hierarchy response:', response)
      return response?.defaultRegionHierarchy || null
    } catch (error) {
      console.error('Failed to fetch default region hierarchy:', error)
      return null
    }
  }
}

export default RegionService
