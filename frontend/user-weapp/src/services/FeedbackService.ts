import { networkManager } from '../utils/networkManager'
import FileUploadService from './FileUploadService'
import { FeedbackRecordVoPage, FeedbackType } from '@aquarush/common'

export interface FeedbackSubmitParams {
  feedbackType: FeedbackType
  content: string
  /** 本地临时文件路径(提交前经 FileUploadService 上传为 fileUrl) */
  images?: string[]
  contactInfo?: string
}

export interface FeedbackServiceResult {
  success: true
}

export interface FeedbackServiceError {
  success: false
  error: {
    message: string
    code?: string
  }
}

export type FeedbackServiceResponse = FeedbackServiceResult | FeedbackServiceError

export interface FeedbackRecordsResult {
  success: true
  data: FeedbackRecordVoPage
}

export type FeedbackRecordsResponse = FeedbackRecordsResult | FeedbackServiceError

/**
 * 意见反馈提交服务(用户端)
 * 与 PRD 07-user.md §F-USR-005 保持一致:图片先经平台存储上传,再提交反馈落库(G04 目标形态)。
 */
class FeedbackService {
  private static instance: FeedbackService

  private constructor() {
    // Use shared networkManager from networkManager.ts
  }

  public static getInstance(): FeedbackService {
    if (!FeedbackService.instance) {
      FeedbackService.instance = new FeedbackService()
    }
    return FeedbackService.instance
  }

  /**
   * 提交反馈(图片 ≤3 张,先上传再提交;提交中防重复由页面状态控制)
   */
  public async submitFeedback(params: FeedbackSubmitParams): Promise<FeedbackServiceResponse> {
    try {
      // 1. 图片上传:本地临时路径 → 平台存储 fileUrl(FileUploadService 默认导出即单例实例)
      const imageUrls: string[] = []
      if (params.images && params.images.length > 0) {
        const uploaded = await FileUploadService.uploadMultipleFiles(params.images)
        imageUrls.push(...uploaded.map((f) => f.fileUrl))
      }

      // 2. 提交反馈
      const mutation = `
        mutation SubmitFeedback($input: FeedbackSubmitInput!) {
          submitFeedback(input: $input)
        }
      `
      const variables = {
        input: {
          feedbackType: params.feedbackType,
          content: params.content,
          images: imageUrls,
          contactInfo: params.contactInfo
        }
      }

      const response = await networkManager.mutate<{ submitFeedback: boolean }>(mutation, variables)
      if (response.submitFeedback) {
        return { success: true }
      }
      return {
        success: false,
        error: { message: '提交失败,请重试' }
      }
    } catch (error: any) {
      console.error('Submit feedback failed:', error)
      return {
        success: false,
        error: {
          message: error?.message || '提交失败,请重试',
          code: error?.code
        }
      }
    }
  }

  /**
   * 我的-反馈记录(F-USR-005 处理结果回传):分页查询本人反馈,展示状态与处理结果
   * @param page 页码(0 起)
   * @param size 每页条数(默认 10)
   */
  public async getMyFeedbacks(page: number = 0, size: number = 10): Promise<FeedbackRecordsResponse> {
    const query = `
      query MyFeedbacks($page: Int, $size: Int) {
        myFeedbacks(page: $page, size: $size) {
          content {
            id
            feedbackType
            content
            images
            status
            handleResult
            handledByName
            handledAt
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
      const variables = { page, size }
      const response = await networkManager.query<{ myFeedbacks: FeedbackRecordVoPage }>(query, variables)
      return {
        success: true,
        data: response.myFeedbacks
      }
    } catch (error: any) {
      console.error('Load my feedbacks failed:', error)
      return {
        success: false,
        error: {
          message: error?.message || '获取反馈记录失败,请重试',
          code: error?.code
        }
      }
    }
  }
}

export default FeedbackService.getInstance()
