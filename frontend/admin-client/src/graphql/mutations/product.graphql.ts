import { gql } from '@apollo/client';

export const CREATE_PRODUCT_MUTATION = gql`
  mutation CreateProduct($input: CreateProductInput!) {
    createProduct(input: $input) {
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
      updatedAt
    }
  }
`;

export const DELETE_PRODUCT_MUTATION = gql`
  mutation DeleteProduct($id: PrimaryId!) {
    deleteProduct(id: $id)
  }
`;
