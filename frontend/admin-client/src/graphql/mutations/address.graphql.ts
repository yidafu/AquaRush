import { gql } from '@apollo/client';

// Address input type based on GraphQL schema
export interface AddressInput {
  receiverName: string;
  phone: string;
  province: string;
  provinceCode?: string;
  city: string;
  cityCode?: string;
  district: string;
  districtCode?: string;
  detailAddress: string;
  longitude?: number;
  latitude?: number;
  isDefault?: boolean;
}

export interface BatchImportResult {
  successCount: number;
  failureCount: number;
  totalCount: number;
}

export const CREATE_ADDRESS_MUTATION = gql`
  mutation CreateAdminAddress($input: AddressInput!) {
    createAdminAddress(input: $input) {
      id
      receiverName
      phone
      province
      city
      district
      detailAddress
      isDefault
      createdAt
    }
  }
`;

export const BATCH_IMPORT_ADDRESSES_MUTATION = gql`
  mutation BatchImportAddresses($input: [AddressInput!]!) {
    batchImportAddresses(input: $input) {
      successCount
      failureCount
      totalCount
    }
  }
`;

export const DELETE_ADDRESS_MUTATION = gql`
  mutation DeleteAdminAddress($id: PrimaryId!) {
    deleteAdminAddress(id: $id)
  }
`;
