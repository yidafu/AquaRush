import { gql } from '@apollo/client';
import { MarketingBreakdownKind } from '@aquarush/common';
import type { MarketingStatisticsVo } from '@aquarush/common';

/**
 * 营销统计页(MarketingStatisticsPage)单页查询(F-STA-007):
 * 一次请求取回「汇总 + 按日趋势 + 营销方式使用明细」,满足 AGENTS.md 规则 7。
 * 数据源:第 3 层日汇总中间表(daily_marketing_facts / daily_marketing_usage_facts)查询侧两段合并。
 */
export const GET_MARKETING_STATISTICS = gql`
  query GetMarketingStatistics($input: DateRangeInput!) {
    marketingStatistics(input: $input) {
      summary {
        marketingOrderCount
        totalBenefitCents
        couponBenefitCents
        promotionBenefitCents
        firstOrderBenefitCents
        couponRedeemedCount
        ticketSalesCount
        ticketSalesRevenueCents
        ticketRedeemedCount
        giftQuantity
      }
      daily {
        date
        marketingOrderCount
        couponBenefitCents
        promotionBenefitCents
        firstOrderBenefitCents
        couponRedeemedCount
        ticketSalesCount
        ticketSalesRevenueCents
        ticketRedeemedCount
        giftQuantity
      }
      breakdown {
        kind
        refId
        name
        orderCount
        benefitCents
        usageCount
        ticketSalesCount
        ticketSalesRevenueCents
        ticketRedeemedCount
      }
    }
  }
`;

export interface MarketingStatisticsData {
  marketingStatistics: MarketingStatisticsVo;
}

/** 营销方式使用明细 Segmented 选项与列配置 */
export interface BreakdownTabConfig {
  kind: MarketingBreakdownKind;
  label: string;
  /** 明细表第一列标题(券=券名称,票种=票种名称,其余=活动名称) */
  nameColumn: string;
  /** 优惠金额列标题(票种=销售金额) */
  benefitColumn: string;
  /** 使用数量列标题(票种=核销张数) */
  usageColumn: string;
  /** 使用数量后缀 */
  usageSuffix: string;
}

export const BREAKDOWN_TABS: BreakdownTabConfig[] = [
  { kind: MarketingBreakdownKind.COUPON_TEMPLATE, label: '优惠券', nameColumn: '券名称', benefitColumn: '优惠金额', usageColumn: '核销张数', usageSuffix: '张' },
  { kind: MarketingBreakdownKind.PROMOTION_ACTIVITY, label: '满减/折扣', nameColumn: '活动名称', benefitColumn: '优惠金额', usageColumn: '使用数量', usageSuffix: '单' },
  { kind: MarketingBreakdownKind.FIRST_ORDER_ACTIVITY, label: '首单立减', nameColumn: '活动名称', benefitColumn: '优惠金额', usageColumn: '使用数量', usageSuffix: '单' },
  { kind: MarketingBreakdownKind.GIFT_ACTIVITY, label: '买赠/满赠', nameColumn: '活动名称', benefitColumn: '优惠金额', usageColumn: '赠品件数', usageSuffix: '件' },
  { kind: MarketingBreakdownKind.TICKET_TYPE, label: '桶水票', nameColumn: '票种名称', benefitColumn: '销售金额', usageColumn: '核销张数', usageSuffix: '张' },
];

/** 营销方式 → 详情页路由前缀(明细表「详情」跳转) */
export const BREAKDOWN_DETAIL_PREFIX: Record<string, string> = {
  COUPON_TEMPLATE: '/marketing/coupons',
  PROMOTION_ACTIVITY: '/marketing/promotions',
  FIRST_ORDER_ACTIVITY: '/marketing/newcomer',
  GIFT_ACTIVITY: '/marketing/buy-gift',
  TICKET_TYPE: '/marketing/water-tickets',
};
