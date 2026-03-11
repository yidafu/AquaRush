import { gql } from '@apollo/client';
import { useQuery, useLazyQuery } from '@apollo/client';

// Region types from GraphQL schema
export interface Region {
  readonly code: string;
  readonly name: string;
  readonly level: number;
  readonly parentCode?: string;
}

// GraphQL Queries
export const GET_REGIONS_QUERY = gql`
  query GetRegions($level: Int, $parentCode: String) {
    regions(level: $level, parentCode: $parentCode) {
      code
      name
      level
    }
  }
`;

// Query to get all regions at once (for building tree structure)
export const GET_ALL_REGIONS_QUERY = gql`
  query GetAllRegions {
    allRegions {
      code
      name
      level
      parentCode
    }
  }
`;

// Hook to get provinces (level 1)
export const useProvinces = () => {
  return useQuery<{ regions: Region[] }>(GET_REGIONS_QUERY, {
    variables: { level: 1 },
    notifyOnNetworkStatusChange: true,
  });
};

// Hook to get cities by province code (level 2)
export const useCities = (provinceCode?: string) => {
  return useQuery<{ regions: Region[] }>(GET_REGIONS_QUERY, {
    variables: { level: 2, parentCode: provinceCode },
    skip: !provinceCode,
    notifyOnNetworkStatusChange: true,
  });
};

// Hook to get districts by city code (level 3)
export const useDistricts = (cityCode?: string) => {
  return useQuery<{ regions: Region[] }>(GET_REGIONS_QUERY, {
    variables: { level: 3, parentCode: cityCode },
    skip: !cityCode,
    notifyOnNetworkStatusChange: true,
  });
};

// Lazy query hook for manual triggering (used by RegionCascader)
export const useLazyRegions = () => {
  return useLazyQuery<{ regions: Region[] }>(GET_REGIONS_QUERY, {
    notifyOnNetworkStatusChange: true,
  });
};

// Hook to get default region hierarchy (not available in admin schema, returns empty)
export const useDefaultRegionHierarchy = () => {
  return {
    data: null,
    loading: false,
    error: null,
  };
};

// Hook to get all regions at once (for building tree structure)
export const useAllRegions = () => {
  return useQuery<{ allRegions: Region[] }>(GET_ALL_REGIONS_QUERY, {
    notifyOnNetworkStatusChange: true,
  });
};
