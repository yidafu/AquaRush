import { gql } from '@apollo/client';

export const CREATE_ADMIN_MUTATION = gql`
  mutation CreateAdmin($input: CreateAdminInput!) {
    createAdmin(input: $input) {
      id
      username
      realName
      phone
      role
      accountStatus
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
      accountStatus
      updatedAt
    }
  }
`;

// 删除管理员(G11 落地,F-USR-020)
export const DELETE_ADMIN_MUTATION = gql`
  mutation DeleteAdmin($id: PrimaryId!) {
    deleteAdmin(id: $id)
  }
`;

// 重置管理员密码(G11 落地;newPassword 留空则重置为默认密码)
export const RESET_ADMIN_PASSWORD_MUTATION = gql`
  mutation ResetAdminPassword($id: PrimaryId!, $newPassword: String) {
    resetAdminPassword(id: $id, newPassword: $newPassword)
  }
`;

// 后台创建用户(F-USR-022,G11 落地)
export const CREATE_USER_MUTATION = gql`
  mutation CreateUser($input: CreateUserInput!) {
    createUser(input: $input) {
      id
      wechatOpenId
      nickname
      phone
      avatarUrl
      status
      role
      createdAt
    }
  }
`;

// 后台编辑用户(F-USR-022)
export const UPDATE_USER_MUTATION = gql`
  mutation UpdateUser($id: PrimaryId!, $input: UpdateUserInput!) {
    updateUser(id: $id, input: $input) {
      id
      wechatOpenId
      nickname
      phone
      avatarUrl
      status
      role
      updatedAt
    }
  }
`;

// 后台删除用户(F-USR-022)
export const DELETE_USER_MUTATION = gql`
  mutation DeleteUser($id: PrimaryId!) {
    deleteUser(id: $id)
  }
`;
