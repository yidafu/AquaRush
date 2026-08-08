import { gql } from '@apollo/client';

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
