import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/login';
import Dashboard from './pages/dashboard';
import Users from './pages/users';
import ProductListPage from './pages/products/list';
import ProductDetailPage from './pages/products/detail';
import Orders from './pages/orders';
import Delivery from './pages/delivery';
import Statistics from './pages/statistics';
import UserDetailPage from './pages/users/detail/index';
import DeliveryWorkerDetailPage from './pages/users/delivery-workers/detail/index';

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
        <Route path="users" element={<Users />} />
        <Route path="users/:id" element={<UserDetailPage />} />
        <Route path="users/delivery-workers/:id" element={<DeliveryWorkerDetailPage />} />
        <Route path="products" element={<ProductListPage />} />
        <Route path="products/:id" element={<ProductDetailPage />} />
        <Route path="orders" element={<Orders />} />
        <Route path="delivery" element={<Delivery />} />
        <Route path="statistics" element={<Statistics />} />
      </Route>
    </Routes>
  );
};

export default App;
