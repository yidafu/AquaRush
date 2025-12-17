import { gql } from '@apollo/client';

export const GET_PRODUCTS_QUERY = gql`
  query GetProducts($page: Int = 0, $size: Int = 20, $status: ProductStatus, $keyword: String) {
    products(page: $page, size: $size, status: $status, keyword: $keyword) {
      list {
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
        isDeleted
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
      isDeleted
      createdAt
      updatedAt
    }
  }
`;

export const GET_PRODUCTS_PAGINATED_QUERY = gql`
  query GetProductsPaginated($input: ProductListInput) {
    productsPaginated(input: $input) {
      list {
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
        isDeleted
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

export const GET_TOP_SALES_PRODUCTS = gql`
  query GetTopSalesProducts($limit: Int = 10) {
    topSalesProducts(limit: $limit) {
      id
      name
      subtitle
      price
      originalPrice
      depositPrice
      coverImageUrl
      specification
      waterSource
      stock
      salesVolume
      status
      sortOrder
      createdAt
    }
  }
`;

export const GET_PRODUCTS_BY_WATER_SOURCE = gql`
  query GetProductsByWaterSource($waterSource: String!) {
    productsByWaterSource(waterSource: $waterSource) {
      id
      name
      subtitle
      price
      coverImageUrl
      specification
      waterSource
      stock
      salesVolume
      status
      createdAt
    }
  }
`;


export const GET_PRODUCTS_BY_TAG = gql`
  query GetProductsByTag($tag: String!) {
    productsByTag(tag: $tag) {
      id
      name
      price
      coverImageUrl
      tags
      stock
      salesVolume
      status
    }
  }
`;

export const GET_ALL_ACTIVE_PRODUCTS = gql`
  query GetAllActiveProducts {
    allActiveProducts {
      id
      name
      price
      coverImageUrl
      specification
      stock
      status
      sortOrder
    }
  }
`;

export const GET_WATER_SOURCE_STATISTICS = gql`
  query GetWaterSourceStatistics {
    waterSourceStatistics
  }
`;

export const GET_SPECIFICATION_STATISTICS = gql`
  query GetSpecificationStatistics {
    specificationStatistics
  }
`;

export const GET_LOW_STOCK_PRODUCTS = gql`
  query GetLowStockProducts($threshold: Int = 10) {
    lowStockProducts(threshold: $threshold) {
      productId
      productName
      currentStock
      threshold
      status
    }
  }
`;
