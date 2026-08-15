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

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <HelmetProvider>
      <ApolloProvider client={apolloClient}>
        <ConfigProvider locale={zhCN}>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </ConfigProvider>
      </ApolloProvider>
    </HelmetProvider>
  </React.StrictMode>
);
