import { networkManager } from '../utils/networkManager'
import {
  MyOrderStatistics,
  Order,
  OrderReviewCheckResponse,
  OrderStatus,
  OrderVoPage,
  OrderPreviewVo,
  Review
} from '@aquarush/common'
import {
  ProductServiceError,
  ProductServiceResult
} from '../types/product'

/**
 * 确认订单计价预览请求参数(F-ORD-023)
 * preferredCouponId:省略=引擎自动选最优,0=不用券;ticketRedeemCount:水票核销张数
 */
/** 订单商品明细项(F-ORD-004 多商品下单;单商品=单元素) */
export interface OrderItemInput {
  productId: string
  quantity: number
}

export interface OrderPreviewRequest {
  items: OrderItemInput[]
  addressId: string | number
  preferredCouponId?: string | number | null
  ticketRedeemCount?: number
}

/**
 * 发起订单支付结果(F-ORD-005)
 * autoCompleted=true 表示测试环境假支付已自动完成
 */
export interface OrderPaymentResult {
  paymentParams: Record<string, any> | null
  requiresSimulation: boolean
  autoCompleted: boolean
}

/**
 * 创建订单请求参数(CreateOrderInput)
 * clientRequestId:服务端下单幂等键(R-ORD-06),确认页会话内生成一次,提交失败重试沿用
 * deliveryTime:配送时段(AC-ORD-001-05,随单落库;缺省=立即配送)
 */
export interface CreateOrderRequest {
  items: OrderItemInput[]
  addressId: string | number
  clientRequestId?: string
  deliveryTime?: 'IMMEDIATE' | 'MORNING' | 'AFTERNOON' | 'EVENING'
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
          items {
            productId
            quantity
            unitPriceCents
            priceSource
            catalogPriceCents
            subtotalCents
            ticketCoveredQuantity
            cashSubtotalCents
          }
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
          depositCents
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
   * 发起订单支付(F-ORD-005):
   * - autoCompleted=true:测试环境假支付已自动完成,无需调起微信支付;
   * - requiresSimulation=true:支付未接入(未配置商户参数),前端提示;
   * - 否则 paymentParams 为 Taro.requestPayment 所需参数。
   */
  public async createOrderPayment(
    orderId: string
  ): Promise<ProductServiceResult<OrderPaymentResult>> {
    const mutation = `
      mutation CreateOrderPayment($orderId: PrimaryId!) {
        createOrderPayment(orderId: $orderId) {
          paymentParams
          requiresSimulation
          autoCompleted
        }
      }
    `

    try {
      const response = await networkManager.mutate<{ createOrderPayment: OrderPaymentResult }>(
        mutation,
        { orderId }
      )
      return {
        success: true,
        data: response.createOrderPayment
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '发起支付失败')
      }
    }
  }

  /**
   * 订单详情(仅本人可见,F-ORD-003):支付结果回显与「去支付」依赖真实订单状态
   */
  public async getOrder(id: string): Promise<ProductServiceResult<Order>> {
    const query = `
      query Order($id: PrimaryId!) {
        order(id: $id) {
          id
          orderNo
          status
          amountCents
          deliveryFeeCents
          depositCents
          payAmountCents
          # 营销计价快照(R-MKT-01/02/07/08:费用明细「满减/首单立减/券/水票」行展示依据)
          promotionDiscountCents
          promotionActivityName
          firstOrderDiscountCents
          firstOrderActivityName
          couponDiscountCents
          couponName
          ticketRedeemCount
          ticketDiscountCents
          giftItems {
            productId
            productName
            quantity
          }
          paymentTransactionId
          paymentTime
          deliveryStartedAt
          completedAt
          createdAt
          remark
          deliveryTimeSlot
          items {
            productId
            quantity
            unitPriceCents
            totalPriceCents
            product {
              id
              name
              priceCents
              coverImageUrl
              specification
            }
          }
          address {
            id
            receiverName
            phone
            province
            city
            district
            detailAddress
            floor
          }
        }
      }
    `

    try {
      const response = await networkManager.query<{ order: Order }>(query, { id })
      return {
        success: true,
        data: response.order
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取订单详情失败')
      }
    }
  }

  /**
   * 创建订单(水票核销/优惠券原子核销由服务端完成;
   * clientRequestId 幂等 R-ORD-06,deliveryTime 配送时段落库 AC-ORD-001-05)
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
   * 我的订单列表(F-ORD-002,G02 真实化):仅本人订单;statuses 为空=全部;
   * 「待配送」Tab 传 [PENDING_DISPATCH, PENDING_DELIVERY];
   * Long 字段传输为字符串,展示层经 normalizeCents 归一化。
   */
  public async getMyOrders(
    statuses: OrderStatus[] | null,
    page: number,
    size: number
  ): Promise<ProductServiceResult<OrderVoPage>> {
    const query = `
      query MyOrders($statuses: [OrderStatus!], $page: Int!, $size: Int!) {
        myOrders(statuses: $statuses, page: $page, size: $size) {
          content {
            id
            orderNo
            status
            amountCents
            deliveryFeeCents
            payAmountCents
            promotionDiscountCents
            promotionActivityName
            couponDiscountCents
            couponName
            firstOrderDiscountCents
            firstOrderActivityName
            ticketRedeemCount
            ticketDiscountCents
            createdAt
            remark
            items {
              productId
              quantity
              unitPriceCents
              totalPriceCents
              product {
                id
                name
                priceCents
                coverImageUrl
                specification
              }
            }
            address {
              id
              receiverName
              phone
              province
              city
              district
              detailAddress
            }
          }
          totalElements
          totalPages
          size
          number
        }
      }
    `

    try {
      const response = await networkManager.query<{ myOrders: OrderVoPage }>(
        query,
        { statuses, page, size }
      )
      return {
        success: true,
        data: response.myOrders
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取订单列表失败')
      }
    }
  }

  /**
   * 取消订单(F-ORD-006):未支付即时取消恢复库存;已支付(待派单)取消自动退款
   */
  public async cancelOrder(orderId: string): Promise<ProductServiceResult<Order>> {
    const mutation = `
      mutation CancelOrder($orderId: PrimaryId!) {
        cancelOrder(orderId: $orderId) {
          id
          orderNo
          status
        }
      }
    `

    try {
      const response = await networkManager.mutate<{ cancelOrder: Order }>(
        mutation,
        { orderId }
      )
      return {
        success: true,
        data: response.cancelOrder
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '取消订单失败')
      }
    }
  }

  /**
   * 催单(F-ORD-008):仅本人配送中订单;服务端防抖,短时间重复点击仅一次生效
   */
  public async urgeOrder(orderId: string): Promise<ProductServiceResult<boolean>> {
    const mutation = `
      mutation UrgeOrder($orderId: PrimaryId!) {
        urgeOrder(orderId: $orderId)
      }
    `

    try {
      const response = await networkManager.mutate<{ urgeOrder: boolean }>(
        mutation,
        { orderId }
      )
      return {
        success: true,
        data: response.urgeOrder
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '催单失败')
      }
    }
  }

  /**
   * 订单评价检查(F-ORD-007):返回已评价内容与是否可评(仅已完成订单)
   */
  public async getReviewCheck(orderId: string): Promise<ProductServiceResult<OrderReviewCheckResponse>> {
    const query = `
      query OrderReviewCheck($orderId: PrimaryId!) {
        orderReviewCheck(orderId: $orderId) {
          hasReviewed
          canReview
          review {
            reviewId
            rating
            comment
            isAnonymous
            createdAt
          }
        }
      }
    `

    try {
      const response = await networkManager.query<{ orderReviewCheck: OrderReviewCheckResponse }>(
        query,
        { orderId }
      )
      return {
        success: true,
        data: response.orderReviewCheck
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取评价信息失败')
      }
    }
  }

  /**
   * 提交订单评价(F-ORD-007,R-EVL-01 一单一评):评分 1-5 星、评论 ≤500 字、可匿名
   */
  public async createReview(input: {
    orderId: string
    rating: number
    comment?: string
    isAnonymous?: boolean
  }): Promise<ProductServiceResult<Review>> {
    const mutation = `
      mutation CreateReview($input: CreateReviewRequest!) {
        createReview(input: $input) {
          reviewId
          orderId
          rating
          comment
          isAnonymous
          createdAt
        }
      }
    `

    try {
      const response = await networkManager.mutate<{ createReview: Review }>(
        mutation,
        {
          input: {
            orderId: input.orderId,
            rating: input.rating,
            comment: input.comment || null,
            isAnonymous: input.isAnonymous || false
          }
        }
      )
      return {
        success: true,
        data: response.createReview
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '评价提交失败')
      }
    }
  }

  /**
   * 我的订单状态计数(F-USR-002,G31):我的页订单角标真实数据,
   * 单请求返回当前用户各状态订单数(仅本人,服务端强制)。
   */
  public async getMyOrderStatistics(): Promise<ProductServiceResult<MyOrderStatistics>> {
    const query = `
      query MyOrderStatistics {
        myOrderStatistics {
          total
          pendingPayment
          pendingDispatch
          pendingDelivery
          delivering
          completed
          cancelled
          refunded
        }
      }
    `

    try {
      const response = await networkManager.query<{ myOrderStatistics: MyOrderStatistics }>(query)
      return {
        success: true,
        data: response.myOrderStatistics
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取订单统计失败')
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