import { ApolloLink } from '@apollo/client';
import type { Operation, FetchResult } from '@apollo/client';
import { Observable } from '@apollo/client/utilities';
import { mockProducts, MOCK_DELAY } from './data/products';

// 判断是否为商品相关的 GraphQL 操作
const isProductOperation = (operationName: string) => {
  return operationName.toLowerCase().includes('product');
};

// 生成mock数据
const generateMockResponse = (operation: Operation): Observable<FetchResult> => {
  const { operationName, variables } = operation;

  return new Observable(observer => {
    setTimeout(() => {
      try {
        let result: any;

        // 处理查询
        if (operationName === 'GetProducts') {
          result = { products: mockProducts };
          observer.next({ data: result });
          observer.complete();
          return;
        }

        if (operationName === 'GetProductDetail') {
          const product = mockProducts.find(p => p.id === variables.id);
          if (product) {
            result = { product };
            observer.next({ data: result });
          } else {
            observer.error(new Error(`Product with id ${variables.id} not found`));
          }
          observer.complete();
          return;
        }

        // 处理变更
        if (operationName === 'CreateProduct') {
          const newProduct = {
            id: String(mockProducts.length + 1),
            ...variables.input,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          };
          mockProducts.push(newProduct);
          result = { createProduct: newProduct };
          observer.next({ data: result });
          observer.complete();
          return;
        }

        if (operationName === 'UpdateProduct') {
          const index = mockProducts.findIndex(p => p.id === variables.id);
          if (index !== -1) {
            mockProducts[index] = { ...mockProducts[index], ...variables.input, updatedAt: new Date().toISOString() };
            result = { updateProduct: mockProducts[index] };
            observer.next({ data: result });
          } else {
            observer.error(new Error(`Product with id ${variables.id} not found`));
          }
          observer.complete();
          return;
        }

        if (operationName === 'DeleteProduct') {
          const index = mockProducts.findIndex(p => p.id === variables.id);
          if (index !== -1) {
            const deletedProduct = mockProducts.splice(index, 1)[0];
            result = { deleteProduct: deletedProduct };
            observer.next({ data: result });
          } else {
            observer.error(new Error(`Product with id ${variables.id} not found`));
          }
          observer.complete();
          return;
        }

        // 对于不支持的operation，返回错误
        observer.error(new Error(`Unsupported operation: ${operationName}`));
      } catch (error) {
        observer.error(error);
      }
    }, MOCK_DELAY);
  });
};

// 创建Mock链接
export const mockLink = new ApolloLink((operation, forward) => {
  // 只处理商品相关的操作，其他的继续转发到真实服务器
  if (isProductOperation(operation.operationName)) {
    return generateMockResponse(operation);
  } else {
    return forward(operation);
  }
});

// 判断是否启用Mock模式的工具函数
export const shouldUseMock = () => {
  // 可以通过环境变量控制
  return import.meta.env.VITE_USE_MOCK === 'true' || import.meta.env.NODE_ENV === 'development';
};

// Apollo Mock 链接器包装器
export const createMockApolloLink = () => {
  return shouldUseMock() ? mockLink : null;
};