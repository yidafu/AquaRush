import { networkManager } from '../utils/networkManager'
import {
  BarrelFeeVo,
  BarrelPurchaseVo,
  BarrelPaymentStatus
} from '@aquarush/common'
import {
  ProductServiceError,
  ProductServiceResult
} from '../types/product'

/**
 * 水桶费服务(用户端,G06:一次性塑料水桶费,独立支付,每用户总限购一次)
 * 买水桶商城 / 我的水桶 / 购买水桶;Long 字段传输为字符串,展示层经 normalizeCents 归一化。
 */
class BarrelService {
  private static instance: BarrelService

  private constructor() {
    // Use shared networkManager from networkManager.ts
  }

  public static getInstance(): BarrelService {
    if (!BarrelService.instance) {
      BarrelService.instance = new BarrelService()
    }
    return BarrelService.instance
  }

  /**
   * 水桶费商城:启用价目列表(一次性塑料水桶费)
   */
  public async getBarrelShop(): Promise<ProductServiceResult<BarrelFeeVo[]>> {
    const query = `
      query BarrelShop {
        barrelShop {
          id
          specification
          priceCents
        }
      }
    `

    try {
      const response = await networkManager.query<{ barrelShop: BarrelFeeVo[] }>(query)
      return {
        success: true,
        data: response.barrelShop ?? []
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取水桶费商城失败')
      }
    }
  }

  /**
   * 我的水桶:当前用户购桶单(未购买返回 null)
   */
  public async getMyBarrelPurchase(): Promise<ProductServiceResult<BarrelPurchaseVo | null>> {
    const query = `
      query MyBarrelPurchase {
        myBarrelPurchase {
          id
          orderNo
          totalAmountCents
          paymentStatus
          paidAt
          refundedAt
          items {
            specification
            unitPriceCents
            quantity
            amountCents
          }
          createdAt
        }
      }
    `

    try {
      const response = await networkManager.query<{ myBarrelPurchase: BarrelPurchaseVo | null }>(query)
      return {
        success: true,
        data: response.myBarrelPurchase ?? null
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取我的水桶失败')
      }
    }
  }

  /**
   * 购买水桶(一次性塑料水桶费,独立支付,每用户总限购一次)
   * @param items 购桶明细[{barrelFeeId, quantity}]
   */
  public async purchaseBarrel(
    items: Array<{ barrelFeeId: string; quantity: number }>
  ): Promise<ProductServiceResult<BarrelPurchaseVo>> {
    const mutation = `
      mutation PurchaseBarrel($input: PurchaseBarrelInput!) {
        purchaseBarrel(input: $input) {
          id
          orderNo
          totalAmountCents
          paymentStatus
          paidAt
          refundedAt
          items {
            specification
            unitPriceCents
            quantity
            amountCents
          }
          paymentParams
          requiresSimulation
        }
      }
    `

    try {
      const variables = {
        input: { items }
      }
      const response = await networkManager.mutate<{ purchaseBarrel: BarrelPurchaseVo }>(
        mutation,
        variables
      )
      return {
        success: true,
        data: response.purchaseBarrel
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '购买水桶失败')
      }
    }
  }

  /**
   * 是否已购桶(每用户总限购一次判定:存在已支付购桶单)
   */
  public isPaid(purchase: BarrelPurchaseVo | null): boolean {
    return !!purchase && purchase.paymentStatus === BarrelPaymentStatus.PAID
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

export default BarrelService
