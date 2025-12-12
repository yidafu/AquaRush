import { gql } from '@apollo/client';

export const UPDATE_USER_MUTATION = gql`
  mutation UpdateUser($input: UpdateUserInput!) {
    updateUser(input: $input) {
      id
      nickname
      phone
      status
      updatedAt
    }
  }
`;

export const CREATE_USER_MUTATION = gql`
  mutation CreateUser($input: CreateUserInput!) {
    createUser(input: $input) {
      id
      wechatOpenId
      nickname
      phone
      status
      createdAt
    }
  }
`;

export const DELETE_USER_MUTATION = gql`
  mutation DeleteUser($id: Long!) {
    deleteUser(id: $id)
  }
`;

export const TOGGLE_USER_STATUS_MUTATION = gql`
  mutation ToggleUserStatus($id: Long!, $status: UserStatus!) {
    updateUserStatus(id: $id, status: $status) {
      id
      status
      updatedAt
    }
  }
`;

export const CREATE_ADMIN_MUTATION = gql`
  mutation CreateAdmin($input: CreateAdminInput!) {
    createAdmin(input: $input) {
      id
      wechatOpenId
      nickname
      phone
      avatarUrl
      createdAt
    }
  }
`;

export const UPDATE_ADMIN_MUTATION = gql`
  mutation UpdateAdmin($input: UpdateAdminInput!) {
    updateAdmin(input: $input) {
      id
      nickname
      phone
      avatarUrl
      updatedAt
    }
  }
`;