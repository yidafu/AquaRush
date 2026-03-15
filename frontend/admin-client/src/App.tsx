import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/login';
import Dashboard from './pages/dashboard';
import ProductListPage from './pages/products/list';
import ProductDetailPage from './pages/products/detail';
import Orders from './pages/orders';
import OrderDetailPage from './pages/orders/detail';
import Statistics from './pages/statistics';
import BucketDeposits from './pages/bucket-deposits';
import UserDetailPage from './pages/users/detail/index';
import DeliveryWorkerDetailPage from './pages/users/delivery-workers/detail/index';
import AdminUsers from './pages/users/admins';
import DeliveryWorkerUsers from './pages/users/delivery-workers';
import CustomerUsers from './pages/users/customers';
import AddressListPage from './pages/addresses/list';
import ApiLogs from './pages/logs/api';
import UserActionLogs from './pages/logs/user-actions';
import BusinessLogs from './pages/logs/business';

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <PrivateRoute>
            <Layout />
          </PrivateRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="users/admins" element={<AdminUsers />} />
        <Route path="users/delivery-workers" element={<DeliveryWorkerUsers />} />
        <Route path="users/customers" element={<CustomerUsers />} />
        <Route path="users" element={<Navigate to="/users/admins" replace />} />
        <Route path="users/:id" element={<UserDetailPage />} />
        <Route path="users/delivery-workers/:id" element={<DeliveryWorkerDetailPage />} />
        <Route path="products" element={<ProductListPage />} />
        <Route path="products/:id" element={<ProductDetailPage />} />
        <Route path="orders" element={<Orders />} />
        <Route path="orders/:id" element={<OrderDetailPage />} />
        <Route path="statistics" element={<Statistics />} />
        <Route path="bucket-deposits" element={<BucketDeposits />} />
        <Route path="addresses" element={<AddressListPage />} />
        <Route path="logs/api" element={<ApiLogs />} />
        <Route path="logs/user-actions" element={<UserActionLogs />} />
        <Route path="logs/business" element={<BusinessLogs />} />
      </Route>
    </Routes>
  );
};

export default App;
