import { useQuery, useMutation } from '@apollo/client';
import { message } from 'antd';
import {
  GET_PRODUCT_DETAIL_QUERY,
  PRODUCTS_PAGE_DATA,
} from '../graphql/queries/product.graphql';
import {
  CREATE_PRODUCT_MUTATION,
  UPDATE_PRODUCT_MUTATION,
  DELETE_PRODUCT_MUTATION,
} from '../graphql/mutations/product.graphql';

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
    // 商品列表页已合并为 ProductsPageData(规则 #10);GetProducts 仍被商品选择器懒查,一并刷新
    refetchQueries: ['GetProducts', PRODUCTS_PAGE_DATA],
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
    refetchQueries: ['GetProducts', 'GetProductDetail', PRODUCTS_PAGE_DATA],
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
    refetchQueries: ['GetProducts', PRODUCTS_PAGE_DATA],
  });
};
