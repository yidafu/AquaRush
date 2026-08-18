import { gql } from '@apollo/client';

export const CREATE_PRODUCT_MUTATION = gql`
  mutation CreateProduct($input: CreateProductInput!) {
    createProduct(input: $input) {
      id
      name
      subtitle
      priceCents
      originalPriceCents
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
      createdAt
      updatedAt
    }
  }
`;

export const UPDATE_PRODUCT_MUTATION = gql`
  mutation UpdateProduct($id: PrimaryId!, $input: UpdateProductInput!) {
    updateProduct(id: $id, input: $input) {
      id
      name
      subtitle
      priceCents
      originalPriceCents
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
      updatedAt
    }
  }
`;

export const DELETE_PRODUCT_MUTATION = gql`
  mutation DeleteProduct($id: PrimaryId!) {
    deleteProduct(id: $id)
  }
`;

// 商品属性选项字典(F-PRO-009):规格/水源地下拉候选的配置
export const CREATE_PRODUCT_OPTION_MUTATION = gql`
  mutation CreateProductOption($input: CreateProductOptionInput!) {
    createProductOption(input: $input) {
      id
      optionType
      value
      sortOrder
      enabled
      createdAt
      updatedAt
    }
  }
`;

export const UPDATE_PRODUCT_OPTION_MUTATION = gql`
  mutation UpdateProductOption($id: PrimaryId!, $input: UpdateProductOptionInput!) {
    updateProductOption(id: $id, input: $input) {
      id
      optionType
      value
      sortOrder
      enabled
      createdAt
      updatedAt
    }
  }
`;

export const DELETE_PRODUCT_OPTION_MUTATION = gql`
  mutation DeleteProductOption($id: PrimaryId!) {
    deleteProductOption(id: $id)
  }
`;
