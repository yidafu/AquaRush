import { gql } from '@apollo/client';

export const CREATE_PRODUCT_MUTATION = gql`
  mutation CreateProduct($input: CreateProductInput!) {
    createProduct(input: $input) {
      id
      name
      description
      price
      category
      imageUrl
      inventory
      status
      createdAt
    }
  }
`;

export const UPDATE_PRODUCT_MUTATION = gql`
  mutation UpdateProduct($id: Long!, $input: UpdateProductInput!) {
    updateProduct(id: $id, input: $input) {
      id
      name
      description
      price
      category
      imageUrl
      inventory
      status
      updatedAt
    }
  }
`;

export const DELETE_PRODUCT_MUTATION = gql`
  mutation DeleteProduct($id: Long!) {
    deleteProduct(id: $id)
  }
`;

export const UPDATE_PRODUCT_INVENTORY_MUTATION = gql`
  mutation UpdateProductInventory($id: Long!, $inventory: Int!) {
    updateProductInventory(id: $id, inventory: $inventory) {
      id
      inventory
      updatedAt
    }
  }
`;