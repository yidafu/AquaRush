import { networkManager } from '../utils/networkManager'
import { Banner } from '@aquarush/common'

export interface ConfigServiceResult<T> {
  success: true
  data: T
}

export interface ConfigServiceError {
  success: false
  error: {
    message: string
    code?: string
  }
}

export type ConfigServiceResponse<T> = ConfigServiceResult<T> | ConfigServiceError

/**
 * 运营配置查询服务(用户端)
 * 首页轮播位动态读取(F-OPS-001);时间窗过期/停用由服务端过滤(R-OPS-01)。
 */
class ConfigService {
  private static instance: ConfigService

  private constructor() {
    // Use shared networkManager from networkManager.ts
  }

  public static getInstance(): ConfigService {
    if (!ConfigService.instance) {
      ConfigService.instance = new ConfigService()
    }
    return ConfigService.instance
  }

  /**
   * 获取当前生效的轮播图列表(按排序号升序;未配置/无生效条目返回空数组)
   */
  public async getActiveBanners(): Promise<ConfigServiceResponse<Banner[]>> {
    const query = `
      query ActiveBanners {
        activeBanners {
          id
          imageUrl
          title
          subtitle
          linkType
          linkTarget
          sortOrder
          enabled
        }
      }
    `

    try {
      const response = await networkManager.query<{ activeBanners: Banner[] }>(query)
      return {
        success: true,
        data: response.data?.activeBanners ?? []
      }
    } catch (error: any) {
      console.error('Get active banners failed:', error)
      return {
        success: false,
        error: {
          message: error?.message || '获取轮播图失败',
          code: error?.code
        }
      }
    }
  }
}

export default ConfigService.getInstance()
