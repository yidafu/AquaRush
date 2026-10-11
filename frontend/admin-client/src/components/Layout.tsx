import React, { useEffect, useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Menu, Dropdown, Avatar, Breadcrumb, message } from '@/ui';
import type { DropdownMenuItem, MenuProps } from '@/ui';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { usePageViewTracking } from '../hooks/usePageViewTracking';
import { userActionTracker } from '../utils/tracking';
import { avatarFromName } from '@aquarush/common/utils';
import { logout } from '../services/auth';
import {
  DashboardOutlined,
  UserOutlined,
  TeamOutlined,
  CarOutlined,
  ShoppingOutlined,
  ShoppingCartOutlined,
  FileTextOutlined,
  BarChartOutlined,
  LogoutOutlined,
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
} from '@/ui/icons';

// Layout 骨架:StyleX div 复刻 antd Layout DOM(div > aside.sider + div > header + main),
// 底色/描边原依赖 antd theme token,现映射到共享 token(bg/card/border/side)。
const styles = stylex.create({
  root: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'row',
    // 原 antd .ant-layout 底色 colorBgLayout(spec/design/ui-design-system.md §2.3)
    backgroundColor: tokens.bg,
  },
  sider: {
    position: 'relative',
    flexGrow: '0',
    flexShrink: '0',
    flexBasis: '200px',
    width: '200px',
    minWidth: '200px',
    maxWidth: '200px',
    // 原 antd Layout Sider theme="dark" 底色 #001529
    backgroundColor: tokens.side,
    overflowY: 'auto',
  },
  siderChildren: {
    display: 'flex',
    flexDirection: 'column',
    minHeight: '100%',
  },
  main: {
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    minWidth: '0px',
    display: 'flex',
    flexDirection: 'column',
    // 原 antd 内层 Layout style={{ background: token.colorBgLayout }}
    backgroundColor: tokens.bg,
  },
  brand: {
    height: '64px',
    display: 'flex',
    alignItems: 'center',
    columnGap: '10px',
    rowGap: '10px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '16px',
    paddingRight: '16px',
    color: tokens.onBrand,
  },
  brandLogo: {
    width: '26px',
    height: '26px',
  },
  brandName: {
    fontSize: '16px',
    fontWeight: 600,
    whiteSpace: 'nowrap',
  },
  header: {
    height: '64px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '24px',
    paddingRight: '24px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    // 原 antd Header token:background colorBgContainer + borderBottom colorBorderSecondary
    backgroundColor: tokens.card,
    borderBottomWidth: '1px',
    borderBottomStyle: 'solid',
    borderBottomColor: tokens.border,
  },
  userBox: {
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
  },
  content: {
    marginTop: '24px',
    marginBottom: '24px',
    marginLeft: '24px',
    marginRight: '24px',
    paddingTop: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
    paddingRight: '24px',
  },
});

/* ===================== 侧边栏菜单三分组(spec/design/_spec/_CONTRACT.md 侧边栏菜单 / 原型 admin/*.html) =====================
 * 「经营 / 客户与营销 / 平台」三分组,仅收录 App.tsx 已实现的路由;
 * 「支付管理」分组(G93,2026-10 建):支付对账(F-ORD-031)+ 退款管理(R-PAY-02)归组,
 *   押金管理仍留「经营」组(_spec/04-order.md §页面25 未将押金纳入支付管理分组);
 * 系统设置仅超管可见(后端以 PERMISSION_SYSTEM_CONFIG_* 强制,前端菜单隐藏为 UX 层,R-SYS-03)。
 */
const GROUP_BIZ = '经营';
const GROUP_CRM = '客户与营销';
const GROUP_PLATFORM = '平台';

const buildMenuItems = (isSuperAdmin: boolean): MenuProps['items'] => [
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
const resolveCrumbs = (pathname: string): string[] => {
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

/** 收集全部叶子路由 key(以 '/' 开头) */
const collectLeafKeys = (items: MenuProps['items']): string[] =>
  (items ?? []).flatMap((item) => {
    if (!item || typeof item !== 'object') return [];
    const it = item as { key?: React.Key; children?: MenuProps['items'] };
    if (it.children?.length) return collectLeafKeys(it.children);
    return it.key && String(it.key).startsWith('/') ? [String(it.key)] : [];
  });

/** 找到 target 叶子所在的子菜单链(分组标题不是可展开项,不计入 openKeys) */
const findOpenKeys = (items: MenuProps['items'], target: string, trail: string[] = []): string[] | null => {
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

const Layout: React.FC = () => {
  const navigate = useNavigate();
  const { pathname } = useLocation();

  // 埋点:路由变化上报 PAGE_VIEW
  usePageViewTracking();

  // 仅超级管理员可管理管理员账号(后端以 PERMISSION_ADMIN_USER_* 强制,前端菜单隐藏为 UX 层)
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const isSuperAdmin = userInfo.role === 'SUPER_ADMIN';

  // 菜单在渲染期构建,保证登录后( userInfo 已写入 localStorage)系统设置分组立即可见
  const menuItems = useMemo(() => buildMenuItems(isSuperAdmin), [isSuperAdmin]);

  const leafKeys = useMemo(() => collectLeafKeys(menuItems), [menuItems]);

  // 菜单高亮随路由:精确匹配,否则取最长前缀(如 /orders/123 → /orders)
  const selectedKey = useMemo(() => {
    if (leafKeys.includes(pathname)) return pathname;
    return (
      leafKeys
        .filter((key) => pathname.startsWith(`${key}/`))
        .sort((a, b) => b.length - a.length)[0] ?? pathname
    );
  }, [leafKeys, pathname]);

  // 子菜单展开:首次按当前路由展开,路由变化时补开选中项所在子菜单;用户手动收起不强制回弹
  const [openKeys, setOpenKeys] = useState<string[]>(() => findOpenKeys(menuItems, selectedKey) ?? []);
  useEffect(() => {
    const chain = findOpenKeys(menuItems, selectedKey) ?? [];
    setOpenKeys((prev) => (chain.every((key) => prev.includes(key)) ? prev : [...prev, ...chain]));
  }, [selectedKey]);

  const crumbs = useMemo(() => resolveCrumbs(pathname), [pathname]);

  const handleLogout = async () => {
    // 调用后端登出接口(G15 落地:记录登出业务日志;JWT 无状态,服务端仅记账)
    try {
      await logout();
    } catch (e) {
      console.warn('登出接口调用失败，仍将清除本地凭证', e);
    }
    // 埋点:登出(显式携带用户上下文,避免登出清空本地存储后上报丢失归属)
    const logoutUser = JSON.parse(localStorage.getItem('userInfo') || '{}');
    userActionTracker.track('CLICK', 'logout', {
      elementType: 'menu',
      elementText: '退出登录',
      userId: logoutUser?.id != null ? String(logoutUser.id) : undefined,
      username: logoutUser?.username || logoutUser?.nickname || undefined,
    });
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    message.success('已退出登录');
    navigate('/login');
  };

  const userMenuItems: DropdownMenuItem[] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人信息',
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '设置',
    },
    {
      key: 'divider',
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  return (
    <div {...stylex.props(styles.root)}>
      <aside {...stylex.props(styles.sider)}>
        <div {...stylex.props(styles.siderChildren)}>
          <div {...stylex.props(styles.brand)}>
            <img src="/aqua-rush-mark.svg" alt="奔泉" {...stylex.props(styles.brandLogo)} />
            <span {...stylex.props(styles.brandName)}>奔泉运营后台</span>
          </div>
          <Menu
            theme="dark"
            mode="inline"
            items={menuItems}
            selectedKeys={[selectedKey]}
            openKeys={openKeys}
            onOpenChange={(keys) => setOpenKeys(keys)}
            onClick={({ key }) => navigate(key)}
          />
        </div>
      </aside>
      <div {...stylex.props(styles.main)}>
        <header {...stylex.props(styles.header)}>
          {crumbs.length > 0 && (
            <Breadcrumb items={crumbs.map((title) => ({ title }))} />
          )}
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div {...stylex.props(styles.userBox)}>
              <Avatar
                src={userInfo.avatar || avatarFromName(userInfo.username || userInfo.nickname || '管理员')}
                icon={<UserOutlined />}
              />
              <span>{userInfo.username || userInfo.nickname || '管理员'}</span>
            </div>
          </Dropdown>
        </header>
        {/* 灰底画布(spec/design/ui-design-system.md §2.3 colorBgLayout),内容白卡由页面内 Card 呈现 */}
        <main {...stylex.props(styles.content)}>
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
