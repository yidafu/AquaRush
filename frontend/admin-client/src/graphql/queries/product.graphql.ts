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
