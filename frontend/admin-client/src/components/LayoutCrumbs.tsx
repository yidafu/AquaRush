import { GROUP_BIZ, GROUP_CRM, GROUP_PLATFORM } from './LayoutMenu';

/* ===================== 面包屑映射(覆盖 Layout 内全部页面路由) =====================
 * route → { 分组, 可选父级菜单, 页面标题 };':id' 为路径参数占位段,resolveCrumbs 逐段匹配。
 * 层级口径与上方菜单三分组一致,标题与 App.tsx page() 页面标题保持同文。
 */
interface RouteCrumb {
  group: string;
  parent?: string;
  title: string;
}

const ROUTE_CRUMBS: Record<string, RouteCrumb> = {
  // —— 经营 ——
  '/dashboard': { group: GROUP_BIZ, title: '工作台' },
  '/statistics': { group: GROUP_BIZ, parent: '数据统计', title: '营收统计' },
  '/statistics/users': { group: GROUP_BIZ, parent: '数据统计', title: '用户统计' },
  '/statistics/delivery-workers': { group: GROUP_BIZ, parent: '数据统计', title: '送水员统计' },
  '/statistics/orders': { group: GROUP_BIZ, parent: '数据统计', title: '订单统计' },
  '/statistics/products': { group: GROUP_BIZ, parent: '数据统计', title: '商品统计' },
  '/statistics/marketing': { group: GROUP_BIZ, parent: '数据统计', title: '营销统计' },
  '/products': { group: GROUP_BIZ, parent: '商品管理', title: '商品列表' },
  '/products/new': { group: GROUP_BIZ, parent: '商品管理', title: '新增商品' },
  '/products/options': { group: GROUP_BIZ, parent: '商品管理', title: '规格与水源地配置' },
  '/products/:id': { group: GROUP_BIZ, parent: '商品管理', title: '产品详情' },
  '/products/:id/edit': { group: GROUP_BIZ, parent: '商品管理', title: '编辑商品' },
  '/orders': { group: GROUP_BIZ, title: '订单管理' },
  '/orders/:id': { group: GROUP_BIZ, parent: '订单管理', title: '订单详情' },
  '/subscriptions': { group: GROUP_BIZ, parent: '订单管理', title: '周期订购计划' },
  '/addresses': { group: GROUP_BIZ, parent: '地址与配送', title: '地址管理' },
  '/trajectories': { group: GROUP_BIZ, parent: '地址与配送', title: '配送轨迹' },
  '/daily-collection': { group: GROUP_BIZ, title: '收银对账' },
  '/payments/reconciliation': { group: GROUP_BIZ, parent: '支付管理', title: '支付对账' },
  '/payments/refunds': { group: GROUP_BIZ, parent: '支付管理', title: '退款管理' },
  '/barrels': { group: GROUP_BIZ, title: '押金管理' },
  // —— 客户与营销 ——
  '/users/accounts': { group: GROUP_CRM, parent: '用户管理', title: '账号' },
  '/users/accounts/:id': { group: GROUP_CRM, parent: '用户管理', title: '账号详情' },
  '/users/customers': { group: GROUP_CRM, parent: '用户管理', title: '用户' },
  '/users/:id': { group: GROUP_CRM, parent: '用户管理', title: '用户详情' },
  '/marketing/customer-groups': { group: GROUP_CRM, parent: '营销管理', title: '客户分组' },
  '/marketing/group-prices': { group: GROUP_CRM, parent: '营销管理', title: '分组价格' },
  '/marketing/coupons': { group: GROUP_CRM, parent: '营销管理', title: '优惠券' },
  '/marketing/coupons/:id': { group: GROUP_CRM, parent: '营销管理', title: '优惠券详情' },
  '/marketing/promotions': { group: GROUP_CRM, parent: '营销管理', title: '满减/折扣' },
  '/marketing/promotions/:id': { group: GROUP_CRM, parent: '营销管理', title: '满减/折扣详情' },
  '/marketing/water-tickets': { group: GROUP_CRM, parent: '营销管理', title: '桶水票' },
  '/marketing/water-tickets/:id': { group: GROUP_CRM, parent: '营销管理', title: '水票详情' },
  '/marketing/flash-sale': { group: GROUP_CRM, parent: '营销管理', title: '限时特价' },
  '/marketing/flash-sale/:id': { group: GROUP_CRM, parent: '营销管理', title: '限时特价详情' },
  '/marketing/buy-gift': { group: GROUP_CRM, parent: '营销管理', title: '买赠/满赠' },
  '/marketing/buy-gift/:id': { group: GROUP_CRM, parent: '营销管理', title: '买赠/满赠详情' },
  '/marketing/newcomer': { group: GROUP_CRM, parent: '营销管理', title: '首单立减/新人价' },
  '/marketing/newcomer/:id': { group: GROUP_CRM, parent: '营销管理', title: '首单立减/新人价详情' },
  '/ops/banners': { group: GROUP_CRM, parent: '运营配置', title: '首页轮播' },
  '/ops/notices': { group: GROUP_CRM, parent: '运营配置', title: '公告' },
  '/ops/feedbacks': { group: GROUP_CRM, parent: '运营配置', title: '意见反馈' },
  '/ops/store-profile': { group: GROUP_CRM, parent: '运营配置', title: '店铺信息' },
  '/ops/delivery-fee-rule': { group: GROUP_CRM, parent: '运营配置', title: '运费规则' },
  // —— 平台 ——
  '/notifications/messages': { group: GROUP_PLATFORM, parent: '通知消息', title: '消息列表' },
  '/system-config/order': { group: GROUP_PLATFORM, parent: '系统设置', title: '订单与履约参数' },
  '/system-config/stock': { group: GROUP_PLATFORM, parent: '系统设置', title: '库存预警阈值' },
  '/system-config/security': { group: GROUP_PLATFORM, parent: '系统设置', title: '账号与安全' },
  '/system-config/notifications': { group: GROUP_PLATFORM, parent: '系统设置', title: '通知与订阅消息' },
  '/system-config/data': { group: GROUP_PLATFORM, parent: '系统设置', title: '数据维护' },
  '/logs/api': { group: GROUP_PLATFORM, parent: '日志管理', title: 'API日志' },
  '/logs/user-actions': { group: GROUP_PLATFORM, parent: '日志管理', title: '用户操作' },
  '/logs/business': { group: GROUP_PLATFORM, parent: '日志管理', title: '业务日志' },
};

/** 精确匹配优先,再按 ':id' 参数段逐段匹配(段数一致 + 非参数段全等) */
export const resolveCrumbs = (pathname: string): string[] => {
  const segs = pathname.split('/').filter(Boolean);
  const hit =
    ROUTE_CRUMBS[pathname] ??
    Object.entries(ROUTE_CRUMBS).find(([pattern]) => {
      const patternSegs = pattern.split('/').filter(Boolean);
      return (
        patternSegs.length === segs.length &&
        patternSegs.every((seg, i) => seg === segs[i] || seg.startsWith(':'))
      );
    })?.[1];
  return hit ? [hit.group, ...(hit.parent ? [hit.parent] : []), hit.title] : [];
};
