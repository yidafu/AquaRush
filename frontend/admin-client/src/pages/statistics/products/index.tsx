import React, { useState } from 'react';
import { Card, Row, Col, Spin, Select, Space, DatePicker } from 'antd';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { useQuery } from '@apollo/client';
import { centsToYuan } from '@aquarush/common';
import StatisticsCard from '@/components/StatisticsCard';
import RefreshControls from '@/components/RefreshControls';
import { useAutoRefresh } from '@/hooks/useAutoRefresh';
import { PRODUCT_STATISTICS_PAGE_DATA_QUERY } from '../../../graphql/queries/statistics.graphql';
import dayjs, { Dayjs } from 'dayjs';

const { RangePicker } = DatePicker;

interface ProductSalesStat {
  productId: number;
  productName: string;
  salesVolume: number;
  revenueCents: number;
}

interface ProductFavoriteStat {
  productId: number;
  productName: string;
  favoriteCount: number;
}

interface ProductDailySalesData {
  date: string;
  productId: number;
  productName: string;
  salesVolume: number;
  revenueCents: number;
}

interface ProductStatisticsPageData {
  productStatistics: {
    totalProducts: number;
    onlineProducts: number;
    offlineProducts: number;
    lowStockProducts: number;
    totalSales: number;
    totalFavorites: number;
    productRanking: ProductSalesStat[];
    favoriteStatistics: ProductFavoriteStat[];
  };
  productDailySales: ProductDailySalesData[];
  productsPaginated: {
    list: { id: number; name: string }[];
  };
}

const ProductStatisticsPage: React.FC = () => {
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(30, 'day'),
    dayjs(),
  ]);
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);

  const startDate = dateRange[0].format('YYYY-MM-DDTHH:mm:ss');
  const endDate = dateRange[1].format('YYYY-MM-DDTHH:mm:ss');

  // 单页单请求:商品统计 + 日销量 + 商品目录一次取回(规则 #10);
  // 日期变化经 variables 变化自动重取,无需单独 refetch
  const { data, loading, error, refetch } = useQuery<ProductStatisticsPageData>(PRODUCT_STATISTICS_PAGE_DATA_QUERY, {
    variables: {
      input: {
        startDate,
        endDate,
      },
    },
    errorPolicy: 'all',
  });

  // 手动刷新 + 定时更新
  const { intervalMs, setIntervalMs, refreshing, refresh, lastUpdated } = useAutoRefresh({
    onRefresh: () => refetch(),
  });

  const statsData = data
  const dailySalesData = data
  const productsData = data
  const statsLoading = loading
  const dailySalesLoading = loading

  if (error) {
    return <div>加载失败: {error.message}</div>;
  }

  const stats = statsData?.productStatistics;

  // Prepare chart data for sales ranking
  const salesChartData = stats?.productRanking.map((item) => ({
    name: item.productName.length > 10 ? item.productName.slice(0, 10) + '...' : item.productName,
    sales: item.salesVolume,
    revenue: centsToYuan(item.revenueCents),
  })) || [];

  // Prepare chart data for favorite ranking
  const favoriteChartData = stats?.favoriteStatistics.map((item) => ({
    name: item.productName.length > 10 ? item.productName.slice(0, 10) + '...' : item.productName,
    favorites: item.favoriteCount,
  })) || [];

  // Prepare daily sales trend chart data
  const dailySales = dailySalesData?.productDailySales || [];

  // Aggregate daily sales by date (for all products combined trend)
  const dailyTrendMap = new Map<string, { date: string; sales: number; revenue: number }>();
  dailySales.forEach((item) => {
    const existing = dailyTrendMap.get(item.date);
    if (existing) {
      existing.sales += item.salesVolume;
      existing.revenue += item.revenueCents;
    } else {
      dailyTrendMap.set(item.date, {
        date: item.date,
        sales: item.salesVolume,
        revenue: item.revenueCents,
      });
    }
  });

  const dailyTrendChartData = Array.from(dailyTrendMap.values())
    .sort((a, b) => a.date.localeCompare(b.date))
    .map((item) => ({
      date: item.date.slice(5), // Show MM-DD format
      sales: item.sales,
      revenue: centsToYuan(item.revenue),
    }));

  // If a specific product is selected, show its daily trend
  const selectedProductDailySales = selectedProductId
    ? dailySales.filter((item) => item.productId === selectedProductId)
    : [];

  const selectedProductTrendData = selectedProductId
    ? selectedProductDailySales
        .sort((a, b) => a.date.localeCompare(b.date))
        .map((item) => ({
          date: item.date.slice(5),
          sales: item.salesVolume,
          revenue: centsToYuan(item.revenueCents),
        }))
    : [];

  const productList = productsData?.productsPaginated?.list || [];

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, margin: 0 }}>商品统计</h1>
        <RefreshControls
          interval={intervalMs}
          onIntervalChange={setIntervalMs}
          onRefresh={refresh}
          refreshing={refreshing}
          lastUpdated={lastUpdated}
        />
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="商品总数"
            value={stats?.totalProducts ?? 0}
            loading={statsLoading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="在售商品"
            value={stats?.onlineProducts ?? 0}
            loading={statsLoading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="低库存预警"
            value={stats?.lowStockProducts ?? 0}
            loading={statsLoading}
            trend={stats?.lowStockProducts && stats.lowStockProducts > 0 ? 'up' : 'neutral'}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="总销量"
            value={stats?.totalSales ?? 0}
            suffix="桶"
            loading={statsLoading}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="总收藏数"
            value={stats?.totalFavorites ?? 0}
            loading={statsLoading}
          />
        </Col>
      </Row>

      {/* Daily Sales Trend Chart */}
      <Card
        title="日销量趋势"
        style={{ marginTop: 24 }}
        extra={
          <Space>
            <Select
              placeholder="选择商品（可选）"
              allowClear
              style={{ width: 200 }}
              value={selectedProductId}
              onChange={setSelectedProductId}
              options={productList.map((p: { id: number; name: string }) => ({
                value: p.id,
                label: p.name,
              }))}
            />
            <RangePicker
              value={dateRange}
              onChange={(dates) => {
                if (dates && dates[0] && dates[1]) {
                  setDateRange([dates[0], dates[1]]);
                }
              }}
            />
          </Space>
        }
      >
        <Spin spinning={dailySalesLoading}>
          <ResponsiveContainer width="100%" height={350}>
            <LineChart data={selectedProductId ? selectedProductTrendData : dailyTrendChartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis yAxisId="left" tick={{ fontSize: 12 }} />
              <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12 }} tickFormatter={(v) => `¥${v}`} />
              <Tooltip
                labelFormatter={(label) => `日期: ${label}`}
                formatter={(value, name) => {
                  const numValue = typeof value === 'number' ? value : 0;
                  const strName = typeof name === 'string' ? name : 'unknown';
                  if (strName === 'sales') return [`${numValue} 桶`, '销量'];
                  return [`¥${numValue.toFixed(2)}`, '营收'];
                }}
              />
              <Legend />
              <Line yAxisId="left" type="monotone" dataKey="sales" name="销量" stroke="#1890ff" strokeWidth={2} dot={{ fill: '#1890ff', r: 3 }} />
              <Line yAxisId="right" type="monotone" dataKey="revenue" name="营收" stroke="#52c41a" strokeWidth={2} dot={{ fill: '#52c41a', r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </Spin>
      </Card>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="商品销量排行 TOP10">
            <Spin spinning={statsLoading}>
              <ResponsiveContainer width="100%" height={350}>
                <BarChart data={salesChartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" tick={{ fontSize: 10 }} angle={-45} textAnchor="end" height={80} />
                  <YAxis yAxisId="left" tick={{ fontSize: 12 }} />
                  <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12 }} tickFormatter={(v) => `¥${v}`} />
                  <Tooltip
                    labelFormatter={(label) => `商品: ${label}`}
                    formatter={(value, name) => {
                      if (name === 'sales') return [`${Number(value)} 桶`, '销量'];
                      return [`¥${Number(value).toFixed(2)}`, '营收'];
                    }}
                  />
                  <Legend />
                  <Bar yAxisId="left" dataKey="sales" name="销量" fill="#1890ff" radius={[4, 4, 0, 0]} />
                  <Bar yAxisId="right" dataKey="revenue" name="营收" fill="#52c41a" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </Spin>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="商品收藏排行 TOP10">
            <Spin spinning={statsLoading}>
              <ResponsiveContainer width="100%" height={350}>
                <BarChart data={favoriteChartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" tick={{ fontSize: 10 }} angle={-45} textAnchor="end" height={80} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip
                    labelFormatter={(label) => `商品: ${label}`}
                    formatter={(value) => [`${Number(value)} 人`, '收藏数']}
                  />
                  <Legend />
                  <Bar dataKey="favorites" name="收藏数" fill="#fa8c16" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </Spin>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default ProductStatisticsPage;