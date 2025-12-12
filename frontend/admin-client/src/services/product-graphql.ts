import { useQuery, useMutation } from '@apollo/client';
import { message } from 'antd';
import {
  GET_PRODUCTS_QUERY,
  GET_PRODUCT_DETAIL_QUERY,
} from '../graphql/queries/product.graphql';
import {
  CREATE_PRODUCT_MUTATION,
  UPDATE_PRODUCT_MUTATION,
  DELETE_PRODUCT_MUTATION,
  UPDATE_PRODUCT_INVENTORY_MUTATION,
} from '../graphql/mutations/product.graphql';
import type {
  Product,
  ProductListInput,
  CreateProductInput,
  UpdateProductInput,
} from '../types/graphql';

// Product Query Hooks
export const useProducts = (input?: ProductListInput) => {
  return useQuery(GET_PRODUCTS_QUERY, {
    variables: { input },
    errorPolicy: 'all',
    notifyOnNetworkStatusChange: true,
  });
};

export const useProductDetail = (id: number) => {
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

export const useUpdateProductInventory = () => {
  return useMutation(UPDATE_PRODUCT_INVENTORY_MUTATION, {
    onCompleted: () => {
      message.success('库存更新成功');
    },
    onError: (error) => {
      message.error(error.message || '库存更新失败');
    },
    refetchQueries: ['GetProducts', 'GetProductDetail'],
  });
};