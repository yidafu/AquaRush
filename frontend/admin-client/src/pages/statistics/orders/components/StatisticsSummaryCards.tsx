import React from 'react';
import { Row, Col } from 'antd';
import StatisticsCard from '@/components/StatisticsCard';

export interface StatisticsSummaryCardsProps {
  loading: boolean;
  totalOrders: number;
  totalRevenue: number;
  avgOrderValue: number;
  completedOrders: number;
}

/** 订单统计汇总卡(statistics/orders/index.tsx 拆分) */
const StatisticsSummaryCards: React.FC<StatisticsSummaryCardsProps> = ({
  loading,
  totalOrders,
  totalRevenue,
  avgOrderValue,
  completedOrders,
}) => (
  <Row gutter={[16, 16]}>
    <Col xs={24} sm={12} lg={6}>
      <StatisticsCard title="订单总数" value={totalOrders} loading={loading} />
    </Col>
    <Col xs={24} sm={12} lg={6}>
      <StatisticsCard title="营收总额" value={totalRevenue} prefix="¥" loading={loading} />
    </Col>
    <Col xs={24} sm={12} lg={6}>
      <StatisticsCard title="平均客单价" value={avgOrderValue} prefix="¥" loading={loading} />
    </Col>
    <Col xs={24} sm={12} lg={6}>
      <StatisticsCard title="已完成订单" value={completedOrders} loading={loading} />
    </Col>
  </Row>
);

export default StatisticsSummaryCards;
