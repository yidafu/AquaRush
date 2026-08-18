import { gql } from '@apollo/client';

/**
 * 商品统计页(ProductStatisticsPage)单页查询:
 * 一次请求取回「商品总览统计 + 日销量明细(时间范围)+ 全量商品目录(筛选下拉)」,
 * 满足 CLAUDE.md 规则 #10 —— 一个页面的一次渲染只发起一个 GraphQL 请求。
 */
export const PRODUCT_STATISTICS_PAGE_DATA_QUERY = gql`
  query ProductStatisticsPageData($input: DateRangeInput!) {
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
        revenueCents
      }
      favoriteStatistics {
        productId
        productName
        favoriteCount
      }
      stockWarningItems {
        productId
        productName
        specification
        currentStock
        threshold
        status
      }
    }
    productDailySales(input: $input) {
      date
      productId
      productName
      salesVolume
      revenueCents
    }
    productsPaginated(input: { page: 0, size: 100 }) {
      list {
        id
        name
      }
    }
  }
`;

/** ProductStatisticsPageData 操作名(供 refetchQueries 引用) */
export const PRODUCT_STATISTICS_PAGE_DATA = 'ProductStatisticsPageData';
