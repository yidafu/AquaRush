import { gql } from '@apollo/client';
import { useQuery } from '@apollo/client';

// Region types from GraphQL schema
export interface Region {
  readonly code: string;
  readonly name: string;
  readonly level: number;
  readonly parentCode?: string;
}

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

// Hook to get all regions at once (for building tree structure)
export const useAllRegions = () => {
  return useQuery<{ allRegions: Region[] }>(GET_ALL_REGIONS_QUERY, {
    notifyOnNetworkStatusChange: true,
  });
};
