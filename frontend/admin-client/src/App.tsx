import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import PrivateRoute from './components/PrivateRoute';
import PageTitle from './components/PageTitle';
import Login from './pages/login';
import Dashboard from './pages/dashboard';
import ProductListPage from './pages/products/list';
import ProductDetailPage from './pages/products/detail';
import Orders from './pages/orders';
import OrderDetailPage from './pages/orders/detail';
import Statistics from './pages/statistics';
import UserStatistics from './pages/statistics/users';
import DeliveryWorkerStatistics from './pages/statistics/delivery-workers';
import OrderStatistics from './pages/statistics/orders';
import ProductStatistics from './pages/statistics/products';
import UserDetailPage from './pages/users/detail/index';
import DeliveryWorkerDetailPage from './pages/users/delivery-workers/detail/index';
import AdminUsers from './pages/users/admins';
import DeliveryWorkerUsers from './pages/users/delivery-workers';
import CustomerUsers from './pages/users/customers';
import AddressListPage from './pages/addresses/list';
import ApiLogs from './pages/logs/api';
import UserActionLogs from './pages/logs/user-actions';
import BusinessLogs from './pages/logs/business';
import DailyCollection from './pages/daily-collection';
import OpsBanners from './pages/ops/banners';
import OpsFeedbacks from './pages/ops/feedbacks';
import OpsNotices from './pages/ops/notices';
import OpsStoreProfile from './pages/ops/store-profile';
import OpsDeliveryFeeRule from './pages/ops/delivery-fee-rule';

// 浏览器标签页标题由 react-helmet-async 统一管理,与 Layout 菜单文案保持一致;
// 页面标题属于实现细节,不在 PRD 中逐页描述。
const page = (title: string, element: React.ReactNode): React.ReactNode => (
  <>
    <PageTitle title={title} />
    {element}
  </>
);

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/login" element={page('登录', <Login />)} />
      <Route
        path="/"
        element={
          <PrivateRoute>
            <Layout />
          </PrivateRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={page('仪表盘', <Dashboard />)} />
        <Route path="users/admins" element={page('管理员', <AdminUsers />)} />
        <Route path="users/delivery-workers" element={page('送水员', <DeliveryWorkerUsers />)} />
        <Route path="users/customers" element={page('用户', <CustomerUsers />)} />
        <Route path="users" element={<Navigate to="/users/admins" replace />} />
        <Route path="users/:id" element={page('用户详情', <UserDetailPage />)} />
        <Route path="users/delivery-workers/:id" element={page('送水员详情', <DeliveryWorkerDetailPage />)} />
        <Route path="products" element={page('产品管理', <ProductListPage />)} />
        <Route path="products/:id" element={page('产品详情', <ProductDetailPage />)} />
        <Route path="orders" element={page('订单管理', <Orders />)} />
        <Route path="orders/:id" element={page('订单详情', <OrderDetailPage />)} />
        <Route path="statistics" element={page('营收统计', <Statistics />)} />
        <Route path="statistics/users" element={page('用户统计', <UserStatistics />)} />
        <Route path="statistics/delivery-workers" element={page('送水员统计', <DeliveryWorkerStatistics />)} />
        <Route path="statistics/orders" element={page('订单统计', <OrderStatistics />)} />
        <Route path="statistics/products" element={page('商品统计', <ProductStatistics />)} />
        <Route path="addresses" element={page('地址管理', <AddressListPage />)} />
        <Route path="logs/api" element={page('API日志', <ApiLogs />)} />
        <Route path="logs/user-actions" element={page('用户操作', <UserActionLogs />)} />
        <Route path="logs/business" element={page('业务日志', <BusinessLogs />)} />
        <Route path="daily-collection" element={page('收银对账', <DailyCollection />)} />
        <Route path="ops/banners" element={page('首页轮播', <OpsBanners />)} />
        <Route path="ops/feedbacks" element={page('意见反馈', <OpsFeedbacks />)} />
        <Route path="ops/notices" element={page('公告', <OpsNotices />)} />
        <Route path="ops/store-profile" element={page('店铺信息', <OpsStoreProfile />)} />
        <Route path="ops/delivery-fee-rule" element={page('运费规则', <OpsDeliveryFeeRule />)} />
      </Route>
    </Routes>
  );
};

export default App;
