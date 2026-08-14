import { gql } from '@apollo/client';

// 营销管理(F-MKT):客户分组/分组价格/优惠券/满减折扣活动/桶水票 变更操作

// ==================== 客户分组(F-MKT-001) ====================

export const CREATE_CUSTOMER_GROUP_MUTATION = gql`
  mutation CreateCustomerGroup($input: CreateCustomerGroupInput!) {
    createCustomerGroup(input: $input) {
      id
      name
      description
      memberCount
      createdAt
      updatedAt
    }
  }
`;

export const UPDATE_CUSTOMER_GROUP_MUTATION = gql`
  mutation UpdateCustomerGroup($id: PrimaryId!, $input: UpdateCustomerGroupInput!) {
    updateCustomerGroup(id: $id, input: $input) {
      id
      name
      description
      memberCount
      createdAt
      updatedAt
    }
  }
`;

export const DELETE_CUSTOMER_GROUP_MUTATION = gql`
  mutation DeleteCustomerGroup($id: PrimaryId!) {
    deleteCustomerGroup(id: $id)
  }
`;

export const SET_USER_CUSTOMER_GROUP_MUTATION = gql`
  mutation SetUserCustomerGroup($userId: PrimaryId!, $customerGroupId: PrimaryId) {
    setUserCustomerGroup(userId: $userId, customerGroupId: $customerGroupId)
  }
`;

// ==================== 分组商品专属价(F-MKT-002) ====================

export const SAVE_GROUP_PRODUCT_PRICES_MUTATION = gql`
  mutation SaveGroupProductPrices($input: SaveGroupProductPricesInput!) {
    saveGroupProductPrices(input: $input)
  }
`;

// ==================== 优惠券(F-MKT-003) ====================

export const CREATE_COUPON_TEMPLATE_MUTATION = gql`
  mutation CreateCouponTemplate($input: CreateCouponTemplateInput!) {
    createCouponTemplate(input: $input) {
      id
      name
      couponType
      status
      receivedCount
      usedCount
    }
  }
`;

export const UPDATE_COUPON_TEMPLATE_MUTATION = gql`
  mutation UpdateCouponTemplate($id: PrimaryId!, $input: UpdateCouponTemplateInput!) {
    updateCouponTemplate(id: $id, input: $input) {
      id
      name
      couponType
      status
      receivedCount
      usedCount
    }
  }
`;

export const DISABLE_COUPON_TEMPLATE_MUTATION = gql`
  mutation DisableCouponTemplate($id: PrimaryId!) {
    disableCouponTemplate(id: $id) {
      id
      name
      status
    }
  }
`;

export const ISSUE_COUPONS_MUTATION = gql`
  mutation IssueCoupons($input: IssueCouponsInput!) {
    issueCoupons(input: $input)
  }
`;

// ==================== 满减/折扣活动(F-MKT-004) ====================

export const CREATE_PROMOTION_ACTIVITY_MUTATION = gql`
  mutation CreatePromotionActivity($input: CreatePromotionActivityInput!) {
    createPromotionActivity(input: $input) {
      id
      name
      activityType
      scope
      status
      currentStatus
    }
  }
`;

export const UPDATE_PROMOTION_ACTIVITY_MUTATION = gql`
  mutation UpdatePromotionActivity($id: PrimaryId!, $input: UpdatePromotionActivityInput!) {
    updatePromotionActivity(id: $id, input: $input) {
      id
      name
      activityType
      scope
      status
      currentStatus
    }
  }
`;

export const DISABLE_PROMOTION_ACTIVITY_MUTATION = gql`
  mutation DisablePromotionActivity($id: PrimaryId!) {
    disablePromotionActivity(id: $id) {
      id
      name
      status
      currentStatus
    }
  }
`;

// ==================== 桶水票(F-MKT-007) ====================

export const CREATE_TICKET_TYPE_MUTATION = gql`
  mutation CreateTicketType($input: CreateTicketTypeInput!) {
    createTicketType(input: $input) {
      id
      name
      ticketType
      status
    }
  }
`;

export const UPDATE_TICKET_TYPE_MUTATION = gql`
  mutation UpdateTicketType($id: PrimaryId!, $input: UpdateTicketTypeInput!) {
    updateTicketType(id: $id, input: $input) {
      id
      name
      ticketType
      status
    }
  }
`;

export const DISABLE_TICKET_TYPE_MUTATION = gql`
  mutation DisableTicketType($id: PrimaryId!) {
    disableTicketType(id: $id) {
      id
      name
      status
    }
  }
`;

// ==================== 限时特价活动(F-MKT-005) ====================

export const CREATE_FLASH_SALE_ACTIVITY_MUTATION = gql`
  mutation CreateFlashSaleActivity($input: CreateFlashSaleActivityInput!) {
    createFlashSaleActivity(input: $input) {
      id
      name
      productId
      salePriceCents
      status
      currentStatus
    }
  }
`;

export const UPDATE_FLASH_SALE_ACTIVITY_MUTATION = gql`
  mutation UpdateFlashSaleActivity($id: PrimaryId!, $input: UpdateFlashSaleActivityInput!) {
    updateFlashSaleActivity(id: $id, input: $input) {
      id
      name
      productId
      salePriceCents
      status
      currentStatus
    }
  }
`;

export const DISABLE_FLASH_SALE_ACTIVITY_MUTATION = gql`
  mutation DisableFlashSaleActivity($id: PrimaryId!) {
    disableFlashSaleActivity(id: $id) {
      id
      name
      status
      currentStatus
    }
  }
`;

// ==================== 买赠/满赠活动(F-MKT-008) ====================

export const CREATE_GIFT_ACTIVITY_MUTATION = gql`
  mutation CreateGiftActivity($input: CreateGiftActivityInput!) {
    createGiftActivity(input: $input) {
      id
      name
      giftType
      giftProductId
      giftQuantity
      status
      currentStatus
    }
  }
`;

export const UPDATE_GIFT_ACTIVITY_MUTATION = gql`
  mutation UpdateGiftActivity($id: PrimaryId!, $input: UpdateGiftActivityInput!) {
    updateGiftActivity(id: $id, input: $input) {
      id
      name
      giftType
      giftProductId
      giftQuantity
      status
      currentStatus
    }
  }
`;

export const DISABLE_GIFT_ACTIVITY_MUTATION = gql`
  mutation DisableGiftActivity($id: PrimaryId!) {
    disableGiftActivity(id: $id) {
      id
      name
      status
      currentStatus
    }
  }
`;

// ==================== 首单立减/新人专享价(F-MKT-009) ====================

export const CREATE_FIRST_ORDER_ACTIVITY_MUTATION = gql`
  mutation CreateFirstOrderActivity($input: CreateFirstOrderActivityInput!) {
    createFirstOrderActivity(input: $input) {
      id
      name
      activityType
      status
      currentStatus
    }
  }
`;

export const UPDATE_FIRST_ORDER_ACTIVITY_MUTATION = gql`
  mutation UpdateFirstOrderActivity($id: PrimaryId!, $input: UpdateFirstOrderActivityInput!) {
    updateFirstOrderActivity(id: $id, input: $input) {
      id
      name
      activityType
      status
      currentStatus
    }
  }
`;

export const DISABLE_FIRST_ORDER_ACTIVITY_MUTATION = gql`
  mutation DisableFirstOrderActivity($id: PrimaryId!) {
    disableFirstOrderActivity(id: $id) {
      id
      name
      status
      currentStatus
    }
  }
`;
