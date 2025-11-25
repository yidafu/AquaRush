import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/login';
import Dashboard from './pages/dashboard';
import Users from './pages/users';
import Products from './pages/products';
import Orders from './pages/orders';
import Delivery from './pages/delivery';
import Statistics from './pages/statistics';

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
        <Route path="products" element={<Products />} />
        <Route path="orders" element={<Orders />} />
        <Route path="delivery" element={<Delivery />} />
        <Route path="statistics" element={<Statistics />} />
      </Route>
    </Routes>
  );
};

export default App;
