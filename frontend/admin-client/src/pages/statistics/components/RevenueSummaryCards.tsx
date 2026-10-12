import React from 'react';
import { Row, Col } from '@/ui';
import StatisticsCard from '@/components/StatisticsCard';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  summaryRow: {
    marginTop: '24px',
  },
  detailRow: {
    marginTop: '16px',
  },
});

export interface RevenueSummaryCardsProps {
  loading: boolean;
  totalRevenueYuan: number;
  averageDailyRevenueYuan: number;
  maxPeriodRevenueYuan: number;
  maxPeriodTitle: string;
  totalRefundYuan: number;
  netRevenueYuan: number;
  /** 退款率文案(营收为 0 时为「—」);null 也展示「—」 */
  refundRate: string | null;
}

/** 营收统计汇总卡(statistics/index.tsx 拆分):营收/日均/最大周期 + 退款/净营收/退款率 */
const RevenueSummaryCards: React.FC<RevenueSummaryCardsProps> = ({
  loading,
  totalRevenueYuan,
  averageDailyRevenueYuan,
  maxPeriodRevenueYuan,
  maxPeriodTitle,
  totalRefundYuan,
  netRevenueYuan,
  refundRate,
}) => (
  <>
    <Row gutter={[16, 16]} {...stylex.props(styles.summaryRow)}>
      <Col xs={24} sm={12} lg={8}>
        <StatisticsCard
          title="营收总额"
          value={totalRevenueYuan}
          prefix="¥"
          precision={2}
          loading={loading}
        />
      </Col>
      <Col xs={24} sm={12} lg={8}>
        <StatisticsCard
          title="日均营收"
          value={averageDailyRevenueYuan}
          prefix="¥"
          precision={2}
          loading={loading}
        />
      </Col>
      <Col xs={24} sm={12} lg={8}>
        <StatisticsCard
          title={maxPeriodTitle}
          value={maxPeriodRevenueYuan}
          prefix="¥"
          precision={2}
          loading={loading}
        />
      </Col>
    </Row>

    {/* 退款/净营收维度(PRD F-STA-002,G01 支付链路落地后补充):退款金额 = payment_refunds status=COMPLETED,
        净营收 = 营收 − 退款金额,与退款记录一致;退款率 = 退款金额 / 营收总额,营收为 0 时显示「—」 */}
    <Row gutter={[16, 16]} {...stylex.props(styles.detailRow)}>
      <Col xs={24} sm={12} lg={8}>
        <StatisticsCard
          title="退款金额"
          value={totalRefundYuan}
          prefix="¥"
          precision={2}
          loading={loading}
        />
      </Col>
      <Col xs={24} sm={12} lg={8}>
        <StatisticsCard
          title="净营收"
          value={netRevenueYuan}
          prefix="¥"
          precision={2}
          loading={loading}
        />
      </Col>
      <Col xs={24} sm={12} lg={8}>
        <StatisticsCard
          title="退款率"
          value={refundRate !== null ? `${refundRate}%` : '—'}
          loading={loading}
        />
      </Col>
    </Row>
  </>
);

export default RevenueSummaryCards;
