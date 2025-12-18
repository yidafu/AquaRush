import { useQuery, useMutation } from '@apollo/client';
import { message } from 'antd';
import {
  GET_PRODUCTS_QUERY,
  GET_PRODUCT_DETAIL_QUERY,
  GET_PRODUCTS_PAGINATED_QUERY,
  GET_TOP_SALES_PRODUCTS,
  GET_PRODUCTS_BY_WATER_SOURCE,
  GET_PRODUCTS_BY_TAG,
  GET_ALL_ACTIVE_PRODUCTS,
  GET_WATER_SOURCE_STATISTICS,
  GET_SPECIFICATION_STATISTICS,
  GET_LOW_STOCK_PRODUCTS,
} from '../graphql/queries/product.graphql';
import {
  CREATE_PRODUCT_MUTATION,
  UPDATE_PRODUCT_MUTATION,
  DELETE_PRODUCT_MUTATION,
  UPDATE_PRODUCT_STATUS_MUTATION,
  BATCH_ADJUST_STOCK_MUTATION,
  INCREASE_STOCK_MUTATION,
  DECREASE_STOCK_MUTATION,
  ONLINE_PRODUCT_MUTATION,
  OFFLINE_PRODUCT_MUTATION,
  BATCH_UPDATE_PRODUCTS_MUTATION,
} from '../graphql/mutations/product.graphql';

// Product Query Hooks
export const useProducts = (variables: any = {}) => {
  return useQuery(GET_PRODUCTS_QUERY, {
    variables: {
      page: 0,
      size: 20,
      ...variables
    },
    errorPolicy: 'all',
    notifyOnNetworkStatusChange: true,
  });
};

export const useProductsQuery = (variables: any = {}) => {
  return useQuery(GET_PRODUCTS_QUERY, {
    variables: {
      page: 0,
      size: 20,
      ...variables
    },
    errorPolicy: 'all',
    notifyOnNetworkStatusChange: true,
  });
};

export const useProductDetail = (id: string) => {
  return useQuery(GET_PRODUCT_DETAIL_QUERY, {
    variables: { id },
    skip: !id,
    errorPolicy: 'all',
  });
};

// Product Mutation Hooks
export const useCreateProduct = () => {
  return useMutation(CREATE_PRODUCT_MUTATION, {
    onCompleted: () => {
      message.success('产品创建成功');
    },
    onError: (error) => {
      message.error(error.message || '产品创建失败');
    },
    refetchQueries: ['GetProducts'],
  });
};

export const useUpdateProduct = () => {
  return useMutation(UPDATE_PRODUCT_MUTATION, {
    onCompleted: () => {
      message.success('产品更新成功');
    },
    onError: (error) => {
      message.error(error.message || '产品更新失败');
    },
    refetchQueries: ['GetProducts', 'GetProductDetail'],
  });
};

export const useDeleteProduct = () => {
  return useMutation(DELETE_PRODUCT_MUTATION, {
    onCompleted: () => {
      message.success('产品删除成功');
    },
    onError: (error) => {
      message.error(error.message || '产品删除失败');
    },
    refetchQueries: ['GetProducts'],
  });
};

export const useUpdateProductStatus = () => {
  return useMutation(UPDATE_PRODUCT_STATUS_MUTATION, {
    onCompleted: () => {
      message.success('商品状态更新成功');
    },
    onError: (error) => {
      message.error(error.message || '商品状态更新失败');
    },
    refetchQueries: ['GetProducts', 'GetProductDetail'],
  });
};

export const useBatchAdjustStock = () => {
  return useMutation(BATCH_ADJUST_STOCK_MUTATION, {
    onCompleted: (data) => {
      const { successCount, failureCount } = data.batchAdjustStock;
      if (failureCount === 0) {
        message.success(`批量调整库存成功，共更新 ${successCount} 个商品`);
      } else {
        message.warning(`批量调整库存部分成功，成功 ${successCount} 个，失败 ${failureCount} 个`);
      }
    },
    onError: (error) => {
      message.error(error.message || '批量调整库存失败');
    },
    refetchQueries: ['GetProducts'],
  });
};

// Additional Query Hooks
export const useProductsPaginated = (input: any = {}) => {
  return useQuery(GET_PRODUCTS_PAGINATED_QUERY, {
    variables: { input },
    errorPolicy: 'all',
    notifyOnNetworkStatusChange: true,
  });
};

export const useTopSalesProducts = (limit: number = 10) => {
  return useQuery(GET_TOP_SALES_PRODUCTS, {
    variables: { limit },
    errorPolicy: 'all',
  });
};

export const useProductsByWaterSource = (waterSource: string) => {
  return useQuery(GET_PRODUCTS_BY_WATER_SOURCE, {
    variables: { waterSource },
    skip: !waterSource,
    errorPolicy: 'all',
  });
};


export const useProductsByTag = (tag: string) => {
  return useQuery(GET_PRODUCTS_BY_TAG, {
    variables: { tag },
    skip: !tag,
    errorPolicy: 'all',
  });
};

export const useAllActiveProducts = () => {
  return useQuery(GET_ALL_ACTIVE_PRODUCTS, {
    errorPolicy: 'all',
  });
};

export const useWaterSourceStatistics = () => {
  return useQuery(GET_WATER_SOURCE_STATISTICS, {
    errorPolicy: 'all',
  });
};

export const useSpecificationStatistics = () => {
  return useQuery(GET_SPECIFICATION_STATISTICS, {
    errorPolicy: 'all',
  });
};

export const useLowStockProducts = (threshold: number = 10) => {
  return useQuery(GET_LOW_STOCK_PRODUCTS, {
    variables: { threshold },
    errorPolicy: 'all',
  });
};

// Additional Mutation Hooks
export const useIncreaseStock = () => {
  return useMutation(INCREASE_STOCK_MUTATION, {
    onCompleted: (data) => {
      message.success(data.increaseStock || '库存增加成功');
    },
    onError: (error) => {
      message.error(error.message || '库存增加失败');
    },
    refetchQueries: ['GetProducts'],
  });
};

export const useDecreaseStock = () => {
  return useMutation(DECREASE_STOCK_MUTATION, {
    onCompleted: () => {
      message.success('库存减少成功');
    },
    onError: (error) => {
      message.error(error.message || '库存减少失败');
    },
    refetchQueries: ['GetProducts'],
  });
};

export const useOnlineProduct = () => {
  return useMutation(ONLINE_PRODUCT_MUTATION, {
    onCompleted: () => {
      message.success('商品上架成功');
    },
    onError: (error) => {
      message.error(error.message || '商品上架失败');
    },
    refetchQueries: ['GetProducts', 'GetProductDetail'],
  });
};

export const useOfflineProduct = () => {
  return useMutation(OFFLINE_PRODUCT_MUTATION, {
    onCompleted: () => {
      message.success('商品下架成功');
    },
    onError: (error) => {
      message.error(error.message || '商品下架失败');
    },
    refetchQueries: ['GetProducts', 'GetProductDetail'],
  });
};

export const useBatchUpdateProducts = () => {
  return useMutation(BATCH_UPDATE_PRODUCTS_MUTATION, {
    onCompleted: (data) => {
      const count = data.batchUpdateProducts.length;
      message.success(`批量更新商品成功，共更新 ${count} 个商品`);
    },
    onError: (error) => {
      message.error(error.message || '批量更新商品失败');
    },
    refetchQueries: ['GetProducts'],
  });
};
