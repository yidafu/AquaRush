import { gql } from '@apollo/client';

export const GET_PRODUCTS_QUERY = gql`
  query GetProducts($input: ProductSearchInput) {
    productsPaginated(input: $input) {
      list {
        id
        name
        subtitle
        priceCents
        originalPriceCents
        depositPriceCents
        coverImageUrl
        imageGallery
        specification
        waterSource
        mineralContent
        stock
        salesVolume
        status
        sortOrder
        tags
        detailContent
        certificateImages
        deliverySettings
        createdAt
        updatedAt
      }
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
  }
`;

// 库存预警阈值(商品列表着色用,读系统参数 F-SYS-003,缺省 10/50)
// 已并入 PRODUCTS_PAGE_DATA_QUERY(商品列表页单页查询,规则 #10)

/**
 * 商品列表页(ProductListPage)单页查询:
 * 一次请求取回「商品分页 + 库存预警阈值(列表着色)」,满足
 * CLAUDE.md 规则 #10 —— 一个页面的一次渲染只发起一个 GraphQL 请求。
 */
export const PRODUCTS_PAGE_DATA_QUERY = gql`
  query ProductsPageData($input: ProductSearchInput) {
    productsPaginated(input: $input) {
      list {
        id
        name
        subtitle
        priceCents
        originalPriceCents
        depositPriceCents
        coverImageUrl
        imageGallery
        specification
        waterSource
        mineralContent
        stock
        salesVolume
        status
        sortOrder
        tags
        detailContent
        certificateImages
        deliverySettings
        createdAt
        updatedAt
      }
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
    stockWarningThresholds {
      lowThreshold
      midThreshold
    }
  }
`;

/** ProductsPageData 操作名(供 refetchQueries 引用) */
export const PRODUCTS_PAGE_DATA = 'ProductsPageData';

export const GET_PRODUCT_DETAIL_QUERY = gql`
  query GetProductDetail($id: PrimaryId!) {
    product(id: $id) {
      id
      name
      subtitle
      price
      originalPrice
      depositPrice
      coverImageUrl
      imageGallery
      specification
      waterSource
      mineralContent
      stock
      salesVolume
      status
      sortOrder
      tags
      detailContent
      certificateImages
      deliverySettings
      createdAt
      updatedAt
    }
  }
`;
