import { networkManager } from '../utils/networkManager'
import {
  CouponTemplateVo,
  UserCouponVo,
  UserCouponStatus
} from '@aquarush/common'
import {
  ProductServiceError,
  ProductServiceResult
} from '../types/product'

/**
 * 优惠券服务(用户端,营销域 F-MKT-003)
 * 领券中心 / 我的券包 / 领取优惠券;Long 字段传输为字符串,展示层经 normalizeCents 归一化。
 */
class CouponService {
  private static instance: CouponService

  private constructor() {
    // Use shared networkManager from networkManager.ts
  }

  public static getInstance(): CouponService {
    if (!CouponService.instance) {
      CouponService.instance = new CouponService()
    }
    return CouponService.instance
  }

  /**
   * 领券中心:可领取的优惠券模板列表(仅自动领取模板)
   */
  public async getCouponCenter(): Promise<ProductServiceResult<CouponTemplateVo[]>> {
    const query = `
      query CouponCenter {
        couponCenter {
          id
          name
          couponType
          thresholdCents
          discountAmountCents
          discountRatePermille
          scope
          productId
          validityType
          validFrom
          validTo
          validDays
          perUserLimit
          claimedCount
        }
      }
    `

    try {
      const response = await networkManager.query<{ couponCenter: CouponTemplateVo[] }>(query)
      return {
        success: true,
        data: response.couponCenter ?? []
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取优惠券列表失败')
      }
    }
  }

  /**
   * 我的券包:按状态过滤(空=全部)
   * @param status 券状态(UserCouponStatus),省略返回全部
   */
  public async getMyCoupons(
    status?: UserCouponStatus
  ): Promise<ProductServiceResult<UserCouponVo[]>> {
    const query = `
      query MyCoupons($status: UserCouponStatus) {
        myCoupons(status: $status) {
          id
          name
          couponType
          thresholdCents
          discountAmountCents
          discountRatePermille
          scope
          productId
          status
          expireTime
          usable
        }
      }
    `

    try {
      const variables = status ? { status } : {}
      const response = await networkManager.query<{ myCoupons: UserCouponVo[] }>(query, variables)
      return {
        success: true,
        data: response.myCoupons ?? []
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取我的券包失败')
      }
    }
  }

  /**
   * 领取优惠券(领券中心)
   * @param couponTemplateId 优惠券模板 ID(PrimaryId,传输为字符串)
   */
  public async claimCoupon(
    couponTemplateId: string
  ): Promise<ProductServiceResult<UserCouponVo>> {
    const mutation = `
      mutation ClaimCoupon($couponTemplateId: PrimaryId!) {
        claimCoupon(couponTemplateId: $couponTemplateId) {
          id
          name
          couponType
          thresholdCents
          discountAmountCents
          discountRatePermille
          scope
          productId
          status
          expireTime
          usable
        }
      }
    `

    try {
      const variables = { couponTemplateId }
      const response = await networkManager.mutate<{ claimCoupon: UserCouponVo }>(
        mutation,
        variables
      )
      return {
        success: true,
        data: response.claimCoupon
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '领取优惠券失败')
      }
    }
  }

  /**
   * 提取 GraphQL/网络错误信息
   */
  private extractError(error: unknown, fallbackMessage: string): ProductServiceError {
    if (error && typeof error === 'object' && 'errors' in error) {
      const graphqlErrors = (error as any).errors
      if (Array.isArray(graphqlErrors) && graphqlErrors.length > 0) {
        const firstError = graphqlErrors[0]
        return {
          message: firstError.message || fallbackMessage,
          code: firstError.extensions?.code
        }
      }
    } else if (error instanceof Error) {
      return {
        message: error.message || fallbackMessage
      }
    }
    return {
      message: fallbackMessage
    }
  }
}

export default CouponService
