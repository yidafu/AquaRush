import React, { useState } from 'react';
import { Card, Space, DatePicker, Statistic, Row, Col, Spin, Empty, Alert, Tag, Button } from '@aquarush/admin-ui';
import { ArrowLeftOutlined } from '@aquarush/admin-ui/icons';
import { useQuery } from '@apollo/client';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import dayjs, { Dayjs } from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { centsToYuan } from '@aquarush/common';
import type { TicketTypeStatsVo } from '@aquarush/common';
import { TICKET_TYPE_STATS_QUERY } from '../../../../graphql/queries/marketing.graphql';
import { BRAND_PRIMARY, COLOR_ERROR, COLOR_SUCCESS } from '../../../../config/theme';
import ChartCard from '@/components/ChartCard';

const { RangePicker } = DatePicker;

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
  emptyBox: {
    paddingTop: '48px',
    paddingBottom: '48px',
    paddingLeft: '0px',
    paddingRight: '0px',
  },
});

interface SalesPoint {
  date: string;
  ticketCount: number;
  revenue: number;
}

interface UsagePoint {
  date: string;
  redeemedCount: number;
  returnedCount: number;
  expiredCount: number;
}

/**
 * 桶水票票种详情页:销售/使用统计图表(F-MKT-007)
 * 独立页面(替代原抽屉):单请求取回汇总 + 销售/使用按日序列(规则 #10);时间范围可切换(默认近 30 天)。
 * 路由:/marketing/water-tickets/:id?name=&kind=
 */
const WaterTicketDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const ticketTypeName = searchParams.get('name') || '水票';
  const ticketTypeKind = searchParams.get('kind') || undefined;

  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(30, 'day'),
    dayjs(),
  ]);

  const variables = id
    ? {
        ticketTypeId: id,
        input: {
          startDate: dateRange[0].format('YYYY-MM-DDTHH:mm:ss'),
          endDate: dateRange[1].format('YYYY-MM-DDTHH:mm:ss'),
        },
      }
    : null;

  const { data, loading, error } = useQuery<{ ticketTypeStats: TicketTypeStatsVo }>(
    TICKET_TYPE_STATS_QUERY,
    {
      variables: variables ?? undefined,
      skip: !id,
      fetchPolicy: 'cache-and-network',
      errorPolicy: 'all',
    }
  );

  const stats = data?.ticketTypeStats;
  const summary = stats?.summary;

  const salesData: SalesPoint[] = (stats?.sales ?? []).map((s) => ({
    date: s.date.slice(5),
    ticketCount: s.ticketCount,
    revenue: centsToYuan(s.revenueCents),
  }));

  const usageData: UsagePoint[] = (stats?.usage ?? []).map((u) => ({
    date: u.date.slice(5),
    redeemedCount: u.redeemedCount,
    returnedCount: u.returnedCount,
    expiredCount: u.expiredCount,
  }));

  return (
    <Card
      title={
        <Space>
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/marketing/water-tickets')}
          />
          <span>{ticketTypeName} · 销售与使用统计</span>
          {ticketTypeKind && (
            <Tag color={ticketTypeKind === 'SPECIFIC_PRODUCT' ? 'blue' : 'purple'}>
              {ticketTypeKind === 'SPECIFIC_PRODUCT' ? '指定商品' : '通兑票'}
            </Tag>
          )}
        </Space>
      }
      extra={
        <RangePicker
          value={dateRange}
          allowClear={false}
          onChange={(dates) => {
            if (dates && dates[0] && dates[1]) {
              setDateRange([dates[0], dates[1]]);
            }
          }}
        />
      }
    >
      {error && <Alert type="error" showIcon message="统计加载失败" description={error.message} />}

      <Spin spinning={loading}>
        {!summary ? (
          <Empty description="暂无统计" {...stylex.props(styles.emptyBox)} />
        ) : (
          <Space direction="vertical" size={24} {...stylex.props(styles.fullWidth)}>
            {/* 区间汇总卡(与所选时间范围一致;availableCount 为当前存量) */}
            <Row gutter={16}>
              <Col span={8}>
                <Card size="small">
                  <Statistic title="区间销售张数" value={summary.soldCount} suffix="张" />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic
                    title="区间销售金额"
                    value={centsToYuan(summary.revenueCents)}
                    precision={2}
                    prefix="¥"
                  />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic title="当前可用" value={summary.availableCount} suffix="张" />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic title="区间核销" value={summary.redeemedCount} suffix="张" />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic title="区间退回" value={summary.returnedCount} suffix="张" />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic title="区间过期" value={summary.expiredCount} suffix="张" />
                </Card>
              </Col>
            </Row>

            {/* 销售趋势:入账张数(柱)+ 销售金额(线,元) */}
            <ChartCard title="销售趋势" size="small" height={320}>
              <BarChart data={salesData} margin={{ top: 5, right: 30, left: 10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                <YAxis yAxisId="left" tick={{ fontSize: 12 }} />
                <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12 }} tickFormatter={(v: number) => `¥${v}`} />
                <Tooltip
                  labelFormatter={(label) => `日期: ${label}`}
                  formatter={(value, name) => {
                    const num = typeof value === 'number' ? value : 0;
                    if (name === 'ticketCount') return [`${num} 张`, '销售张数'];
                    return [`¥${num.toFixed(2)}`, '销售金额'];
                  }}
                />
                <Legend />
                <Bar yAxisId="left" dataKey="ticketCount" name="销售张数" fill={BRAND_PRIMARY} radius={[4, 4, 0, 0]} />
                <Line yAxisId="right" type="monotone" dataKey="revenue" name="销售金额" stroke={COLOR_SUCCESS} strokeWidth={2} dot={{ r: 2 }} />
              </BarChart>
            </ChartCard>

            {/* 使用情况:核销/退回/过期 */}
            <ChartCard title="使用情况" size="small" height={320}>
              <LineChart data={usageData} margin={{ top: 5, right: 30, left: 10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip
                  labelFormatter={(label) => `日期: ${label}`}
                  formatter={(value, name) => [`${typeof value === 'number' ? value : 0} 张`, String(name)]}
                />
                <Legend />
                <Line type="monotone" dataKey="redeemedCount" name="核销" stroke={BRAND_PRIMARY} strokeWidth={2} dot={{ r: 2 }} />
                <Line type="monotone" dataKey="returnedCount" name="退回" stroke="#faad14" strokeWidth={2} dot={{ r: 2 }} />
                <Line type="monotone" dataKey="expiredCount" name="过期" stroke={COLOR_ERROR} strokeWidth={2} dot={{ r: 2 }} />
              </LineChart>
            </ChartCard>
          </Space>
        )}
      </Spin>
    </Card>
  );
};

export default WaterTicketDetailPage;
