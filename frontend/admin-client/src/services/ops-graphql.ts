import { useLazyQuery, useMutation } from '@apollo/client';
import { message } from 'antd';
import {
  GET_BANNERS_QUERY,
  GET_BANNER_QUERY,
  GET_FEEDBACKS_QUERY,
  GET_NOTICES_QUERY,
  GET_NOTICE_QUERY,
  GET_STORE_PROFILE_QUERY,
  GET_DELIVERY_FEE_RULE_QUERY,
} from '../graphql/queries/ops.graphql';
import {
  CREATE_BANNER_MUTATION,
  UPDATE_BANNER_MUTATION,
  DELETE_BANNER_MUTATION,
  REORDER_BANNER_MUTATION,
  REVIEW_FEEDBACK_MUTATION,
  CREATE_NOTICE_MUTATION,
  UPDATE_NOTICE_MUTATION,
  PUBLISH_NOTICE_MUTATION,
  STOP_NOTICE_MUTATION,
  SET_NOTICE_PINNED_MUTATION,
  DELETE_NOTICE_MUTATION,
  SAVE_STORE_PROFILE_MUTATION,
  SAVE_DELIVERY_FEE_RULE_MUTATION,
} from '../graphql/mutations/ops.graphql';

// ==================== 轮播图 ====================

export const useBanners = () => {
  return useLazyQuery(GET_BANNERS_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

// 轮播图详情(编辑弹窗按 ID 拉取后回填)
export const useBanner = () => {
  return useLazyQuery(GET_BANNER_QUERY, {
    fetchPolicy: 'network-only',
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

// ==================== 公告 ====================

export const useNotices = () => {
  return useLazyQuery(GET_NOTICES_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

// 公告详情(编辑弹窗按 ID 拉取后回填)
export const useNotice = () => {
  return useLazyQuery(GET_NOTICE_QUERY, {
    fetchPolicy: 'network-only',
    errorPolicy: 'all',
  });
};

export const useCreateNotice = () => {
  return useMutation(CREATE_NOTICE_MUTATION, {
    onCompleted: () => {
      message.success('公告创建成功');
    },
    onError: (error) => {
      message.error(error.message || '公告创建失败');
    },
    refetchQueries: ['GetNotices'],
  });
};

export const useUpdateNotice = () => {
  return useMutation(UPDATE_NOTICE_MUTATION, {
    onCompleted: () => {
      message.success('公告更新成功');
    },
    onError: (error) => {
      message.error(error.message || '公告更新失败');
    },
    refetchQueries: ['GetNotices'],
  });
};

export const usePublishNotice = () => {
  return useMutation(PUBLISH_NOTICE_MUTATION, {
    onCompleted: () => {
      message.success('公告已发布');
    },
    onError: (error) => {
      message.error(error.message || '公告发布失败');
    },
    refetchQueries: ['GetNotices'],
  });
};

export const useStopNotice = () => {
  return useMutation(STOP_NOTICE_MUTATION, {
    onCompleted: () => {
      message.success('公告已停用');
    },
    onError: (error) => {
      message.error(error.message || '公告停用失败');
    },
    refetchQueries: ['GetNotices'],
  });
};

export const useSetNoticePinned = () => {
  return useMutation(SET_NOTICE_PINNED_MUTATION, {
    onCompleted: () => {
      message.success('置顶状态已更新');
    },
    onError: (error) => {
      message.error(error.message || '置顶状态更新失败');
    },
    refetchQueries: ['GetNotices'],
  });
};

export const useDeleteNotice = () => {
  return useMutation(DELETE_NOTICE_MUTATION, {
    onCompleted: () => {
      message.success('公告删除成功');
    },
    onError: (error) => {
      message.error(error.message || '公告删除失败');
    },
    refetchQueries: ['GetNotices'],
  });
};

// ==================== 店铺信息 ====================

export const useStoreProfile = () => {
  return useLazyQuery(GET_STORE_PROFILE_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useSaveStoreProfile = () => {
  return useMutation(SAVE_STORE_PROFILE_MUTATION, {
    onCompleted: () => {
      message.success('店铺信息保存成功');
    },
    onError: (error) => {
      message.error(error.message || '店铺信息保存失败');
    },
    refetchQueries: ['GetStoreProfile'],
  });
};

// ==================== 配送费规则 ====================

export const useDeliveryFeeRule = () => {
  return useLazyQuery(GET_DELIVERY_FEE_RULE_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useSaveDeliveryFeeRule = () => {
  return useMutation(SAVE_DELIVERY_FEE_RULE_MUTATION, {
    onCompleted: () => {
      message.success('配送费规则保存成功');
    },
    onError: (error) => {
      message.error(error.message || '配送费规则保存失败');
    },
    refetchQueries: ['GetDeliveryFeeRule'],
  });
};
