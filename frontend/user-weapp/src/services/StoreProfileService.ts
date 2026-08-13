import { networkManager } from '../utils/networkManager'
import { StoreProfile } from '@aquarush/common'
import { ConfigServiceError, ConfigServiceResult } from './ConfigService'

export type StoreProfileServiceResponse<T> = ConfigServiceResult<T> | ConfigServiceError

/**
 * 店铺信息查询服务(用户端,F-OPS-004)
 * 关于页/意见反馈页动态读取店铺名称、LOGO、客服电话、营业时间等;
 * 未配置时服务端返回 null,页面按默认值兜底展示(R-OPS-01)。
 */
class StoreProfileService {
  private static instance: StoreProfileService

  private constructor() {
    // Use shared networkManager from networkManager.ts
  }

  public static getInstance(): StoreProfileService {
    if (!StoreProfileService.instance) {
      StoreProfileService.instance = new StoreProfileService()
    }
    return StoreProfileService.instance
  }

  /**
   * 获取当前店铺配置(未配置返回 null)
   */
  public async getStoreProfile(): Promise<StoreProfileServiceResponse<StoreProfile | null>> {
    const query = `
      query StoreProfile {
        storeProfile {
          shopName
          logoUrl
          serviceHotline
          businessHoursStart
          businessHoursEnd
          shopIntro
          aboutUs
        }
      }
    `

    try {
      const response = await networkManager.query<{ storeProfile: StoreProfile | null }>(query)
      return {
        success: true,
        data: response.data?.storeProfile ?? null
      }
    } catch (error: any) {
      console.error('Get store profile failed:', error)
      return {
        success: false,
        error: {
          message: error?.message || '获取店铺信息失败',
          code: error?.code
        }
      }
    }
  }
}

export default StoreProfileService.getInstance()
