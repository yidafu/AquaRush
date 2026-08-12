import { useLazyQuery, useMutation } from '@apollo/client';
import { message } from 'antd';
import {
  GET_BANNERS_QUERY,
  GET_FEEDBACKS_QUERY,
} from '../graphql/queries/ops.graphql';
import {
  CREATE_BANNER_MUTATION,
  UPDATE_BANNER_MUTATION,
  DELETE_BANNER_MUTATION,
  REORDER_BANNER_MUTATION,
  REVIEW_FEEDBACK_MUTATION,
} from '../graphql/mutations/ops.graphql';

// ==================== 轮播图 ====================

export const useBanners = () => {
  return useLazyQuery(GET_BANNERS_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useCreateBanner = () => {
  return useMutation(CREATE_BANNER_MUTATION, {
    onCompleted: () => {
      message.success('轮播图创建成功');
    },
    onError: (error) => {
      message.error(error.message || '轮播图创建失败');
    },
    refetchQueries: ['GetBanners'],
  });
};

export const useUpdateBanner = () => {
  return useMutation(UPDATE_BANNER_MUTATION, {
    onCompleted: () => {
      message.success('轮播图更新成功');
    },
    onError: (error) => {
      message.error(error.message || '轮播图更新失败');
    },
    refetchQueries: ['GetBanners'],
  });
};

export const useDeleteBanner = () => {
  return useMutation(DELETE_BANNER_MUTATION, {
    onCompleted: () => {
      message.success('轮播图删除成功');
    },
    onError: (error) => {
      message.error(error.message || '轮播图删除失败');
    },
    refetchQueries: ['GetBanners'],
  });
};

export const useReorderBanner = () => {
  return useMutation(REORDER_BANNER_MUTATION, {
    onCompleted: () => {
      message.success('排序已更新');
    },
    onError: (error) => {
      message.error(error.message || '排序更新失败');
    },
    refetchQueries: ['GetBanners'],
  });
};

// ==================== 意见反馈 ====================

export const useFeedbacks = () => {
  return useLazyQuery(GET_FEEDBACKS_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useReviewFeedback = () => {
  return useMutation(REVIEW_FEEDBACK_MUTATION, {
    onCompleted: () => {
      message.success('反馈处理成功');
    },
    onError: (error) => {
      message.error(error.message || '反馈处理失败');
    },
    refetchQueries: ['GetFeedbacks'],
  });
};
