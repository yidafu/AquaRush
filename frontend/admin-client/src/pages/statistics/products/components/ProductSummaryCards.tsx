import React from 'react';
import { Row, Col } from '@/ui';
import StatisticsCard from '@/components/StatisticsCard';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  secondRow: {
    marginTop: '16px',
  },
});

export interface ProductSummaryCardsProps {
  loading: boolean;
  totalProducts: number;
  onlineProducts: number;
  lowStockProducts: number;
  totalSales: number;
  totalFavorites: number;
}

/** 商品统计汇总卡(statistics/products/index.tsx 拆分) */
const ProductSummaryCards: React.FC<ProductSummaryCardsProps> = ({
  loading,
  totalProducts,
  onlineProducts,
  lowStockProducts,
  totalSales,
  totalFavorites,
}) => (
  <>
    <Row gutter={[16, 16]}>
      <Col xs={24} sm={12} lg={6}>
        <StatisticsCard title="商品总数" value={totalProducts} loading={loading} />
      </Col>
      <Col xs={24} sm={12} lg={6}>
        <StatisticsCard title="在售商品" value={onlineProducts} loading={loading} />
      </Col>
      <Col xs={24} sm={12} lg={6}>
        <StatisticsCard
          title="低库存预警"
          value={lowStockProducts}
          loading={loading}
          trend={lowStockProducts > 0 ? 'up' : 'neutral'}
        />
      </Col>
      <Col xs={24} sm={12} lg={6}>
        <StatisticsCard title="总销量" value={totalSales} suffix="桶" loading={loading} />
      </Col>
    </Row>
    <Row gutter={[16, 16]} {...stylex.props(styles.secondRow)}>
      <Col xs={24} sm={12} lg={6}>
        <StatisticsCard title="总收藏数" value={totalFavorites} loading={loading} />
      </Col>
    </Row>
  </>
);

export default ProductSummaryCards;
