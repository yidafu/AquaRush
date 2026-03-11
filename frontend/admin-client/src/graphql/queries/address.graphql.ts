import { gql } from '@apollo/client';

// Address types based on GraphQL schema
export interface Address {
  id: string;
  userId: string | null;
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
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export const GET_ADDRESSES_QUERY = gql`
  query GetAddresses {
    addresses {
      id
      userId
      receiverName
      phone
      province
      provinceCode
      city
      cityCode
      district
      districtCode
      detailAddress
      longitude
      latitude
      isDefault
      createdAt
      updatedAt
    }
  }
`;

export const GET_ADDRESS_BY_ID_QUERY = gql`
  query GetAddressById($id: PrimaryId!) {
    address(id: $id) {
      id
      userId
      receiverName
      phone
      province
      provinceCode
      city
      cityCode
      district
      districtCode
      detailAddress
      longitude
      latitude
      isDefault
      createdAt
      updatedAt
    }
  }
`;
