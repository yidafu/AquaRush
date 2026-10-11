import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import PrivateRoute from './components/PrivateRoute';
import PageTitle from './components/PageTitle';
import Login from './pages/login';
import Dashboard from './pages/dashboard';
import ProductListPage from './pages/products/list';
import ProductFormPage from './pages/products/form';
import ProductOptionsPage from './pages/products/options';
import Orders from './pages/orders';
import OrderDetailPage from './pages/orders/detail';
import Statistics from './pages/statistics';
import UserStatistics from './pages/statistics/users';
import DeliveryWorkerStatistics from './pages/statistics/delivery-workers';
import OrderStatistics from './pages/statistics/orders';
import ProductStatistics from './pages/statistics/products';
import MarketingStatistics from './pages/statistics/marketing';
import UserDetailPage from './pages/users/detail/index';
import AccountUsers from './pages/users/accounts';
import AccountDetailPage from './pages/users/accounts/detail/index';
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
import MarketingCustomerGroups from './pages/marketing/customer-groups';
import MarketingGroupPrices from './pages/marketing/group-prices';
import MarketingCoupons from './pages/marketing/coupons';
import MarketingCouponDetail from './pages/marketing/coupons/detail';
import MarketingPromotions from './pages/marketing/promotions';
import MarketingPromotionDetail from './pages/marketing/promotions/detail';
import MarketingWaterTickets from './pages/marketing/water-tickets';
import MarketingWaterTicketDetail from './pages/marketing/water-tickets/detail';
import MarketingFlashSale from './pages/marketing/flash-sale';
import MarketingFlashSaleDetail from './pages/marketing/flash-sale/detail';
import MarketingBuyGift from './pages/marketing/buy-gift';
import MarketingGiftDetail from './pages/marketing/buy-gift/detail';
import MarketingNewcomer from './pages/marketing/newcomer';
import MarketingNewcomerDetail from './pages/marketing/newcomer/detail';
import SystemOrderConfig from './pages/system/order';
import SystemStockConfig from './pages/system/stock';
import SystemSecurityConfig from './pages/system/security';
import SystemNotificationsConfig from './pages/system/notifications';
import NotificationMessages from './pages/notifications/messages';
import SystemDataMaintenanceConfig from './pages/system/data';
import DeliveryTrajectories from './pages/trajectories';
import RefundManagementPage from './pages/payments/refunds';
import PaymentReconciliationPage from './pages/payments/reconciliation';
import BarrelManagementPage from './pages/barrels';

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
        <Route path="users/accounts" element={page('账号管理', <AccountUsers />)} />
        <Route path="users/accounts/:id" element={page('账号详情', <AccountDetailPage />)} />
        <Route path="users/customers" element={page('用户', <CustomerUsers />)} />
        <Route path="users" element={<Navigate to="/users/accounts" replace />} />
        <Route path="users/:id" element={page('用户详情', <UserDetailPage />)} />
        <Route path="products" element={page('产品管理', <ProductListPage />)} />
        {/* 新增/编辑/详情共用一个页面组件,按路由区分模式 */}
        <Route path="products/new" element={page('新增商品', <ProductFormPage />)} />
        <Route path="products/:id" element={page('产品详情', <ProductFormPage />)} />
        <Route path="products/:id/edit" element={page('编辑商品', <ProductFormPage />)} />
        <Route path="products/options" element={page('规格与水源地配置', <ProductOptionsPage />)} />
        <Route path="orders" element={page('订单管理', <Orders />)} />
        <Route path="orders/:id" element={page('订单详情', <OrderDetailPage />)} />
        <Route path="statistics" element={page('营收统计', <Statistics />)} />
        <Route path="statistics/users" element={page('用户统计', <UserStatistics />)} />
        <Route path="statistics/delivery-workers" element={page('送水员统计', <DeliveryWorkerStatistics />)} />
        <Route path="statistics/orders" element={page('订单统计', <OrderStatistics />)} />
        <Route path="statistics/products" element={page('商品统计', <ProductStatistics />)} />
        <Route path="statistics/marketing" element={page('营销统计', <MarketingStatistics />)} />
        <Route path="addresses" element={page('地址管理', <AddressListPage />)} />
        <Route path="trajectories" element={page('配送轨迹', <DeliveryTrajectories />)} />
        <Route path="logs/api" element={page('API日志', <ApiLogs />)} />
        <Route path="logs/user-actions" element={page('用户操作', <UserActionLogs />)} />
        <Route path="logs/business" element={page('业务日志', <BusinessLogs />)} />
        <Route path="daily-collection" element={page('收银对账', <DailyCollection />)} />
        <Route path="payments/refunds" element={page('退款管理', <RefundManagementPage />)} />
        <Route path="payments/reconciliation" element={page('支付对账', <PaymentReconciliationPage />)} />
        <Route path="barrels" element={page('押金管理', <BarrelManagementPage />)} />
        <Route path="ops/banners" element={page('首页轮播', <OpsBanners />)} />
        <Route path="ops/feedbacks" element={page('意见反馈', <OpsFeedbacks />)} />
        <Route path="ops/notices" element={page('公告', <OpsNotices />)} />
        <Route path="ops/store-profile" element={page('店铺信息', <OpsStoreProfile />)} />
        <Route path="ops/delivery-fee-rule" element={page('运费规则', <OpsDeliveryFeeRule />)} />
        <Route path="marketing/customer-groups" element={page('客户分组', <MarketingCustomerGroups />)} />
        <Route path="marketing/group-prices" element={page('分组价格', <MarketingGroupPrices />)} />
        <Route path="marketing/coupons" element={page('优惠券', <MarketingCoupons />)} />
        <Route path="marketing/coupons/:id" element={page('优惠券详情', <MarketingCouponDetail />)} />
        <Route path="marketing/promotions" element={page('满减/折扣', <MarketingPromotions />)} />
        <Route path="marketing/promotions/:id" element={page('满减/折扣详情', <MarketingPromotionDetail />)} />
        <Route path="marketing/water-tickets" element={page('桶水票', <MarketingWaterTickets />)} />
        <Route path="marketing/water-tickets/:id" element={page('水票详情', <MarketingWaterTicketDetail />)} />
        <Route path="marketing/flash-sale" element={page('限时特价', <MarketingFlashSale />)} />
        <Route path="marketing/flash-sale/:id" element={page('限时特价详情', <MarketingFlashSaleDetail />)} />
        <Route path="marketing/buy-gift" element={page('买赠/满赠', <MarketingBuyGift />)} />
        <Route path="marketing/buy-gift/:id" element={page('买赠/满赠详情', <MarketingGiftDetail />)} />
        <Route path="marketing/newcomer" element={page('首单立减/新人价', <MarketingNewcomer />)} />
        <Route path="marketing/newcomer/:id" element={page('首单立减/新人价详情', <MarketingNewcomerDetail />)} />
        <Route path="system-config/order" element={page('订单与履约参数', <SystemOrderConfig />)} />
        <Route path="system-config/stock" element={page('库存预警阈值', <SystemStockConfig />)} />
        <Route path="system-config/security" element={page('账号与安全', <SystemSecurityConfig />)} />
        <Route path="system-config/notifications" element={page('通知与订阅消息', <SystemNotificationsConfig />)} />
        <Route path="notifications/messages" element={page('消息列表', <NotificationMessages />)} />
        <Route path="system-config/data" element={page('数据维护', <SystemDataMaintenanceConfig />)} />
      </Route>
    </Routes>
  );
};

export default App;
