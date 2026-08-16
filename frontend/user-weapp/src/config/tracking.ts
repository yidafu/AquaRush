/**
 * 用户操作日志 - 页面埋点配置(weapp)
 *
 * 集中维护「页面路由 → 页面名/附加属性」映射,供 usePageViewTracking 上报 PAGE_VIEW。
 * 未命中映射的路由也会以上报原始路由兜底,不丢数据。
 * 类型定义与上报核心见 @aquarush/common/tracking。
 */
import type { PageTrackConfig } from '@aquarush/common/tracking'

export const PAGE_TRACK_CONFIG: Record<string, PageTrackConfig> = {
  '/pages/home/index': { title: '首页', props: { tab: 'home' } },
  '/pages/product-detail/index': { title: '商品详情' },
  '/pages/order-confirm/index': { title: '订单确认' },
  '/pages/address-list/index': { title: '地址列表' },
  '/pages/address-edit/index': { title: '地址编辑' },
  '/pages/order-list/index': { title: '订单列表', props: { tab: 'order' } },
  '/pages/order-detail/index': { title: '订单详情' },
  '/pages/delivery-tracking/index': { title: '配送跟踪' },
  '/pages/my/index': { title: '我的', props: { tab: 'my' } },
  '/pages/profile-edit/index': { title: '个人资料' },
  '/pages/about/index': { title: '关于我们' },
  '/pages/feedback/index': { title: '意见反馈' },
  '/pages/feedback-records/index': { title: '反馈记录' },
  '/pages/favorites/index': { title: '我的收藏' },
  '/pages/notice-detail/index': { title: '公告详情' },
  '/pages/coupon-center/index': { title: '领券中心' },
  '/pages/coupon-list/index': { title: '我的优惠券' },
  '/pages/ticket-shop/index': { title: '水票商城' },
  '/pages/ticket-list/index': { title: '我的水票' },
}
