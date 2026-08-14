import { useLazyQuery, useMutation } from '@apollo/client';
import { message } from 'antd';
import {
  GET_CUSTOMER_GROUPS_QUERY,
  GET_GROUP_PRODUCT_PRICES_QUERY,
  GET_COUPON_TEMPLATES_QUERY,
  GET_PROMOTION_ACTIVITIES_QUERY,
  GET_TICKET_TYPES_QUERY,
  GET_FLASH_SALE_ACTIVITIES_QUERY,
  GET_GIFT_ACTIVITIES_QUERY,
  GET_FIRST_ORDER_ACTIVITIES_QUERY,
} from '../graphql/queries/marketing.graphql';
import {
  CREATE_CUSTOMER_GROUP_MUTATION,
  UPDATE_CUSTOMER_GROUP_MUTATION,
  DELETE_CUSTOMER_GROUP_MUTATION,
  SET_USER_CUSTOMER_GROUP_MUTATION,
  SAVE_GROUP_PRODUCT_PRICES_MUTATION,
  CREATE_COUPON_TEMPLATE_MUTATION,
  UPDATE_COUPON_TEMPLATE_MUTATION,
  DISABLE_COUPON_TEMPLATE_MUTATION,
  ISSUE_COUPONS_MUTATION,
  CREATE_PROMOTION_ACTIVITY_MUTATION,
  CREATE_FLASH_SALE_ACTIVITY_MUTATION,
  UPDATE_FLASH_SALE_ACTIVITY_MUTATION,
  DISABLE_FLASH_SALE_ACTIVITY_MUTATION,
  CREATE_GIFT_ACTIVITY_MUTATION,
  UPDATE_GIFT_ACTIVITY_MUTATION,
  DISABLE_GIFT_ACTIVITY_MUTATION,
  CREATE_FIRST_ORDER_ACTIVITY_MUTATION,
  UPDATE_FIRST_ORDER_ACTIVITY_MUTATION,
  DISABLE_FIRST_ORDER_ACTIVITY_MUTATION,
  UPDATE_PROMOTION_ACTIVITY_MUTATION,
  DISABLE_PROMOTION_ACTIVITY_MUTATION,
  CREATE_TICKET_TYPE_MUTATION,
  UPDATE_TICKET_TYPE_MUTATION,
  DISABLE_TICKET_TYPE_MUTATION,
} from '../graphql/mutations/marketing.graphql';

// ==================== 客户分组(F-MKT-001) ====================

export const useCustomerGroups = () => {
  return useLazyQuery(GET_CUSTOMER_GROUPS_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useCreateCustomerGroup = () => {
  return useMutation(CREATE_CUSTOMER_GROUP_MUTATION, {
    onCompleted: () => {
      message.success('客户分组创建成功');
    },
    onError: (error) => {
      message.error(error.message || '客户分组创建失败');
    },
    refetchQueries: ['GetCustomerGroups'],
  });
};

export const useUpdateCustomerGroup = () => {
  return useMutation(UPDATE_CUSTOMER_GROUP_MUTATION, {
    onCompleted: () => {
      message.success('客户分组更新成功');
    },
    onError: (error) => {
      message.error(error.message || '客户分组更新失败');
    },
    refetchQueries: ['GetCustomerGroups'],
  });
};

export const useDeleteCustomerGroup = () => {
  return useMutation(DELETE_CUSTOMER_GROUP_MUTATION, {
    onCompleted: () => {
      message.success('客户分组已删除');
    },
    onError: (error) => {
      message.error(error.message || '客户分组删除失败');
    },
    refetchQueries: ['GetCustomerGroups', 'GetGroupProductPrices', 'GetUsers'],
  });
};

export const useSetUserCustomerGroup = () => {
  return useMutation(SET_USER_CUSTOMER_GROUP_MUTATION, {
    onCompleted: () => {
      message.success('用户分组已更新');
    },
    onError: (error) => {
      message.error(error.message || '用户分组更新失败');
    },
    refetchQueries: ['GetUsers'],
  });
};

// ==================== 分组商品专属价(F-MKT-002) ====================

export const useGroupProductPrices = () => {
  return useLazyQuery(GET_GROUP_PRODUCT_PRICES_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useSaveGroupProductPrices = () => {
  return useMutation(SAVE_GROUP_PRODUCT_PRICES_MUTATION, {
    onCompleted: () => {
      message.success('分组价格已保存');
    },
    onError: (error) => {
      message.error(error.message || '分组价格保存失败');
    },
    refetchQueries: ['GetGroupProductPrices', 'GetCustomerGroups'],
  });
};

// ==================== 优惠券(F-MKT-003) ====================

export const useCouponTemplates = () => {
  return useLazyQuery(GET_COUPON_TEMPLATES_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useCreateCouponTemplate = () => {
  return useMutation(CREATE_COUPON_TEMPLATE_MUTATION, {
    onCompleted: () => {
      message.success('优惠券创建成功');
    },
    onError: (error) => {
      message.error(error.message || '优惠券创建失败');
    },
    refetchQueries: ['GetCouponTemplates'],
  });
};

export const useUpdateCouponTemplate = () => {
  return useMutation(UPDATE_COUPON_TEMPLATE_MUTATION, {
    onCompleted: () => {
      message.success('优惠券更新成功');
    },
    onError: (error) => {
      message.error(error.message || '优惠券更新失败');
    },
    refetchQueries: ['GetCouponTemplates'],
  });
};

export const useDisableCouponTemplate = () => {
  return useMutation(DISABLE_COUPON_TEMPLATE_MUTATION, {
    onCompleted: () => {
      message.success('优惠券已停用');
    },
    onError: (error) => {
      message.error(error.message || '优惠券停用失败');
    },
    refetchQueries: ['GetCouponTemplates'],
  });
};

export const useIssueCoupons = () => {
  return useMutation(ISSUE_COUPONS_MUTATION, {
    onError: (error) => {
      message.error(error.message || '优惠券发放失败');
    },
    refetchQueries: ['GetCouponTemplates'],
  });
};

// ==================== 满减/折扣活动(F-MKT-004) ====================

export const usePromotionActivities = () => {
  return useLazyQuery(GET_PROMOTION_ACTIVITIES_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useCreatePromotionActivity = () => {
  return useMutation(CREATE_PROMOTION_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('活动创建成功');
    },
    onError: (error) => {
      message.error(error.message || '活动创建失败');
    },
    refetchQueries: ['GetPromotionActivities'],
  });
};

export const useUpdatePromotionActivity = () => {
  return useMutation(UPDATE_PROMOTION_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('活动更新成功');
    },
    onError: (error) => {
      message.error(error.message || '活动更新失败');
    },
    refetchQueries: ['GetPromotionActivities'],
  });
};

export const useDisablePromotionActivity = () => {
  return useMutation(DISABLE_PROMOTION_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('活动已停用');
    },
    onError: (error) => {
      message.error(error.message || '活动停用失败');
    },
    refetchQueries: ['GetPromotionActivities'],
  });
};

// ==================== 桶水票(F-MKT-007) ====================

export const useTicketTypes = () => {
  return useLazyQuery(GET_TICKET_TYPES_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useCreateTicketType = () => {
  return useMutation(CREATE_TICKET_TYPE_MUTATION, {
    onCompleted: () => {
      message.success('票种创建成功');
    },
    onError: (error) => {
      message.error(error.message || '票种创建失败');
    },
    refetchQueries: ['GetTicketTypes'],
  });
};

export const useUpdateTicketType = () => {
  return useMutation(UPDATE_TICKET_TYPE_MUTATION, {
    onCompleted: () => {
      message.success('票种更新成功');
    },
    onError: (error) => {
      message.error(error.message || '票种更新失败');
    },
    refetchQueries: ['GetTicketTypes'],
  });
};

export const useDisableTicketType = () => {
  return useMutation(DISABLE_TICKET_TYPE_MUTATION, {
    onCompleted: () => {
      message.success('票种已停用');
    },
    onError: (error) => {
      message.error(error.message || '票种停用失败');
    },
    refetchQueries: ['GetTicketTypes'],
  });
};

// ==================== 限时特价活动(F-MKT-005) ====================

export const useFlashSaleActivities = () => {
  return useLazyQuery(GET_FLASH_SALE_ACTIVITIES_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useCreateFlashSaleActivity = () => {
  return useMutation(CREATE_FLASH_SALE_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('特价活动创建成功');
    },
    onError: (error) => {
      message.error(error.message || '特价活动创建失败');
    },
    refetchQueries: ['GetFlashSaleActivities'],
  });
};

export const useUpdateFlashSaleActivity = () => {
  return useMutation(UPDATE_FLASH_SALE_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('特价活动更新成功');
    },
    onError: (error) => {
      message.error(error.message || '特价活动更新失败');
    },
    refetchQueries: ['GetFlashSaleActivities'],
  });
};

export const useDisableFlashSaleActivity = () => {
  return useMutation(DISABLE_FLASH_SALE_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('特价活动已停用');
    },
    onError: (error) => {
      message.error(error.message || '特价活动停用失败');
    },
    refetchQueries: ['GetFlashSaleActivities'],
  });
};

// ==================== 买赠/满赠活动(F-MKT-008) ====================

export const useGiftActivities = () => {
  return useLazyQuery(GET_GIFT_ACTIVITIES_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useCreateGiftActivity = () => {
  return useMutation(CREATE_GIFT_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('活动创建成功');
    },
    onError: (error) => {
      message.error(error.message || '活动创建失败');
    },
    refetchQueries: ['GetGiftActivities'],
  });
};

export const useUpdateGiftActivity = () => {
  return useMutation(UPDATE_GIFT_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('活动更新成功');
    },
    onError: (error) => {
      message.error(error.message || '活动更新失败');
    },
    refetchQueries: ['GetGiftActivities'],
  });
};

export const useDisableGiftActivity = () => {
  return useMutation(DISABLE_GIFT_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('活动已停用');
    },
    onError: (error) => {
      message.error(error.message || '活动停用失败');
    },
    refetchQueries: ['GetGiftActivities'],
  });
};

// ==================== 首单立减/新人专享价(F-MKT-009) ====================

export const useFirstOrderActivities = () => {
  return useLazyQuery(GET_FIRST_ORDER_ACTIVITIES_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useCreateFirstOrderActivity = () => {
  return useMutation(CREATE_FIRST_ORDER_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('新人优惠创建成功');
    },
    onError: (error) => {
      message.error(error.message || '新人优惠创建失败');
    },
    refetchQueries: ['GetFirstOrderActivities'],
  });
};

export const useUpdateFirstOrderActivity = () => {
  return useMutation(UPDATE_FIRST_ORDER_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('新人优惠更新成功');
    },
    onError: (error) => {
      message.error(error.message || '新人优惠更新失败');
    },
    refetchQueries: ['GetFirstOrderActivities'],
  });
};

export const useDisableFirstOrderActivity = () => {
  return useMutation(DISABLE_FIRST_ORDER_ACTIVITY_MUTATION, {
    onCompleted: () => {
      message.success('新人优惠已停用');
    },
    onError: (error) => {
      message.error(error.message || '新人优惠停用失败');
    },
    refetchQueries: ['GetFirstOrderActivities'],
  });
};
