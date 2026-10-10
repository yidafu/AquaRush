import { networkManager } from '../utils/networkManager'
import {
  BarrelAccountVo,
  DepositRefundApplicationVo,
  DepositRefundApplicationStatus,
  DepositRefundApplicationVoPage,
  SubmitDepositRefundInput
} from '@aquarush/common'
import {
  ProductServiceError,
  ProductServiceResult
} from '../types/product'

/**
 * 水桶账户服务(用户端,F-PRO-010 水桶账户与退押 / R-MON-03 押金桶)
 * 桶账查询(押金合计/欠桶/按规格在租)、押金记录分页、退押申请提交与记录;
 * Long 字段传输为字符串,展示层经 normalizeCents 归一化。
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
   * 水桶账户(F-PRO-010):押金合计 / 欠桶合计 / 按规格桶账 + 押金记录分页
   * @param page 押金记录页码(0 起)
   * @param size 每页押金记录条数
   */
  public async getBarrelAccount(
    page: number = 0,
    size: number = 10
  ): Promise<ProductServiceResult<BarrelAccountVo>> {
    const query = `
      query BarrelAccount($page: Int, $size: Int) {
        barrelAccount(page: $page, size: $size) {
          depositHeldCents
          owedBucketCount
          recordTotal
          specs {
            specification
            heldBucketCount
            owedBucketCount
            depositHeldCents
          }
          records {
            id
            orderId
            specification
            bucketCount
            unitDepositCents
            depositAmountCents
            status
            collectedAt
            refundedAt
            refundRemark
            createdAt
          }
        }
      }
    `

    try {
      const response = await networkManager.query<{ barrelAccount: BarrelAccountVo }>(query, {
        page,
        size
      })
      return {
        success: true,
        data: response.barrelAccount
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取水桶账户失败')
      }
    }
  }

  /**
   * 退押申请记录(F-PRO-010:状态机 待处理 → 已回收 → 已退还 / 已驳回)
   * @param status 状态筛选(缺省查全部)
   */
  public async getDepositRefundApplications(
    page: number = 0,
    size: number = 10,
    status?: DepositRefundApplicationStatus
  ): Promise<ProductServiceResult<DepositRefundApplicationVoPage>> {
    const query = `
      query DepositRefundApplications($status: DepositRefundApplicationStatus, $page: Int, $size: Int) {
        depositRefundApplications(status: $status, page: $page, size: $size) {
          content {
            id
            specification
            bucketCount
            depositCents
            receiverName
            receiverPhone
            receiverDetail
            remark
            status
            rejectReason
            recoveredAt
            refundedAt
            rejectedAt
            createdAt
          }
          totalElements
          totalPages
          size
          number
        }
      }
    `

    try {
      const response = await networkManager.query<{
        depositRefundApplications: DepositRefundApplicationVoPage
      }>(query, { status: status ?? null, page, size })
      return {
        success: true,
        data: response.depositRefundApplications
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '获取退押申请记录失败')
      }
    }
  }

  /**
   * 提交退押申请(F-PRO-010):桶数 ≤ 该规格在租桶数由页面校验;
   * 提交后进入「待处理」,门店回收空桶并确认后退还押金。
   */
  public async submitDepositRefund(
    input: SubmitDepositRefundInput
  ): Promise<ProductServiceResult<DepositRefundApplicationVo>> {
    const mutation = `
      mutation SubmitDepositRefund($input: SubmitDepositRefundInput!) {
        submitDepositRefund(input: $input) {
          id
          specification
          bucketCount
          depositCents
          status
          createdAt
        }
      }
    `

    try {
      const response = await networkManager.mutate<{
        submitDepositRefund: DepositRefundApplicationVo
      }>(mutation, { input })
      return {
        success: true,
        data: response.submitDepositRefund
      }
    } catch (error) {
      return {
        success: false,
        error: this.extractError(error, '提交退押申请失败')
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

export default BarrelService
