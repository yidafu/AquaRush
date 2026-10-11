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

// 管理端设计 token(spec/design/ui-design-system.md §2.3;品牌主色 #3A9BD9,背景 #F5F7FA)
// 圆角/阴影/发丝线取自 spec/design/assets/tokens.css(--r-card 16、--sh-card、--border)。
// Card 的软投影 antd6 未暴露为 Card 组件 token,统一在 index.css 用 .ant-card 覆盖。
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
  borderRadiusLG: 16,
  colorBorderSecondary: 'rgba(0, 0, 0, 0.06)',
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
