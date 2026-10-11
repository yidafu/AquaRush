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
import 'virtual:stylex.css';

// 管理端设计 token 由 @aquarush/common/theme 提供(spec/design/ui-design-system.md
// §2.3;品牌主色 #3A9BD9,背景 #F5F7FA)。antd 需要真实值,经 ANTD_THEME_TOKEN
// 与 StyleX tokens.stylex.ts 保持一致(守卫:common tokens.test.ts)。
// 圆角/阴影/发丝线取自 spec/design/assets/tokens.css(--r-card 16、--sh-card、--border)。
// Card 的软投影 antd6 未暴露为 Card 组件 token,统一在 index.css 用 .ant-card 覆盖。
import { ANTD_THEME_TOKEN } from '@aquarush/common/theme';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <HelmetProvider>
      <ApolloProvider client={apolloClient}>
        <ConfigProvider locale={zhCN} theme={{ token: ANTD_THEME_TOKEN }}>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </ConfigProvider>
      </ApolloProvider>
    </HelmetProvider>
  </React.StrictMode>
);
