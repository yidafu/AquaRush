import React from 'react';
import { Card, Col, Row, Statistic } from '@/ui';
import { centsToYuan } from '@aquarush/common';
import type { MarketingUsageSummaryVo } from '@aquarush/common';

interface MarketingUsageSummaryCardsProps {
  /** 使用情况汇总(加载中/未返回时为 null) */
  summary?: MarketingUsageSummaryVo | null;
  loading?: boolean;
  /** 优惠金额卡片标题(默认「优惠金额」) */
  benefitLabel?: string;
  /** 使用数量卡片标题(默认「使用数量」) */
  usageLabel?: string;
  /** 使用数量后缀(如「张」) */
  usageSuffix?: string;
}

/**
 * 营销方式详情页「营销使用情况」汇总卡(F-MKT-003~009):
 * 使用订单数 / 优惠金额(元)/ 使用数量;金额以分存储、展示转元(R-MON-01)。
 */
const MarketingUsageSummaryCards: React.FC<MarketingUsageSummaryCardsProps> = ({
  summary,
  loading,
  benefitLabel = '优惠金额',
  usageLabel = '使用数量',
  usageSuffix = '',
}) => (
  <Row gutter={16}>
    <Col span={8}>
      <Card size="small" loading={loading}>
        <Statistic title="使用订单数" value={summary?.orderCount ?? 0} suffix="单" />
      </Card>
    </Col>
    <Col span={8}>
      <Card size="small" loading={loading}>
        <Statistic title={benefitLabel} value={centsToYuan(summary?.benefitCents ?? 0)} precision={2} prefix="¥" />
      </Card>
    </Col>
    <Col span={8}>
      <Card size="small" loading={loading}>
        <Statistic title={usageLabel} value={summary?.usageCount ?? 0} suffix={usageSuffix} />
      </Card>
    </Col>
  </Row>
);

export default MarketingUsageSummaryCards;
