import { gql } from '@apollo/client';

// 营销管理(F-MKT):客户分组/分组价格/优惠券/满减折扣活动/桶水票 列表查询

export const GET_CUSTOMER_GROUPS_QUERY = gql`
  query GetCustomerGroups($input: CustomerGroupListInput) {
    customerGroups(input: $input) {
      content {
        id
        name
        description
        memberCount
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_GROUP_PRODUCT_PRICES_QUERY = gql`
  query GetGroupProductPrices($input: GroupProductPriceListInput!) {
    groupProductPrices(input: $input) {
      content {
        id
        customerGroupId
        productId
        productName
        productSpecification
        catalogPriceCents
        priceCents
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_COUPON_TEMPLATES_QUERY = gql`
  query GetCouponTemplates($input: CouponTemplateListInput) {
    couponTemplates(input: $input) {
      content {
        id
        name
        couponType
        thresholdCents
        discountAmountCents
        discountRatePermille
        scope
        productId
        validityType
        validFrom
        validTo
        validDays
        issuanceType
        perUserLimit
        status
        receivedCount
        usedCount
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_PROMOTION_ACTIVITIES_QUERY = gql`
  query GetPromotionActivities($input: PromotionActivityListInput) {
    promotionActivities(input: $input) {
      content {
        id
        name
        activityType
        scope
        productId
        customerGroupId
        validFrom
        validTo
        status
        currentStatus
        tiers {
          id
          thresholdCents
          discountAmountCents
          discountRatePermille
        }
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_TICKET_TYPES_QUERY = gql`
  query GetTicketTypes($input: TicketTypeListInput) {
    ticketTypes(input: $input) {
      content {
        id
        name
        ticketType
        productId
        specification
        packageCount
        packagePriceCents
        giftCount
        validityType
        validFrom
        validTo
        validDays
        perUserLimit
        status
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

// ==================== 限时特价活动(F-MKT-005) ====================

export const GET_FLASH_SALE_ACTIVITIES_QUERY = gql`
  query GetFlashSaleActivities($input: FlashSaleActivityListInput) {
    flashSaleActivities(input: $input) {
      content {
        id
        name
        productId
        productName
        catalogPriceCents
        salePriceCents
        validFrom
        validTo
        status
        currentStatus
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_FLASH_SALE_ACTIVITY_QUERY = gql`
  query GetFlashSaleActivity($id: PrimaryId!) {
    flashSaleActivity(id: $id) {
      id
      name
      productId
      productName
      catalogPriceCents
      salePriceCents
      validFrom
      validTo
      status
      currentStatus
    }
  }
`;

// ==================== 买赠/满赠活动(F-MKT-008) ====================

export const GET_GIFT_ACTIVITIES_QUERY = gql`
  query GetGiftActivities($input: GiftActivityListInput) {
    giftActivities(input: $input) {
      content {
        id
        name
        giftType
        thresholdCents
        thresholdQuantity
        scope
        productId
        customerGroupId
        giftProductId
        giftProductName
        giftQuantity
        validFrom
        validTo
        status
        currentStatus
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_GIFT_ACTIVITY_QUERY = gql`
  query GetGiftActivity($id: PrimaryId!) {
    giftActivity(id: $id) {
      id
      name
      giftType
      thresholdCents
      thresholdQuantity
      scope
      productId
      customerGroupId
      giftProductId
      giftProductName
      giftQuantity
      validFrom
      validTo
      status
      currentStatus
    }
  }
`;

// ==================== 首单立减/新人专享价(F-MKT-009) ====================

export const GET_FIRST_ORDER_ACTIVITIES_QUERY = gql`
  query GetFirstOrderActivities($input: FirstOrderActivityListInput) {
    firstOrderActivities(input: $input) {
      content {
        id
        name
        activityType
        thresholdCents
        discountAmountCents
        discountRatePermille
        scope
        productId
        productName
        newcomerPriceCents
        validFrom
        validTo
        status
        currentStatus
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_FIRST_ORDER_ACTIVITY_QUERY = gql`
  query GetFirstOrderActivity($id: PrimaryId!) {
    firstOrderActivity(id: $id) {
      id
      name
      activityType
      thresholdCents
      discountAmountCents
      discountRatePermille
      scope
      productId
      productName
      newcomerPriceCents
      validFrom
      validTo
      status
      currentStatus
    }
  }
`;
