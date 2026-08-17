import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import { ApolloProvider } from '@apollo/client';
import { HelmetProvider } from 'react-helmet-async';
import zhCN from 'antd/locale/zh_CN';
import { apolloClient } from './lib/apollo-client';
import App from './App';
import './utils/tracking';
import './index.css';

// 管理端设计 token(admin-client docs/ui-spec.md §1;品牌主色 #3A9BD9,背景 #F5F7FA)
const themeToken = {
  colorPrimary: '#3A9BD9',
  colorLink: '#3A9BD9',
  colorBgLayout: '#F5F7FA',
  colorBgContainer: '#FFFFFF',
  colorText: '#1A1A1A',
  colorTextSecondary: '#8C8C8C',
  colorError: '#FF4D4F',
  colorSuccess: '#52C41A',
  colorWarning: '#FAAD14',
  borderRadius: 8,
};

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <HelmetProvider>
      <ApolloProvider client={apolloClient}>
        <ConfigProvider locale={zhCN} theme={{ token: themeToken }}>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </ConfigProvider>
      </ApolloProvider>
    </HelmetProvider>
  </React.StrictMode>
);
