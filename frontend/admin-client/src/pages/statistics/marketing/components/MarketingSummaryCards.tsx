import React from 'react';
import { Row, Col } from '@aquarush/admin-ui';
import StatisticsCard from '@/components/StatisticsCard';
import { centsToYuan } from '@aquarush/common';
import type { MarketingStatisticsSummaryVo } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  summaryRow: {
    marginTop: '16px',
  },
});

interface MarketingSummaryCardsProps {
  summary?: MarketingStatisticsSummaryVo | null;
  loading: boolean;
}

/**
 * 营销统计汇总卡(F-STA-007):
 * 营销订单数 / 让利总额(券+满减+首单立减,快照列精确口径)/ 用券数 / 水票销售金额 / 水票核销 / 赠品件数。
 * 金额以分存储、展示转元两位小数(R-MON-01);赠品估算与水票抵扣不计入让利总额(口径见 PRD)。
 */
const MarketingSummaryCards: React.FC<MarketingSummaryCardsProps> = ({ summary, loading }) => (
  <Row gutter={[16, 16]} {...stylex.props(styles.summaryRow)}>
    <Col xs={24} sm={12} lg={4}>
      <StatisticsCard title="营销订单数" value={summary?.marketingOrderCount ?? 0} suffix="单" loading={loading} />
    </Col>
    <Col xs={24} sm={12} lg={4}>
      <StatisticsCard
        title="让利总额"
        value={centsToYuan(summary?.totalBenefitCents ?? 0)}
        prefix="¥"
        precision={2}
        loading={loading}
      />
    </Col>
    <Col xs={24} sm={12} lg={4}>
      <StatisticsCard title="用券数" value={summary?.couponRedeemedCount ?? 0} suffix="张" loading={loading} />
    </Col>
    <Col xs={24} sm={12} lg={4}>
      <StatisticsCard
        title="水票销售金额"
        value={centsToYuan(summary?.ticketSalesRevenueCents ?? 0)}
        prefix="¥"
        precision={2}
        loading={loading}
      />
    </Col>
    <Col xs={24} sm={12} lg={4}>
      <StatisticsCard title="水票核销" value={summary?.ticketRedeemedCount ?? 0} suffix="张" loading={loading} />
    </Col>
    <Col xs={24} sm={12} lg={4}>
      <StatisticsCard title="赠品数量" value={summary?.giftQuantity ?? 0} suffix="件" loading={loading} />
    </Col>
  </Row>
);

export default MarketingSummaryCards;
