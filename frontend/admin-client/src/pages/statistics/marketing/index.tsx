import React, { useState } from 'react';
import { Alert, Button, Space, DatePicker, Row, Col } from '@/ui';
import { useQuery } from '@apollo/client';
import { centsToYuan } from '@aquarush/common';
import type { MarketingStatisticsVo as MarketingStatisticsVO } from '@aquarush/common';
import RefreshControls from '@/components/RefreshControls';
import { useAutoRefresh } from '@/hooks/useAutoRefresh';
import dayjs, { Dayjs } from 'dayjs';
import { GET_MARKETING_STATISTICS, type MarketingStatisticsData } from './data';
import {
  MarketingSummaryCards,
  MarketingBenefitTrendChart,
  MarketingOrderTrendChart,
  TicketTrendChart,
  MarketingCompositionPieChart,
  buildCompositionData,
  MarketingUsageBreakdownTable,
} from './components';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

// .page-title(index.css 自研类)内联进组件(原 margin: 0 拆 longhand)
const styles = stylex.create({
  pageTitle: {
    marginTop: '0px',
    marginRight: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    fontSize: tokens.fsPage,
    lineHeight: tokens.lhPage,
    fontWeight: 600,
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  filterBar: {
    marginTop: '16px',
  },
  errorAlert: {
    marginTop: '24px',
  },
});

const { RangePicker } = DatePicker;

/**
 * 营销统计页(F-STA-007,statistics/marketing):
 * 时间范围筛选(默认近 30 天)+ 汇总卡 + 优惠/订单/水票趋势 + 营销方式构成 + 营销方式使用明细表;
 * 数据来自第 3 层日汇总中间表查询侧两段合并,单页面单 GraphQL 请求(AGENTS.md 规则 7)。
 */
const MarketingStatisticsPage: React.FC = () => {
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(29, 'day'),
    dayjs(),
  ]);

  const variables = {
    input: {
      startDate: dateRange[0].startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
      endDate: dateRange[1].endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    },
  };

  const { data, loading, error, refetch } = useQuery<MarketingStatisticsData>(GET_MARKETING_STATISTICS, {
    variables,
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });

  const { intervalMs, setIntervalMs, refreshing, refresh, lastUpdated } = useAutoRefresh({
    onRefresh: () => refetch(),
  });

  const stats: MarketingStatisticsVO | undefined = data?.marketingStatistics;
  const summary = stats?.summary;
  const daily = stats?.daily ?? [];

  const benefitTrend = daily.map((d) => ({
    date: d.date.slice(5),
    coupon: centsToYuan(d.couponBenefitCents),
    promotion: centsToYuan(d.promotionBenefitCents),
    firstOrder: centsToYuan(d.firstOrderBenefitCents),
  }));

  const orderTrend = daily.map((d) => ({
    date: d.date.slice(5),
    orders: d.marketingOrderCount,
    coupons: d.couponRedeemedCount,
  }));

  const ticketTrend = daily.map((d) => ({
    date: d.date.slice(5),
    sales: d.ticketSalesCount,
    redeemed: d.ticketRedeemedCount,
  }));

  const compositionData = buildCompositionData({
    couponBenefitCents: summary?.couponBenefitCents ?? 0,
    promotionBenefitCents: summary?.promotionBenefitCents ?? 0,
    firstOrderBenefitCents: summary?.firstOrderBenefitCents ?? 0,
  });

  return (
    <div>
      <div {...stylex.props(styles.header)}>
        <h1 {...stylex.props(styles.pageTitle)}>营销统计</h1>
        <RefreshControls
          interval={intervalMs}
          onIntervalChange={setIntervalMs}
          onRefresh={refresh}
          refreshing={refreshing}
          lastUpdated={lastUpdated}
        />
      </div>

      <Space {...stylex.props(styles.filterBar)} wrap>
        <span>时间范围:</span>
        <RangePicker
          value={dateRange}
          allowClear={false}
          disabledDate={(current) => current.isAfter(dayjs(), 'day')}
          onChange={(dates) => {
            if (dates && dates[0] && dates[1]) {
              setDateRange([dates[0], dates[1]]);
            }
          }}
        />
      </Space>

      {error && (
        <Alert
          {...stylex.props(styles.errorAlert)}
          type="error"
          showIcon
          message="数据加载失败"
          description={error.message}
          action={
            <Button size="small" onClick={() => refetch()}>
              重试
            </Button>
          }
        />
      )}

      <MarketingSummaryCards summary={summary} loading={loading} />

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={14}>
          <MarketingBenefitTrendChart data={benefitTrend} loading={loading} />
        </Col>
        <Col xs={24} lg={10}>
          <MarketingCompositionPieChart data={compositionData} loading={loading} />
        </Col>
        <Col xs={24} lg={12}>
          <MarketingOrderTrendChart data={orderTrend} loading={loading} />
        </Col>
        <Col xs={24} lg={12}>
          <TicketTrendChart data={ticketTrend} loading={loading} />
        </Col>
      </Row>

      <MarketingUsageBreakdownTable rows={stats?.breakdown ?? []} loading={loading} />
    </div>
  );
};

export default MarketingStatisticsPage;
