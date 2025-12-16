import { gql } from '@apollo/client';

export const CREATE_PRODUCT_MUTATION = gql`
  mutation CreateProduct($input: CreateProductInput!) {
    createProduct(input: $input) {
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
      phValue
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

export const UPDATE_PRODUCT_MUTATION = gql`
  mutation UpdateProduct($id: Long!, $input: UpdateProductInput!) {
    updateProduct(id: $id, input: $input) {
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
      phValue
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
      updatedAt
    }
  }
`;

export const DELETE_PRODUCT_MUTATION = gql`
  mutation DeleteProduct($id: Long!) {
    deleteProduct(id: $id)
  }
`;

export const UPDATE_PRODUCT_STATUS_MUTATION = gql`
  mutation UpdateProductStatus($productId: Long!, $status: ProductStatus!) {
    updateProductStatus(productId: $productId, status: $status) {
      id
      status
      updatedAt
    }
  }
`;

export const BATCH_ADJUST_STOCK_MUTATION = gql`
  mutation BatchAdjustStock($input: BatchStockAdjustmentInput!) {
    batchAdjustStock(input: $input) {
      success
      successCount
      failureCount
      results {
        productId
        success
        message
        previousStock
        newStock
      }
    }
  }
`;

export const INCREASE_STOCK_MUTATION = gql`
  mutation IncreaseStock($productId: Long!, $quantity: Int!) {
    increaseStock(productId: $productId, quantity: $quantity)
  }
`;

export const DECREASE_STOCK_MUTATION = gql`
  mutation DecreaseStock($productId: Long!, $quantity: Int!) {
    decreaseStock(productId: $productId, quantity: $quantity)
  }
`;

export const ONLINE_PRODUCT_MUTATION = gql`
  mutation OnlineProduct($id: Long!) {
    onlineProduct(id: $id) {
      id
      status
      updatedAt
    }
  }
`;

export const OFFLINE_PRODUCT_MUTATION = gql`
  mutation OfflineProduct($id: Long!) {
    offlineProduct(id: $id) {
      id
      status
      updatedAt
    }
  }
`;

export const BATCH_UPDATE_PRODUCTS_MUTATION = gql`
  mutation BatchUpdateProducts($input: [ProductUpdateRequest!]!) {
    batchUpdateProducts(input: $input) {
      id
      name
      price
      stock
      status
      updatedAt
    }
  }
`;