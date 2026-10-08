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

export const SEARCH_ALL_ADDRESSES_QUERY = gql`
  query SearchAllAddresses($keyword: String, $page: Int, $size: Int) {
    searchAllAddresses(keyword: $keyword, page: $page, size: $size) {
      content {
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
      totalElements
      totalPages
      size
      number
    }
  }
`;
