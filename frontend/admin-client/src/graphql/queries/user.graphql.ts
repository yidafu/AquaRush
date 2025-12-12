import { gql } from '@apollo/client';

export const GET_USERS_QUERY = gql`
  query GetUsers($input: UserListInput) {
    users(input: $input) {
      content {
        id
        wechatOpenId
        nickname
        phone
        avatarUrl
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

export const GET_USER_DETAIL_QUERY = gql`
  query GetUserDetail($id: Long!) {
    user(id: $id) {
      id
      wechatOpenId
      nickname
      phone
      avatarUrl
      status
      addresses {
        id
        receiverName
        phone
        province
        city
        district
        detailAddress
        isDefault
        createdAt
        updatedAt
      }
      createdAt
      updatedAt
    }
  }
`;

export const GET_ADMINS_QUERY = gql`
  query GetAdmins {
    admins {
      id
      wechatOpenId
      nickname
      phone
      avatarUrl
      createdAt
      updatedAt
    }
  }
`;

export const GET_DELIVERY_WORKERS_QUERY = gql`
  query GetDeliveryWorkers {
    deliveryWorkers {
      id
      wechatOpenId
      nickname
      phone
      avatarUrl
      status
      createdAt
      updatedAt
    }
  }
`;