import { networkManager } from '../utils/networkManager'
import {
  Order,
  OrderPreviewVo
} from '@aquarush/common'
import {
  ProductServiceError,
  ProductServiceResult
} from '../types/product'

/**
 * 确认订单计价预览请求参数(F-ORD-023)
 * preferredCouponId:省略=引擎自动选最优,0=不用券;ticketRedeemCount:水票核销张数
 */
export interface OrderPreviewRequest {
  productId: string
  quantity: number
  addressId: string | number
  preferredCouponId?: string | number | null
  ticketRedeemCount?: number
}

/**
 * 创建订单请求参数(CreateOrderInput)
 */
export interface CreateOrderRequest {
  productId: string
  addressId: string | number
  quantity: number
  preferredCouponId?: string | number | null
  ticketRedeemCount?: number
  remark?: string
}

/**
 * 订单服务(用户端)
 * 确认订单页计价预览(服务端计算 R-MON-01,促销引擎 R-MKT-02/07/08 全链路)与创建订单。
 * Long 字段传输为字符串,展示层经 normalizeCents 归一化。
 */
class OrderService {
  private static instance: OrderService

  private constructor() {
    // Use shared networkManager from networkManager.ts
  }

  public static getInstance(): OrderService {
    if (!OrderService.instance) {
      OrderService.instance = new OrderService()
    }
    return OrderService.instance
  }

  /**
   * 确认订单计价预览:服务端统一计算成交单价/水票抵扣/促销/优惠券/配送费/实付
   */
  public async getOrderPreview(
    input: OrderPreviewRequest
  ): Promise<ProductServiceResult<OrderPreviewVo>> {
    const query = `
      query OrderPreview($input: OrderPreviewInput!) {
        orderPreview(input: $input) {
          unitPriceCents
          priceSource
          subtotalCents
          ticketRedeemCount
          ticketCoveredQuantity
          cashSubtotalCents
          promotionDiscountCents
          promotionActivityId
          promotionActivityName
          firstOrderDiscountCents
          firstOrderActivityId
          firstOrderActivityName
          couponDiscountCents
          selectedCouponId
          selectedCouponName
          totalDiscountCents
          payableProductCents
          deliveryFeeCents
          payAmountCents
          availableTicketCount
          couponOptions {
            couponId
            name
            discountCents
            usable
            isBest
          }
          availableCoupons {
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
          giftItems {
            productId
            productName
            quantity
          }
        }
      }
    `

    try {
      const variables = { input }
      const response = await networkManager.query<{ orderPreview: OrderPreviewVo }>(
        query,
        variables
      )
      return {
        success: true,
        data: response.orderPreview
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取订单计价失败')
      }
    }
  }

  /**
   * 创建订单(水票核销/优惠券原子核销由服务端完成)
   */
  public async createOrder(
    input: CreateOrderRequest
  ): Promise<ProductServiceResult<Order>> {
    const mutation = `
      mutation CreateOrder($input: CreateOrderInput!) {
        createOrder(input: $input) {
          id
          orderNo
          status
          amountCents
          payAmountCents
        }
      }
    `

    try {
      const variables = { input }
      const response = await networkManager.mutate<{ createOrder: Order }>(
        mutation,
        variables
      )
      return {
        success: true,
        data: response.createOrder
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '创建订单失败')
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

export default OrderService
