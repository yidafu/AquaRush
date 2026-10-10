import React from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { Layout as AntLayout, Menu, Dropdown, Avatar, message } from 'antd';
import type { MenuProps } from 'antd';
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
} from '@ant-design/icons';

const { Header, Sider, Content } = AntLayout;

const Layout: React.FC = () => {
  const navigate = useNavigate();

  // 埋点:路由变化上报 PAGE_VIEW
  usePageViewTracking();

  // 仅超级管理员可管理管理员账号（后端以 PERMISSION_ADMIN_USER_* 强制，前端菜单隐藏为 UX 层）
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const isSuperAdmin = userInfo.role === 'SUPER_ADMIN';

  const menuItems = [
    { key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
    {
      key: 'users',
      icon: <UserOutlined />,
      label: '用户管理',
      children: [
        // 管理员与送水员合并为账号管理(spec/prd/07-user.md §F-USR-020/021 单表架构);
        // 超管查看全部账号,普通管理员仅见配送能力账号(后端权限隔离)
        { key: '/users/accounts', icon: <TeamOutlined />, label: '账号' },
        { key: '/users/customers', icon: <UserOutlined />, label: '用户' },
      ]
    },
    {
      key: 'products',
      icon: <ShoppingOutlined />,
      label: '产品管理',
      children: [
        { key: '/products', icon: <ShoppingOutlined />, label: '商品列表' },
        // 规格/水源地下拉选项的配置字典(F-PRO-009)
        { key: '/products/options', icon: <SettingOutlined />, label: '规格/水源地配置' },
      ],
    },
    { key: '/addresses', icon: <EnvironmentOutlined />, label: '地址管理' },
    // 配送轨迹(F-ADR-012):送水工实时位置与轨迹回放,仅管理员可见(后端 PERMISSION_DELIVERY_WORKER_READ 强制)
    { key: '/trajectories', icon: <HistoryOutlined />, label: '配送轨迹' },
    { key: '/orders', icon: <FileTextOutlined />, label: '订单管理' },
    { key: '/daily-collection', icon: <DollarOutlined />, label: '收银对账' },
    // 支付管理(R-PAY-02 退款审核流 / G06 押金模块):退款申请审核 + 押金价目/押金记录/退押审核/桶账台账
    {
      key: 'payments',
      icon: <DollarOutlined />,
      label: '支付管理',
      children: [
        { key: '/payments/refunds', icon: <DollarOutlined />, label: '退款管理' },
        { key: '/barrels', icon: <SafetyOutlined />, label: '押金管理' },
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
      ]
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
      ]
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
      key: 'notifications',
      icon: <BellOutlined />,
      label: '通知与消息',
      children: [
        { key: '/notifications/messages', icon: <MessageOutlined />, label: '消息列表' },
      ]
    },
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
      ]
    },
    {
      key: 'logs',
      icon: <HistoryOutlined />,
      label: '日志管理',
      children: [
        { key: '/logs/api', icon: <AuditOutlined />, label: 'API日志' },
        { key: '/logs/user-actions', icon: <FileTextOutlined />, label: '用户操作' },
        { key: '/logs/business', icon: <FileTextOutlined />, label: '业务日志' },
      ]
    },
  ];

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

  const userMenuItems: MenuProps['items'] = [
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
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider>
        <div style={{ height: 64, color: '#fff', textAlign: 'center', lineHeight: '64px', fontSize: 18 }}>
          桶装水管理
        </div>
        <Menu
          theme="dark"
          mode="inline"
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <AntLayout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0 }}>管理后台</h2>
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Avatar
                src={userInfo.avatar || avatarFromName(userInfo.username || userInfo.nickname || '管理员')}
                icon={<UserOutlined />}
              />
              <span>{userInfo.username || userInfo.nickname || '管理员'}</span>
            </div>
          </Dropdown>
        </Header>
        <Content style={{ margin: '24px 16px', padding: 24, background: '#fff' }}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
};

export default Layout;
