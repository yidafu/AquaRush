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
  mutation DeleteUser($id: PrimaryId!) {
    deleteUser(id: $id)
  }
`;

export const TOGGLE_USER_STATUS_MUTATION = gql`
  mutation ToggleUserStatus($id: PrimaryId!, $status: UserStatus!) {
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
      username
      realName
      phone
      role
      createdAt
    }
  }
`;

export const UPDATE_ADMIN_MUTATION = gql`
  mutation UpdateAdmin($id: PrimaryId!, $input: UpdateAdminInput!) {
    updateAdmin(id: $id, input: $input) {
      id
      username
      realName
      phone
      role
      updatedAt
    }
  }
`;

export const DELETE_ADMIN_MUTATION = gql`
  mutation DeleteAdmin($id: PrimaryId!) {
    deleteAdmin(id: $id)
  }
`;