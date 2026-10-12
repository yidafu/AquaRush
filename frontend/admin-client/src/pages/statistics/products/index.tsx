import React, { useState } from 'react';
import { Row } from '@aquarush/admin-ui';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@apollo/client';
import { centsToYuan } from '@aquarush/common';
import RefreshControls from '@/components/RefreshControls';
import { useAutoRefresh } from '@/hooks/useAutoRefresh';
import dayjs, { Dayjs } from 'dayjs';
import {
  PRODUCT_STATISTICS_PAGE_DATA_QUERY,
  type ProductStatisticsPageData,
} from './data';
import {
  ProductSummaryCards,
  SalesTrendChart,
  SalesRankingChart,
  FavoriteRankingChart,
  StockWarningTable,
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
    marginBottom: '24px',
  },
  rankingRow: {
    marginTop: '24px',
  },
});

const ProductStatisticsPage: React.FC = () => {
  const navigate = useNavigate();
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

  if (error) {
    return <div>加载失败: {error.message}</div>;
  }

  const stats = data?.productStatistics;

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
  const dailySales = data?.productDailySales || [];

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

  const productList = data?.productsPaginated?.list || [];

  return (
    <div>
      <div {...stylex.props(styles.header)}>
        <h1 {...stylex.props(styles.pageTitle)}>商品统计</h1>
        <RefreshControls
          interval={intervalMs}
          onIntervalChange={setIntervalMs}
          onRefresh={refresh}
          refreshing={refreshing}
          lastUpdated={lastUpdated}
        />
      </div>

      <ProductSummaryCards
        loading={loading}
        totalProducts={stats?.totalProducts ?? 0}
        onlineProducts={stats?.onlineProducts ?? 0}
        lowStockProducts={stats?.lowStockProducts ?? 0}
        totalSales={stats?.totalSales ?? 0}
        totalFavorites={stats?.totalFavorites ?? 0}
      />

      <SalesTrendChart
        data={selectedProductId ? selectedProductTrendData : dailyTrendChartData}
        loading={loading}
        productList={productList}
        selectedProductId={selectedProductId}
        dateRange={dateRange}
        onProductChange={setSelectedProductId}
        onDateRangeChange={setDateRange}
      />

      <Row gutter={[16, 16]} {...stylex.props(styles.rankingRow)}>
        <SalesRankingChart data={salesChartData} loading={loading} />
        <FavoriteRankingChart data={favoriteChartData} loading={loading} />
      </Row>

      {/* 库存预警明细(F-STA-006,G39) */}
      <StockWarningTable
        items={stats?.stockWarningItems || []}
        loading={loading}
        threshold={stats?.stockWarningItems?.[0]?.threshold ?? 10}
        onConfigThreshold={() => navigate('/system-config/stock')}
      />
    </div>
  );
};

export default ProductStatisticsPage;
