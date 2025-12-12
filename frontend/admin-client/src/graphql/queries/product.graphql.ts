import { gql } from '@apollo/client';

export const GET_PRODUCTS_QUERY = gql`
  query GetProducts($input: ProductListInput) {
    products(input: $input) {
      content {
        id
        name
        description
        price
        category
        imageUrl
        inventory
        status
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_PRODUCT_DETAIL_QUERY = gql`
  query GetProductDetail($id: Long!) {
    product(id: $id) {
      id
      name
      description
      price
      category
      imageUrl
      inventory
      status
      createdAt
      updatedAt
    }
  }
`;