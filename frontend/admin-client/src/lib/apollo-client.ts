import {
  ApolloClient,
  InMemoryCache,
  createHttpLink,
  from,
  ApolloLink
} from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { onError } from '@apollo/client/link/error';
import { message } from 'antd';

// HTTP 连接到 GraphQL 服务器
const httpLink = createHttpLink({
  uri: '/graphql', // 通过 Vite 代理到后端 9090 端口
});

// 认证 Link - 添加 JWT token
const authLink = setContext((_, { headers }) => {
  const token = localStorage.getItem('token');

  return {
    headers: {
      ...headers,
      Authorization: token ? `Bearer ${token}` : '',
    },
  };
});

// 错误处理 Link
const errorLink = onError(({ graphQLErrors, networkError, operation, forward }) => {
  if (graphQLErrors) {
    graphQLErrors.forEach(({ message: errorMessage, extensions }) => {
      // 处理认证错误
      if (extensions?.code === 'UNAUTHENTICATED') {
        localStorage.removeItem('token');
        localStorage.removeItem('userInfo');
        window.location.href = '/login';
        return;
      }
      // 显示其他 GraphQL 错误
      message.error(errorMessage || 'GraphQL 请求失败');
    });
  }

  if (networkError) {
    message.error('网络连接失败，请检查网络');
  }
});

// 日志 Link（开发环境）
const logLink = new ApolloLink((operation, forward) => {
  if (import.meta.env.DEV) {
    console.log(`[GraphQL] ${operation.operationName}:`, operation.variables);
  }
  return forward(operation).map(result => {
    if (import.meta.env.DEV) {
      console.log(`[GraphQL] Response ${operation.operationName}:`, result);
    }
    return result;
  });
});

// 创建 Apollo Client 实例
export const apolloClient = new ApolloClient({
  link: from([
    logLink,
    errorLink,
    authLink,
    httpLink
  ]),
  cache: new InMemoryCache({
    // 配置缓存策略
    typePolicies: {
      Query: {
        fields: {
          // 配置查询的缓存策略
          users: {
            merge(existing = [], incoming) {
              return incoming;
            },
          },
          orders: {
            merge(existing = [], incoming) {
              return incoming;
            },
          },
          products: {
            merge(existing = [], incoming) {
              return incoming;
            },
          },
        },
      },
      User: {
        keyFields: ["id"],
      },
      Order: {
        keyFields: ["id"],
      },
      Product: {
        keyFields: ["id"],
      },
      DeliveryWorker: {
        keyFields: ["id"],
      },
      Admin: {
        keyFields: ["id"],
      },
    },
  }),
  defaultOptions: {
    watchQuery: {
      errorPolicy: 'all',
      notifyOnNetworkStatusChange: true,
    },
    query: {
      errorPolicy: 'all',
    },
    mutate: {
      errorPolicy: 'all',
    },
  },
});

// 导出自定义 hooks
export const useErrorHandler = () => {
  return (error: any) => {
    if (error?.networkError) {
      message.error('网络连接失败');
      return;
    }

    if (error?.graphQLErrors) {
      error.graphQLErrors.forEach((err: any) => {
        switch (err.extensions?.code) {
          case 'UNAUTHENTICATED':
            message.error('登录已过期，请重新登录');
            localStorage.removeItem('token');
            localStorage.removeItem('userInfo');
            window.location.href = '/login';
            break;
          case 'FORBIDDEN':
            message.error('权限不足');
            break;
          default:
            message.error(err.message || '操作失败');
        }
      });
    }
  };
};