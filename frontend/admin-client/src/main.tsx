import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ApolloProvider } from '@apollo/client';
import { HelmetProvider } from 'react-helmet-async';
import { apolloClient } from './lib/apollo-client';
import App from './App';
import './utils/tracking';
import './index.css';
import 'virtual:stylex.css';

// 管理端设计 token 由 @aquarush/common/theme/tokens.stylex 提供
// (spec/design/ui-design-system.md §2.3;品牌主色 #3A9BD9,背景 #F5F7FA),
// 经 @aquarush/admin-ui 组件库的 StyleX 样式直接消费,无需运行时主题 Provider。
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <HelmetProvider>
      <ApolloProvider client={apolloClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ApolloProvider>
    </HelmetProvider>
  </React.StrictMode>
);
