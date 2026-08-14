import { networkManager } from '../utils/networkManager'
import {
  TicketPurchaseVo,
  TicketTypeVo,
  UserTicketStatus,
  UserTicketVo
} from '@aquarush/common'
import {
  ProductServiceError,
  ProductServiceResult
} from '../types/product'

/**
 * 桶水票服务(用户端,营销域 F-MKT-007)
 * 水票商城 / 我的水票 / 购买水票;Long 字段传输为字符串,展示层经 normalizeCents 归一化。
 */
class TicketService {
  private static instance: TicketService

  private constructor() {
    // Use shared networkManager from networkManager.ts
  }

  public static getInstance(): TicketService {
    if (!TicketService.instance) {
      TicketService.instance = new TicketService()
    }
    return TicketService.instance
  }

  /**
   * 水票商城:在售票种列表(仅上架票种)
   */
  public async getTicketShop(): Promise<ProductServiceResult<TicketTypeVo[]>> {
    const query = `
      query TicketShop {
        ticketShop {
          id
          name
          ticketType
          productId
          specification
          packageCount
          packagePriceCents
          giftCount
          validityType
          validFrom
          validTo
          validDays
          perUserLimit
          myPurchasedCount
        }
      }
    `

    try {
      const response = await networkManager.query<{ ticketShop: TicketTypeVo[] }>(query)
      return {
        success: true,
        data: response.ticketShop ?? []
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取水票商城列表失败')
      }
    }
  }

  /**
   * 我的水票:按状态过滤(空=全部)
   * @param status 水票状态(UserTicketStatus),省略返回全部
   */
  public async getMyTickets(
    status?: UserTicketStatus
  ): Promise<ProductServiceResult<UserTicketVo[]>> {
    const query = `
      query MyTickets($status: UserTicketStatus) {
        myTickets(status: $status) {
          id
          ticketTypeName
          specification
          status
          expireTime
          isGift
        }
      }
    `

    try {
      const variables = status ? { status } : {}
      const response = await networkManager.query<{ myTickets: UserTicketVo[] }>(query, variables)
      return {
        success: true,
        data: response.myTickets ?? []
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取我的水票失败')
      }
    }
  }

  /**
   * 购买桶水票(在线支付,购票即预收款项,一经购买不支持退款 R-MKT-08)
   * @param ticketTypeId 票种 ID(PrimaryId,传输为字符串)
   * @param packageCount 购票张数(1~10)
   */
  public async purchaseTickets(
    ticketTypeId: string,
    packageCount: number
  ): Promise<ProductServiceResult<TicketPurchaseVo>> {
    const mutation = `
      mutation PurchaseTickets($input: PurchaseTicketsInput!) {
        purchaseTickets(input: $input) {
          id
          orderNo
          ticketTypeName
          totalCount
          packagePriceCents
          paymentStatus
          paymentParams
          requiresSimulation
        }
      }
    `

    try {
      const variables = {
        input: {
          ticketTypeId,
          packageCount
        }
      }
      const response = await networkManager.mutate<{ purchaseTickets: TicketPurchaseVo }>(
        mutation,
        variables
      )
      return {
        success: true,
        data: response.purchaseTickets
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '购买水票失败')
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

export default TicketService
