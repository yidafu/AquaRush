import React, { useMemo, useState } from 'react';
import { Alert, Button, Col, DatePicker, Row, Segmented, Space, Tooltip } from 'antd';
import { ClockCircleOutlined, DollarOutlined, ShoppingCartOutlined } from '@ant-design/icons';
import { useQuery } from '@apollo/client';
import { centsToYuan, normalizeCents } from '@aquarush/common';
import type { DailyStatisticVo, PendingOrderBreakdownVo } from '@aquarush/common';
import dayjs, { Dayjs } from 'dayjs';
import StatisticsCard from '../../components/StatisticsCard';
import RefreshControls from '../../components/RefreshControls';
import { useAutoRefresh } from '../../hooks/useAutoRefresh';
import DailyRevenueChart from './components/DailyRevenueChart';
import DailyOrderChart from './components/DailyOrderChart';
import { DASHBOARD_PAGE_DATA_QUERY } from '../../graphql/queries/dashboard.graphql';

interface DashboardPageData {
  dailyStatistics: DailyStatisticVo[];
  pendingOrderBreakdown: PendingOrderBreakdownVo;
}

type Preset = '7D' | '30D' | 'MONTH';

interface TrendInfo {
  trend: 'up' | 'down' | 'neutral';
  value: number | null; // null 表示上期无数据，显示「—」
}

/**
 * 后端 Long 字段在网络上以字符串序列化（见 common normalizeCents 注释），统一归一为数字
 */
const toSafeNumber = (value: number | string | null | undefined): number => {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
};

/**
 * 环比计算（PRD 规则）：(本期 − 上期) / 上期 × 100%，保留 1 位小数；
 * 上期为 0 且本期 > 0 时返回 null（「—」，避免除零误导）
 */
const calcTrend = (current: number, prev: number): TrendInfo => {
  if (!Number.isFinite(current) || !Number.isFinite(prev)) {
    return { trend: 'neutral', value: null };
  }
  if (prev === 0) {
    return current > 0 ? { trend: 'neutral', value: null } : { trend: 'neutral', value: 0 };
  }
  const rate = ((current - prev) / prev) * 100;
  const value = Math.round(rate * 10) / 10;
  return { trend: value > 0 ? 'up' : value < 0 ? 'down' : 'neutral', value };
};

const sumRevenueCents = (items: Array<{ revenueCents: number | string | null | undefined }>) =>
  items.reduce((sum, item) => sum + (normalizeCents(item.revenueCents) ?? 0), 0);

const Dashboard: React.FC = () => {
  // 时间范围：预设（近7天/近30天/本月）+ 自定义，默认近30天
  const [preset, setPreset] = useState<Preset | null>('30D');
  const [range, setRange] = useState<[Dayjs, Dayjs]>([dayjs().subtract(29, 'day'), dayjs()]);

  const handlePreset = (value: Preset) => {
    const now = dayjs();
    const start =
      value === '7D' ? now.subtract(6, 'day') : value === '30D' ? now.subtract(29, 'day') : now.startOf('month');
    setRange([start, now]);
    setPreset(value);
  };

  // 扩展查询窗口：单次查询覆盖「昨日」(较昨日环比) 与「上月同期」(较上月同期环比)，图表/卡片只取所选范围切片
  const prevDayStart = range[0].subtract(1, 'day');
  const prevMonthStart = range[0].subtract(1, 'month');
  const queryStart = prevDayStart.isBefore(prevMonthStart) ? prevDayStart : prevMonthStart;
  const input = {
    startDate: queryStart.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    endDate: range[1].endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
  };

  // 单页单请求:日统计(时间范围)+ 待处理订单存量快照一次取回(规则 #10)
  const { data, loading, error, refetch } = useQuery<DashboardPageData>(DASHBOARD_PAGE_DATA_QUERY, {
    variables: { input },
  });

  // 手动刷新 + 定时更新
  const { intervalMs, setIntervalMs, refreshing, refresh, lastUpdated } = useAutoRefresh({
    onRefresh: () => refetch(),
  });

  // 所选范围切片（YYYY-MM-DD 字符串字典序比较即日期比较）；线上 Long 为字符串，统一归一为数字
  const sliced = useMemo(() => {
    const startKey = range[0].format('YYYY-MM-DD');
    const endKey = range[1].format('YYYY-MM-DD');
    return (data?.dailyStatistics ?? [])
      .filter((d) => d.date >= startKey && d.date <= endKey)
      .map((d) => ({
        date: d.date,
        orderCount: toSafeNumber(d.orderCount),
        orderProductCount: toSafeNumber(d.orderProductCount),
        revenueCents: toSafeNumber(d.revenueCents),
      }));
  }, [data, range]);

  // 今日订单/今日营收：取切片最后一条（昨日缺失即自定义单日区间时环比显示「—」）
  const lastDay = sliced[sliced.length - 1];
  const prevDay = sliced[sliced.length - 2];
  const todayOrders = lastDay?.orderCount ?? 0;
  const todayRevenueCents = lastDay?.revenueCents ?? 0;
  const orderTrend: TrendInfo = prevDay
    ? calcTrend(todayOrders, prevDay.orderCount)
    : { trend: 'neutral', value: null };
  const revenueTrend: TrendInfo = prevDay
    ? calcTrend(todayRevenueCents, prevDay.revenueCents)
    : { trend: 'neutral', value: null };

  // 本月累计营收：所选范围累加；环比基准 = 所选范围平移一个月（近30天→前30天、本月→上月1日至同日、近7天→前7天）
  const currentRangeRevenue = sumRevenueCents(sliced);
  const prevRangeStart = range[0].subtract(1, 'month');
  const prevRangeEnd = range[1].subtract(1, 'month');
  const prevMonthRevenue = (data?.dailyStatistics ?? [])
    .filter((d) => d.date >= prevRangeStart.format('YYYY-MM-DD') && d.date <= prevRangeEnd.format('YYYY-MM-DD'))
    .reduce((sum, item) => sum + (normalizeCents(item.revenueCents) ?? 0), 0);
  const totalRevenueTrend = calcTrend(currentRangeRevenue, prevMonthRevenue);

  const pending = data?.pendingOrderBreakdown;
  // 后端按日期范围逐日返回，无订单的日期补零（见 StatisticsServiceImpl.getDailyStatistics）；
  // 只要存在日期行（即使全部为 0）就渲染图表，仅在完全没有日期行时展示空态
  const isEmpty = sliced.length === 0;

  if (error) {
    return (
      <div>
        <Alert
          style={{ marginTop: 24 }}
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
      </div>
    );
  }

  const rangeLabel = preset === '7D' ? '最近7天' : preset === '30D' ? '最近30天' : preset === 'MONTH' ? '本月' : '所选范围';

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 className="page-title">仪表盘</h1>
        <RefreshControls
          interval={intervalMs}
          onIntervalChange={setIntervalMs}
          onRefresh={refresh}
          refreshing={refreshing}
          lastUpdated={lastUpdated}
        />
      </div>
      <Space style={{ marginTop: 24 }} wrap>
        <Segmented
          options={[
            { label: '近7天', value: '7D' },
            { label: '近30天', value: '30D' },
            { label: '本月', value: 'MONTH' },
          ]}
          value={preset ?? undefined}
          onChange={(value) => handlePreset(value as Preset)}
        />
        <DatePicker.RangePicker
          value={range}
          onChange={(dates) => {
            if (dates && dates[0] && dates[1]) {
              setRange([dates[0], dates[1]]);
              setPreset(null);
            }
          }}
          disabledDate={(current) => current.isAfter(dayjs(), 'day')}
        />
      </Space>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="今日订单"
            value={todayOrders}
            prefix={<ShoppingCartOutlined />}
            loading={loading}
            trend={orderTrend.trend}
            trendValue={orderTrend.value}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="今日营收"
            value={centsToYuan(todayRevenueCents)}
            prefix={<DollarOutlined />}
            suffix="元"
            precision={2}
            loading={loading}
            trend={revenueTrend.trend}
            trendValue={revenueTrend.value}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="本月累计营收"
            value={centsToYuan(currentRangeRevenue)}
            prefix={<DollarOutlined />}
            suffix="元"
            precision={2}
            loading={loading}
            trend={totalRevenueTrend.trend}
            trendValue={totalRevenueTrend.value}
            trendLabel="较上月同期"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Tooltip
            title={`待支付 ${pending?.pendingPayment ?? 0} · 待派单 ${pending?.pendingDispatch ?? 0} · 待配送 ${pending?.pendingDelivery ?? 0}`}
          >
            <StatisticsCard
              title="待处理订单"
              value={pending?.total ?? 0}
              prefix={<ClockCircleOutlined />}
              loading={loading}
            />
          </Tooltip>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={24}>
          <DailyRevenueChart data={sliced} title={`${rangeLabel}营收趋势`} loading={loading} empty={isEmpty} />
        </Col>
      </Row>

      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col span={24}>
          <DailyOrderChart data={sliced} title={`${rangeLabel}订单数`} loading={loading} empty={isEmpty} />
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
