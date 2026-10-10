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

/**
 * 水票管理页(WaterTicketsPage)单页查询:
 * 一次请求取回「票种分页 + 商品目录(指定商品票展示商品名)」,满足
 * AGENTS.md 规则 7 —— 一个页面的一次渲染只发起一个 GraphQL 请求。
 */
export const WATER_TICKETS_PAGE_DATA_QUERY = gql`
  query WaterTicketsPageData($input: TicketTypeListInput, $productsInput: ProductSearchInput) {
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
    productsPaginated(input: $productsInput) {
      list {
        id
        name
        specification
      }
    }
  }
`;

/** WaterTicketsPageData 操作名(供 refetchQueries 引用) */
export const WATER_TICKETS_PAGE_DATA = 'WaterTicketsPageData';

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

// 桶水票票种销售/使用统计(详情抽屉图表,F-MKT-007):单请求取回汇总 + 销售/使用序列(规则 #10)
export const TICKET_TYPE_STATS_QUERY = gql`
  query TicketTypeStats($ticketTypeId: PrimaryId!, $input: DateRangeInput!) {
    ticketTypeStats(ticketTypeId: $ticketTypeId, input: $input) {
      ticketTypeId
      ticketTypeName
      summary {
        soldCount
        revenueCents
        redeemedCount
        returnedCount
        expiredCount
        availableCount
      }
      sales {
        date
        purchaseCount
        ticketCount
        revenueCents
      }
      usage {
        date
        redeemedCount
        returnedCount
        expiredCount
      }
    }
  }
`;

// ==================== 营销方式详情页:使用情况 + 相关订单(F-MKT-003~009) ====================
// 单页单请求(规则 #10):配置根字段 + 使用情况根字段一次取回;分页经 variables 变化自动重取。

// 优惠券模板使用情况与相关订单(F-MKT-003)
export const COUPON_TEMPLATE_USAGE_PAGE_DATA_QUERY = gql`
  query CouponTemplateUsagePageData($id: PrimaryId!, $input: MarketingUsageListInput) {
    couponTemplate(id: $id) {
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
    couponTemplateUsage(couponTemplateId: $id, input: $input) {
      summary {
        orderCount
        benefitCents
        usageCount
      }
      orders {
        content {
          id
          orderNo
          user {
            id
            nickname
            phone
          }
          status
          amountCents
          couponName
          couponDiscountCents
          createdAt
        }
        totalElements
        totalPages
        size
        number
      }
    }
  }
`;

// 满减/折扣活动使用情况与相关订单(F-MKT-004)
export const PROMOTION_ACTIVITY_USAGE_PAGE_DATA_QUERY = gql`
  query PromotionActivityUsagePageData($id: PrimaryId!, $input: MarketingUsageListInput) {
    promotionActivity(id: $id) {
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
    promotionActivityUsage(promotionActivityId: $id, input: $input) {
      summary {
        orderCount
        benefitCents
        usageCount
      }
      orders {
        content {
          id
          orderNo
          user {
            id
            nickname
            phone
          }
          status
          amountCents
          promotionActivityName
          promotionDiscountCents
          createdAt
        }
        totalElements
        totalPages
        size
        number
      }
    }
  }
`;

// 限时特价活动使用情况与相关订单(F-MKT-005,订单按「商品+活动价+时间窗」匹配,无订单级优惠列)
export const FLASH_SALE_USAGE_PAGE_DATA_QUERY = gql`
  query FlashSaleUsagePageData($id: PrimaryId!, $input: MarketingUsageListInput) {
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
      createdAt
      updatedAt
    }
    flashSaleActivityUsage(flashSaleActivityId: $id, input: $input) {
      summary {
        orderCount
        benefitCents
        usageCount
      }
      orders {
        content {
          id
          orderNo
          user {
            id
            nickname
            phone
          }
          status
          amountCents
          createdAt
        }
        totalElements
        totalPages
        size
        number
      }
    }
  }
`;

// 买赠/满赠活动使用情况与相关订单(F-MKT-008,赠品随单快照 order_gifts)
export const GIFT_ACTIVITY_USAGE_PAGE_DATA_QUERY = gql`
  query GiftActivityUsagePageData($id: PrimaryId!, $input: MarketingUsageListInput) {
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
      createdAt
      updatedAt
    }
    giftActivityUsage(giftActivityId: $id, input: $input) {
      summary {
        orderCount
        benefitCents
        usageCount
      }
      orders {
        content {
          id
          orderNo
          user {
            id
            nickname
            phone
          }
          status
          amountCents
          giftItems {
            productName
            quantity
          }
          createdAt
        }
        totalElements
        totalPages
        size
        number
      }
    }
  }
`;

// 首单立减/新人专享价活动使用情况与相关订单(F-MKT-009,按活动类型分发)
export const FIRST_ORDER_USAGE_PAGE_DATA_QUERY = gql`
  query FirstOrderUsagePageData($id: PrimaryId!, $input: MarketingUsageListInput) {
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
      createdAt
      updatedAt
    }
    firstOrderActivityUsage(firstOrderActivityId: $id, input: $input) {
      summary {
        orderCount
        benefitCents
        usageCount
      }
      orders {
        content {
          id
          orderNo
          user {
            id
            nickname
            phone
          }
          status
          amountCents
          firstOrderActivityName
          firstOrderDiscountCents
          createdAt
        }
        totalElements
        totalPages
        size
        number
      }
    }
  }
`;
