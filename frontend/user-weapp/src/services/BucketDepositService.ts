import { networkManager } from '../utils/networkManager'
import Taro from '@tarojs/taro'

export interface BucketDeposit {
  id: string
  userId: string
  quantity: number
  amountCents: number
  status: 'DEPOSITED' | 'REFUNDED'
  paymentTransactionId?: string
  paymentTime?: string
  refundedAt?: string
  refundedBy?: string
  remark?: string
  createdAt: string
  updatedAt: string
}

export interface PageInfo {
  total: number
  pageSize: number
  pageNum: number
  hasNext: boolean
  hasPrevious: boolean
  totalPages: number
}

export interface BucketDepositPage {
  list: BucketDeposit[]
  pageInfo: PageInfo
}

class BucketDepositService {
  static getInstance(): BucketDepositService {
    // @ts-ignore
    if (!this.instance) {
      // @ts-ignore
      this.instance = new BucketDepositService()
    }
    // @ts-ignore
    return this.instance
  }

  /**
   * 获取当前用户的押桶记录列表
   */
  async getBucketDeposits(): Promise<BucketDeposit[]> {
    const query = `
      query {
        myBucketDeposits {
          id
          userId
          quantity
          amountCents
          status
          paymentTransactionId
          paymentTime
          refundedAt
          refundedBy
          remark
          createdAt
          updatedAt
        }
      }
    `
    const result = await networkManager.query<{ myBucketDeposits: BucketDeposit[] }>(query, {})
    return result.myBucketDeposits
  }

  /**
   * 获取当前用户的押桶记录列表（分页）
   */
  async getBucketDepositsPaged(page: number = 0, size: number = 20): Promise<BucketDepositPage> {
    const query = `
      query MyBucketDepositsPaged($page: Int, $size: Int) {
        myBucketDepositsPaged(page: $page, size: $size) {
          list {
            id
            userId
            quantity
            amountCents
            status
            paymentTransactionId
            paymentTime
            refundedAt
            refundedBy
            remark
            createdAt
            updatedAt
          }
          pageInfo {
            total
            pageSize
            pageNum
            hasNext
            hasPrevious
            totalPages
          }
        }
      }
    `
    const result = await networkManager.query<{ myBucketDepositsPaged: BucketDepositPage }>(query, { page, size })
    return result.myBucketDepositsPaged
  }

  /**
   * 获取当前用户的有效押桶数量
   */
  async getActiveBucketCount(): Promise<number> {
    const query = `
      query {
        myActiveBucketCount
      }
    `
    const result = await networkManager.query<{ myActiveBucketCount: number }>(query, {})
    return result.myActiveBucketCount
  }

  /**
   * 获取押桶金额配置
   */
  async getBucketDepositAmount(): Promise<number> {
    const query = `
      query {
        bucketDepositAmount
      }
    `
    const result = await networkManager.query<{ bucketDepositAmount: number }>(query, {})
    return result.bucketDepositAmount
  }

  /**
   * 创建押桶记录
   */
  async createBucketDeposit(quantity: number): Promise<{ depositId: string; paymentParams: any }> {
    // 获取用户 OpenID
    const openId = Taro.getStorageSync('openId') || ''

    const mutation = `
      mutation CreateBucketDeposit($quantity: Int!, $openId: String!) {
        createBucketDeposit(quantity: $quantity, openId: $openId) {
          id
          userId
          quantity
          amountCents
          status
          createdAt
        }
      }
    `
    const result = await networkManager.query<{ createBucketDeposit: BucketDeposit }>(mutation, { quantity, openId })
    return {
      depositId: result.createBucketDeposit.id,
      paymentParams: {} // 支付参数需要从后端返回
    }
  }

  /**
   * 处理押桶支付成功回调
   */
  async handlePaymentSuccess(depositId: string, transactionId: string): Promise<boolean> {
    const mutation = `
      mutation HandleBucketDepositPaymentSuccess($depositId: Long!, $transactionId: String!) {
        handleBucketDepositPaymentSuccess(depositId: $depositId, transactionId: $transactionId)
      }
    `
    const result = await networkManager.query<{ handleBucketDepositPaymentSuccess: boolean }>(mutation, {
      depositId: parseInt(depositId),
      transactionId
    })
    return result.handleBucketDepositPaymentSuccess
  }
}

export default BucketDepositService.getInstance()
