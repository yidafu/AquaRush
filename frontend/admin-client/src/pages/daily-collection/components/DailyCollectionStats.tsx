import React from 'react';
import { Row, Col } from '@aquarush/admin-ui';
import { centsToYuan } from '@aquarush/common';
import StatisticsCard from '../../../components/StatisticsCard';

export interface DailyCollectionStatsData {
  totalCash: number;
  totalQrCode: number;
  totalWaterTicket: number;
  totalOrderAmount: number;
}

export interface DailyCollectionStatsProps {
  stats: DailyCollectionStatsData;
}

/** 顶部统计卡(今日现金/扫码/水票/订单金额,daily-collection/index.tsx 拆分;复用 StatisticsCard 外壳) */
const DailyCollectionStats: React.FC<DailyCollectionStatsProps> = ({ stats }) => (
  <Row gutter={16}>
    <Col span={6}>
      <StatisticsCard title="现金收款" value={centsToYuan(stats.totalCash)} precision={2} prefix="¥" />
    </Col>
    <Col span={6}>
      <StatisticsCard title="扫码收款" value={centsToYuan(stats.totalQrCode)} precision={2} prefix="¥" />
    </Col>
    <Col span={6}>
      <StatisticsCard title="水票数量" value={stats.totalWaterTicket} />
    </Col>
    <Col span={6}>
      <StatisticsCard title="订单金额" value={centsToYuan(stats.totalOrderAmount)} precision={2} prefix="¥" />
    </Col>
  </Row>
);

export default DailyCollectionStats;
