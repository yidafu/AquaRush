import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Spin } from 'antd';
import {
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

const ProductStatisticsPage: React.FC = () => {
  const { data, loading, error } = useQuery<ProductStatisticsData>(GET_PRODUCT_STATISTICS);

  if (error) {
    return <div>加载失败: {error.message}</div>;
  }

  const stats = data?.productStatistics;

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

  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: 24, marginBottom: 24 }}>商品统计</h1>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="商品总数"
            value={stats?.totalProducts ?? 0}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="在售商品"
            value={stats?.onlineProducts ?? 0}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="低库存预警"
            value={stats?.lowStockProducts ?? 0}
            loading={loading}
            trend={stats?.lowStockProducts && stats.lowStockProducts > 0 ? 'up' : 'neutral'}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="总销量"
            value={stats?.totalSales ?? 0}
            suffix="桶"
            loading={loading}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="总收藏数"
            value={stats?.totalFavorites ?? 0}
            loading={loading}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="商品销量排行 TOP10">
            <Spin spinning={loading}>
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
            <Spin spinning={loading}>
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