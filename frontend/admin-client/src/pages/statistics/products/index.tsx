import React, { useEffect, useState } from 'react';
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
import { useQuery, gql } from '@apollo/client';
import StatisticsCard from '@/components/StatisticsCard';
import dayjs, { Dayjs } from 'dayjs';

const { RangePicker } = DatePicker;

const GET_PRODUCT_STATISTICS = gql`
  query GetProductStatistics {
    productStatistics {
      totalProducts
      onlineProducts
      offlineProducts
      lowStockProducts
      totalSales
      totalFavorites
      productRanking {
        productId
        productName
        salesVolume
        revenue
      }
      favoriteStatistics {
        productId
        productName
        favoriteCount
      }
    }
  }
`;

const GET_PRODUCT_DAILY_SALES = gql`
  query GetProductDailySales($input: DateRangeInput!) {
    productDailySales(input: $input) {
      date
      productId
      productName
      salesVolume
      revenue
    }
  }
`;

const GET_ALL_PRODUCTS = gql`
  query GetAllProducts {
    productsPaginated(page: 0, size: 100) {
      list {
        id
        name
      }
    }
  }
`;

interface ProductSalesStat {
  productId: number;
  productName: string;
  salesVolume: number;
  revenue: number;
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
  revenue: number;
}

interface ProductStatisticsData {
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
}

interface ProductDailySalesQueryData {
  productDailySales: ProductDailySalesData[];
}

interface ProductListData {
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

  const { data: statsData, loading: statsLoading, error: statsError } = useQuery<ProductStatisticsData>(GET_PRODUCT_STATISTICS);

  const { data: dailySalesData, loading: dailySalesLoading, refetch: refetchDailySales } = useQuery<ProductDailySalesQueryData>(GET_PRODUCT_DAILY_SALES, {
    variables: {
      input: {
        startDate,
        endDate,
      },
    },
    skip: !dateRange[0] || !dateRange[1],
  });

  const { data: productsData } = useQuery<ProductListData>(GET_ALL_PRODUCTS);

  useEffect(() => {
    refetchDailySales({
      input: {
        startDate,
        endDate,
      },
    });
  }, [dateRange, refetchDailySales, startDate, endDate]);

  if (statsError) {
    return <div>加载失败: {statsError.message}</div>;
  }

  const stats = statsData?.productStatistics;

  // Prepare chart data for sales ranking
  const salesChartData = stats?.productRanking.map((item) => ({
    name: item.productName.length > 10 ? item.productName.slice(0, 10) + '...' : item.productName,
    sales: item.salesVolume,
    revenue: item.revenue / 100, // Convert cents to yuan
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
      existing.revenue += item.revenue;
    } else {
      dailyTrendMap.set(item.date, {
        date: item.date,
        sales: item.salesVolume,
        revenue: item.revenue,
      });
    }
  });

  const dailyTrendChartData = Array.from(dailyTrendMap.values())
    .sort((a, b) => a.date.localeCompare(b.date))
    .map((item) => ({
      date: item.date.slice(5), // Show MM-DD format
      sales: item.sales,
      revenue: item.revenue / 100, // Convert cents to yuan
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
          revenue: item.revenue / 100,
        }))
    : [];

  const productList = productsData?.productsPaginated?.list || [];

  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: 24, marginBottom: 24 }}>商品统计</h1>

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
                    formatter={(value: number, name: string) => {
                      if (name === 'sales') return [`${value} 桶`, '销量'];
                      return [`¥${value.toFixed(2)}`, '营收'];
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
                    formatter={(value: number) => [`${value} 人`, '收藏数']}
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