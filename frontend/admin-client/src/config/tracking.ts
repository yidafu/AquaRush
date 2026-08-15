/**
 * 用户操作日志 - 页面埋点配置(admin-client)
 *
 * 集中维护「路由 → 页面名/附加属性」映射,供 usePageViewTracking 上报 PAGE_VIEW。
 * 未命中映射的路由也会以上报原始路由兜底,不丢数据。
 * 类型定义与上报核心见 @aquarush/common/tracking。
 */
import type { PageTrackConfig } from '@aquarush/common/tracking'

export const PAGE_TRACK_CONFIG: Record<string, PageTrackConfig> = {
  '/login': { title: '登录' },
  '/dashboard': { title: '工作台' },
  '/products': { title: '商品列表' },
  '/orders': { title: '订单管理' },
  '/statistics': { title: '数据统计' },
  '/statistics/users': { title: '用户统计' },
  '/statistics/orders': { title: '订单统计' },
  '/statistics/products': { title: '商品统计' },
  '/statistics/delivery-workers': { title: '配送员统计' },
  '/users': { title: '用户管理' },
  '/users/admins': { title: '管理员列表' },
  '/users/customers': { title: '客户列表' },
  '/users/delivery-workers': { title: '配送员列表' },
  '/addresses': { title: '地址管理' },
  '/logs/api': { title: 'API 日志' },
  '/logs/user-actions': { title: '用户操作日志' },
  '/logs/business': { title: '业务日志' },
  '/daily-collection': { title: '每日收款' },
  '/ops/banners': { title: '轮播图管理' },
  '/ops/feedbacks': { title: '意见反馈' },
  '/ops/notices': { title: '公告管理' },
  '/ops/store-profile': { title: '店铺资料' },
  '/ops/delivery-fee-rule': { title: '配送费规则' },
  '/marketing/customer-groups': { title: '客户分组' },
  '/marketing/group-prices': { title: '分组价格' },
  '/marketing/coupons': { title: '优惠券管理' },
  '/marketing/promotions': { title: '促销活动' },
  '/marketing/water-tickets': { title: '水票管理' },
  '/marketing/flash-sale': { title: '限时抢购' },
  '/marketing/buy-gift': { title: '买赠活动' },
  '/marketing/newcomer': { title: '新人活动' },
  '/system-config/parameters': { title: '系统参数' },
  '/system-config/order': { title: '订单配置' },
  '/system-config/stock': { title: '库存配置' },
}
