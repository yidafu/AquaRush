import { PRODUCT_STATISTICS_PAGE_DATA_QUERY } from '../../../graphql/queries/statistics.graphql';

export { PRODUCT_STATISTICS_PAGE_DATA_QUERY };

export interface ProductSalesStat {
  productId: number;
  productName: string;
  salesVolume: number;
  revenueCents: number;
}

export interface ProductFavoriteStat {
  productId: number;
  productName: string;
  favoriteCount: number;
}

export interface ProductDailySalesData {
  date: string;
  productId: number;
  productName: string;
  salesVolume: number;
  revenueCents: number;
}

// 库存预警明细项(F-STA-006,G39)
export interface StockWarningItem {
  productId: number;
  productName: string;
  specification: string;
  currentStock: number;
  threshold: number;
  status: 'OUT_OF_STOCK' | 'WARNING';
}

export interface ProductStatisticsPageData {
  productStatistics: {
    totalProducts: number;
    onlineProducts: number;
    offlineProducts: number;
    lowStockProducts: number;
    totalSales: number;
    totalFavorites: number;
    productRanking: ProductSalesStat[];
    favoriteStatistics: ProductFavoriteStat[];
    stockWarningItems: StockWarningItem[];
  };
  productDailySales: ProductDailySalesData[];
  productsPaginated: {
    list: { id: number; name: string }[];
  };
}
