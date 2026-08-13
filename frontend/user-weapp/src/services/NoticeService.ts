import { networkManager } from '../utils/networkManager'
import { Notice } from '@aquarush/common'
import { ConfigServiceError, ConfigServiceResult } from './ConfigService'

export type NoticeServiceResponse<T> = ConfigServiceResult<T> | ConfigServiceError

/**
 * 公告查询服务(用户端,F-OPS-002)
 * 首页公告栏/公告详情页动态读取;已发布且在时间窗内、置顶优先/发布时间倒序由服务端完成(R-OPS-01)。
 */
class NoticeService {
  private static instance: NoticeService

  private constructor() {
    // Use shared networkManager from networkManager.ts
  }

  public static getInstance(): NoticeService {
    if (!NoticeService.instance) {
      NoticeService.instance = new NoticeService()
    }
    return NoticeService.instance
  }

  /**
   * 获取当前生效的公告列表(置顶优先、发布时间倒序;无公告返回空数组,用户端隐藏公告栏)
   */
  public async getActiveNotices(): Promise<NoticeServiceResponse<Notice[]>> {
    const query = `
      query ActiveNotices {
        activeNotices {
          id
          title
          content
          type
          pinned
          status
          publishedAt
        }
      }
    `

    try {
      const response = await networkManager.query<{ activeNotices: Notice[] }>(query)
      return {
        success: true,
        data: response.data?.activeNotices ?? []
      }
    } catch (error: any) {
      console.error('Get active notices failed:', error)
      return {
        success: false,
        error: {
          message: error?.message || '获取公告失败',
          code: error?.code
        }
      }
    }
  }
}

export default NoticeService.getInstance()
