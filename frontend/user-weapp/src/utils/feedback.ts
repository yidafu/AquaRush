/**
 * 意见反馈展示工具(用户端,PRD 07-user.md §F-USR-005 / 12-operations-config.md §F-OPS-003)
 * 反馈类型/状态中文标签与状态色,供意见反馈页、我的-反馈记录页复用;
 * 与后台管理页(FEEDBACK_TYPE_LABELS/FEEDBACK_STATUS_OPTIONS)文案一致。
 */
import { FeedbackStatus, FeedbackType } from '@aquarush/common'

/**
 * 反馈类型标签(与反馈页胶囊选项一致)
 */
export const getFeedbackTypeLabel = (type: FeedbackType): string => {
  switch (type) {
    case FeedbackType.PRODUCT_QUALITY:
      return '产品质量问题'
    case FeedbackType.DELIVERY_SERVICE:
      return '配送服务问题'
    case FeedbackType.APP_USAGE:
      return '小程序使用问题'
    case FeedbackType.SUGGESTION:
      return '功能建议'
    case FeedbackType.COMPLAINT:
      return '投诉建议'
    case FeedbackType.OTHER:
      return '其他问题'
    default:
      return '其他问题'
  }
}

/**
 * 反馈状态标签(待处理/处理中/已处理)
 */
export const getFeedbackStatusLabel = (status: FeedbackStatus): string => {
  switch (status) {
    case FeedbackStatus.PENDING:
      return '待处理'
    case FeedbackStatus.PROCESSING:
      return '处理中'
    case FeedbackStatus.RESOLVED:
      return '已处理'
    default:
      return ''
  }
}

/**
 * 反馈状态色(与后台 Tag 着色一致:待处理橙/处理中蓝/已处理绿)
 */
export const getFeedbackStatusColor = (status: FeedbackStatus): string => {
  switch (status) {
    case FeedbackStatus.PENDING:
      return '#fa8c16'
    case FeedbackStatus.PROCESSING:
      return '#1890ff'
    case FeedbackStatus.RESOLVED:
      return '#52c41a'
    default:
      return '#999999'
  }
}
