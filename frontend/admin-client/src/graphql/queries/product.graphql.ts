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
export const GET_STOCK_WARNING_THRESHOLDS_QUERY = gql`
  query GetStockWarningThresholds {
    stockWarningThresholds {
      lowThreshold
      midThreshold
    }
  }
`;

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
