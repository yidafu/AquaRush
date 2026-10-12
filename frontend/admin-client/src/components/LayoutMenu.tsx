import React from 'react';
import type { MenuProps } from '@aquarush/admin-ui';
import {
  DashboardOutlined,
  UserOutlined,
  TeamOutlined,
  CarOutlined,
  ShoppingOutlined,
  ShoppingCartOutlined,
  FileTextOutlined,
  BarChartOutlined,
  SettingOutlined,
  EnvironmentOutlined,
  HistoryOutlined,
  AuditOutlined,
  DollarOutlined,
  UndoOutlined,
  ControlOutlined,
  PictureOutlined,
  MessageOutlined,
  SoundOutlined,
  ShopOutlined,
  CarOutlined as DeliveryCarOutlined,
  TagsOutlined,
  GiftOutlined,
  PercentageOutlined,
  SkinOutlined,
  GroupOutlined,
  ThunderboltOutlined,
  GiftTwoTone,
  RocketOutlined,
  SafetyOutlined,
  BellOutlined,
  DatabaseOutlined,
  CalendarOutlined,
} from '@aquarush/admin-ui/icons';

/* ===================== 侧边栏菜单三分组(spec/design/_spec/_CONTRACT.md 侧边栏菜单 / 原型 admin/*.html) =====================
 * 「经营 / 客户与营销 / 平台」三分组,仅收录 App.tsx 已实现的路由;
 * 「支付管理」分组(G93,2026-10 建):支付对账(F-ORD-031)+ 退款管理(R-PAY-02)归组,
 *   押金管理仍留「经营」组(_spec/04-order.md §页面25 未将押金纳入支付管理分组);
 * 系统设置仅超管可见(后端以 PERMISSION_SYSTEM_CONFIG_* 强制,前端菜单隐藏为 UX 层,R-SYS-03)。
 */
export const GROUP_BIZ = '经营';
export const GROUP_CRM = '客户与营销';
export const GROUP_PLATFORM = '平台';

export const buildMenuItems = (isSuperAdmin: boolean): MenuProps['items'] => [
  {
    type: 'group',
    label: GROUP_BIZ,
    children: [
      { key: '/dashboard', icon: <DashboardOutlined />, label: '工作台' },
      {
        key: 'statistics',
        icon: <BarChartOutlined />,
        label: '数据统计',
        children: [
          { key: '/statistics', icon: <BarChartOutlined />, label: '营收统计' },
          { key: '/statistics/users', icon: <UserOutlined />, label: '用户统计' },
          { key: '/statistics/delivery-workers', icon: <CarOutlined />, label: '送水员统计' },
          { key: '/statistics/orders', icon: <FileTextOutlined />, label: '订单统计' },
          { key: '/statistics/products', icon: <ShoppingCartOutlined />, label: '商品统计' },
          { key: '/statistics/marketing', icon: <GiftOutlined />, label: '营销统计' },
        ],
      },
      {
        key: 'products',
        icon: <ShoppingOutlined />,
        label: '商品管理',
        children: [
          { key: '/products', icon: <ShoppingOutlined />, label: '商品列表' },
          // 规格/水源地下拉选项的配置字典(F-PRO-009)
          { key: '/products/options', icon: <SettingOutlined />, label: '规格/水源地配置' },
        ],
      },
      { key: '/orders', icon: <FileTextOutlined />, label: '订单管理' },
      // 周期订购计划(F-ORD-026/G77 后台入口;菜单对全部管理员可见,权限由后端强制)
      { key: '/subscriptions', icon: <CalendarOutlined />, label: '周期订购计划' },
      {
        key: 'addresses',
        icon: <EnvironmentOutlined />,
        label: '地址与配送',
        children: [
          { key: '/addresses', icon: <EnvironmentOutlined />, label: '地址管理' },
          // 配送轨迹(F-ADR-012):送水工实时位置与轨迹回放,仅管理员可见(后端 PERMISSION_DELIVERY_WORKER_READ 强制)
          { key: '/trajectories', icon: <HistoryOutlined />, label: '配送轨迹' },
        ],
      },
      { key: '/daily-collection', icon: <DollarOutlined />, label: '收银对账' },
      // 支付管理分组(G93,2026-10 建,F-ORD-031):支付对账 + 退款管理归组(与设计稿「支付管理」组一致);
      // 押金管理仍留经营组(_spec/04-order.md §页面25 未将押金管理纳入支付管理分组)
      {
        key: 'payments',
        icon: <DollarOutlined />,
        label: '支付管理',
        children: [
          { key: '/payments/reconciliation', icon: <AuditOutlined />, label: '支付对账' },
          { key: '/payments/refunds', icon: <UndoOutlined />, label: '退款管理' },
        ],
      },
      { key: '/barrels', icon: <SafetyOutlined />, label: '押金管理' },
    ],
  },
  {
    type: 'group',
    label: GROUP_CRM,
    children: [
      {
        key: 'users',
        icon: <UserOutlined />,
        label: '用户管理',
        children: [
          // 管理员与送水员合并为账号管理(spec/prd/07-user.md §F-USR-020/021 单表架构);
          // 超管查看全部账号,普通管理员仅见配送能力账号(后端权限隔离)
          { key: '/users/accounts', icon: <TeamOutlined />, label: '账号' },
          { key: '/users/customers', icon: <UserOutlined />, label: '用户' },
        ],
      },
      {
        key: 'marketing',
        icon: <TagsOutlined />,
        label: '营销管理',
        children: [
          { key: '/marketing/customer-groups', icon: <GroupOutlined />, label: '客户分组' },
          { key: '/marketing/group-prices', icon: <DollarOutlined />, label: '分组价格' },
          { key: '/marketing/coupons', icon: <GiftOutlined />, label: '优惠券' },
          { key: '/marketing/promotions', icon: <PercentageOutlined />, label: '满减/折扣' },
          { key: '/marketing/water-tickets', icon: <SkinOutlined />, label: '桶水票' },
          { key: '/marketing/flash-sale', icon: <ThunderboltOutlined />, label: '限时特价' },
          { key: '/marketing/buy-gift', icon: <GiftTwoTone />, label: '买赠/满赠' },
          { key: '/marketing/newcomer', icon: <RocketOutlined />, label: '首单立减/新人价' },
        ],
      },
      {
        key: 'ops',
        icon: <ControlOutlined />,
        label: '运营配置',
        children: [
          { key: '/ops/banners', icon: <PictureOutlined />, label: '首页轮播' },
          { key: '/ops/notices', icon: <SoundOutlined />, label: '公告' },
          { key: '/ops/feedbacks', icon: <MessageOutlined />, label: '意见反馈' },
          { key: '/ops/store-profile', icon: <ShopOutlined />, label: '店铺信息' },
          { key: '/ops/delivery-fee-rule', icon: <DeliveryCarOutlined />, label: '运费规则' },
        ],
      },
    ],
  },
  {
    type: 'group',
    label: GROUP_PLATFORM,
    children: [
      {
        key: 'notifications',
        icon: <BellOutlined />,
        label: '通知消息',
        children: [{ key: '/notifications/messages', icon: <MessageOutlined />, label: '消息列表' }],
      },
      // 仅超级管理员可见(后端以 PERMISSION_SYSTEM_CONFIG_* 强制,前端菜单隐藏为 UX 层,R-SYS-03)
      ...(isSuperAdmin
        ? [
            {
              key: 'system',
              icon: <SettingOutlined />,
              label: '系统设置',
              children: [
                { key: '/system-config/order', icon: <ShoppingCartOutlined />, label: '订单与履约参数' },
                { key: '/system-config/stock', icon: <DollarOutlined />, label: '库存预警阈值' },
                { key: '/system-config/security', icon: <SafetyOutlined />, label: '账号与安全' },
                { key: '/system-config/notifications', icon: <BellOutlined />, label: '通知与订阅消息' },
                { key: '/system-config/data', icon: <DatabaseOutlined />, label: '数据维护' },
              ],
            },
          ]
        : []),
      {
        key: 'logs',
        icon: <HistoryOutlined />,
        label: '日志管理',
        children: [
          { key: '/logs/api', icon: <AuditOutlined />, label: 'API日志' },
          { key: '/logs/user-actions', icon: <FileTextOutlined />, label: '用户操作' },
          { key: '/logs/business', icon: <FileTextOutlined />, label: '业务日志' },
        ],
      },
    ],
  },
];

/** 收集全部叶子路由 key(以 '/' 开头) */
export const collectLeafKeys = (items: MenuProps['items']): string[] =>
  (items ?? []).flatMap((item) => {
    if (!item || typeof item !== 'object') return [];
    const it = item as { key?: React.Key; children?: MenuProps['items'] };
    if (it.children?.length) return collectLeafKeys(it.children);
    return it.key && String(it.key).startsWith('/') ? [String(it.key)] : [];
  });

/** 找到 target 叶子所在的子菜单链(分组标题不是可展开项,不计入 openKeys) */
export const findOpenKeys = (items: MenuProps['items'], target: string, trail: string[] = []): string[] | null => {
  for (const item of items ?? []) {
    if (!item || typeof item !== 'object') continue;
    const it = item as { key?: React.Key; children?: MenuProps['items'] };
    if (!it.children?.length) {
      if (it.key === target) return trail;
      continue;
    }
    const nextTrail = it.key && !String(it.key).startsWith('/') ? [...trail, String(it.key)] : trail;
    const found = findOpenKeys(it.children, target, nextTrail);
    if (found) return found;
  }
  return null;
};
