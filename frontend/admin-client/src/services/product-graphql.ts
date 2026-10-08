import { useQuery, useMutation, useLazyQuery } from '@apollo/client';
import { message } from 'antd';
import {
  GET_PRODUCT_DETAIL_QUERY,
  PRODUCTS_PAGE_DATA,
  PRODUCT_FORM_PAGE_DATA,
  PRODUCT_OPTIONS,
} from '../graphql/queries/product.graphql';
import {
  CREATE_PRODUCT_MUTATION,
  UPDATE_PRODUCT_MUTATION,
  DELETE_PRODUCT_MUTATION,
  CREATE_PRODUCT_OPTION_MUTATION,
  UPDATE_PRODUCT_OPTION_MUTATION,
  DELETE_PRODUCT_OPTION_MUTATION,
} from '../graphql/mutations/product.graphql';
import { PRODUCT_FORM_PAGE_DATA_QUERY, PRODUCT_OPTIONS_QUERY } from '../graphql/queries/product.graphql';

export const useProductDetail = (id: string) => {
  return useQuery(GET_PRODUCT_DETAIL_QUERY, {
    variables: { id },
    skip: !id,
    errorPolicy: 'all',
  });
};

// 商品新增/编辑/详情页单页查询(规则 #10):
// 编辑/详情模式 = ProductFormPageData(product + 规格/水源地启用选项);
// 新增模式 = ProductOptions(仅选项)。两种模式各自只发一个 GraphQL 请求。
export const useProductFormPageData = (id: string | undefined) => {
  const combined = useQuery(PRODUCT_FORM_PAGE_DATA_QUERY, {
    variables: { id: id ?? '' },
    skip: !id,
    errorPolicy: 'all',
  });
  const optionsOnly = useQuery(PRODUCT_OPTIONS_QUERY, {
    variables: { enabledOnly: true },
    skip: Boolean(id),
    errorPolicy: 'all',
  });
  return id ? combined : optionsOnly;
};

// 规格/水源地配置页单页查询(规则 #10):一次取回两类全量选项(含禁用项)
export const useProductOptionsConfig = () => {
  return useLazyQuery(PRODUCT_OPTIONS_QUERY, {
    variables: { enabledOnly: false },
    fetchPolicy: 'cache-and-network',
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

// ==================== 商品属性选项字典(F-PRO-009) ====================

export const useCreateProductOption = () => {
  return useMutation(CREATE_PRODUCT_OPTION_MUTATION, {
    onCompleted: () => {
      message.success('选项创建成功');
    },
    onError: (error) => {
      message.error(error.message || '选项创建失败');
    },
    refetchQueries: [PRODUCT_OPTIONS, PRODUCT_FORM_PAGE_DATA],
  });
};

export const useUpdateProductOption = () => {
  return useMutation(UPDATE_PRODUCT_OPTION_MUTATION, {
    onCompleted: () => {
      message.success('选项更新成功');
    },
    onError: (error) => {
      message.error(error.message || '选项更新失败');
    },
    refetchQueries: [PRODUCT_OPTIONS, PRODUCT_FORM_PAGE_DATA],
  });
};

export const useDeleteProductOption = () => {
  return useMutation(DELETE_PRODUCT_OPTION_MUTATION, {
    onCompleted: () => {
      message.success('选项删除成功');
    },
    onError: (error) => {
      message.error(error.message || '选项删除失败');
    },
    refetchQueries: [PRODUCT_OPTIONS, PRODUCT_FORM_PAGE_DATA],
  });
};
