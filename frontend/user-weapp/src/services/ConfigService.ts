import { networkManager } from '../utils/networkManager'
import { BannerVo, DeliveryFeeRuleVo, DeliverySlotVo } from '@aquarush/common'

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
  public async getActiveBanners(): Promise<ConfigServiceResponse<BannerVo[]>> {
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
      const response = await networkManager.query<{ activeBanners: BannerVo[] }>(query)
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

  /**
   * 获取配送费规则(F-OPS-005,楼层口径):免费楼层 + 超出一次性加价
   * 未配置时服务端返回默认规则(免费4楼/加价2元);金额为分(Long 以字符串返回,normalize 转 number)。
   */
  public async getDeliveryFeeRule(): Promise<ConfigServiceResponse<DeliveryFeeRuleVo>> {
    const query = `
      query DeliveryFeeRuleVo {
        deliveryFeeRule {
          freeFloorLimit
          surchargeCents
          updatedAt
        }
      }
    `

    try {
      const response = await networkManager.query<{ deliveryFeeRule: DeliveryFeeRuleVo }>(query)
      const rule = response.data?.deliveryFeeRule
      return {
        success: true,
        data: {
          ...rule,
          // Long 标量在 JSON 中可能为字符串,统一转 number
          surchargeCents: Number(rule?.surchargeCents ?? 0)
        }
      }
    } catch (error: any) {
      console.error('Get delivery fee rule failed:', error)
      return {
        success: false,
        error: {
          message: error?.message || '获取配送费规则失败',
          code: error?.code
        }
      }
    }
  }

  /**
   * 获取配送时段配置(F-SYS-002,AC-SYS-002-01/03):后台「订单与履约参数」配置,
   * 立即配送时限/上午/下午/晚上时间窗与启停,保存即热生效;
   * 未配置/失败时服务端返回种子默认,前端另有 DEFAULT_DELIVERY_SLOTS 兜底。
   */
  public async getDeliverySlots(): Promise<ConfigServiceResponse<DeliverySlotVo[]>> {
    const query = `
      query DeliverySlots {
        deliverySlots {
          id
          name
          hint
          enabled
        }
      }
    `

    try {
      const response = await networkManager.query<{ deliverySlots: DeliverySlotVo[] }>(query)
      return {
        success: true,
        data: response.data?.deliverySlots ?? []
      }
    } catch (error: any) {
      console.error('Get delivery slots failed:', error)
      return {
        success: false,
        error: {
          message: error?.message || '获取配送时段失败',
          code: error?.code
        }
      }
    }
  }
}

export default ConfigService.getInstance()
