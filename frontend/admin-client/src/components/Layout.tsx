import React from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { Layout as AntLayout, Menu, Dropdown, Avatar, message } from 'antd';
import type { MenuProps } from 'antd';
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
  InboxOutlined,
  EnvironmentOutlined,
  HistoryOutlined,
  AuditOutlined,
  DollarOutlined,
} from '@ant-design/icons';

const { Header, Sider, Content } = AntLayout;

const Layout: React.FC = () => {
  const navigate = useNavigate();

  const menuItems = [
    { key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
    {
      key: 'users',
      icon: <UserOutlined />,
      label: '用户管理',
      children: [
        { key: '/users/admins', icon: <TeamOutlined />, label: '管理员' },
        { key: '/users/delivery-workers', icon: <CarOutlined />, label: '送水员' },
        { key: '/users/customers', icon: <UserOutlined />, label: '用户' },
      ]
    },
    { key: '/products', icon: <ShoppingOutlined />, label: '产品管理' },
    { key: '/addresses', icon: <EnvironmentOutlined />, label: '地址管理' },
    { key: '/orders', icon: <FileTextOutlined />, label: '订单管理' },
    { key: '/bucket-deposits', icon: <InboxOutlined />, label: '押桶管理' },
    { key: '/daily-collection', icon: <DollarOutlined />, label: '收银对账' },
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

  const handleLogout = () => {
    // TODO: 调用登出 API
    // await logout();
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
              <Avatar icon={<UserOutlined />} />
              <span>管理员</span>
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
