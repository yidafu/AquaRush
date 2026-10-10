import { gql } from '@apollo/client';
import { useQuery } from '@apollo/client';

// Region types from GraphQL schema
export interface Region {
  readonly code: string;
  readonly name: string;
  readonly level: number;
  readonly parentCode?: string;
}

/**
 * 按父级查询地区(parentCode 为空 = 省级)。
 *
 * AGENTS.md 规则 10:后端不再提供整表返回的 allRegions,前端树形结构改为逐级懒加载,
 * 每次请求返回的都是有界结果(省级 ~34 / 市级 ~20 / 区级 ~20)。
 */
export const GET_REGIONS_QUERY = gql`
  query GetRegions($parentCode: String) {
    regions(parentCode: $parentCode) {
      code
      name
      level
      parentCode
    }
  }
`;

/** 查询某一级地区(parentCode 省略 = 省级列表) */
export const useRegions = (parentCode?: string) => {
  return useQuery<{ regions: Region[] }>(GET_REGIONS_QUERY, {
    variables: { parentCode: parentCode ?? null },
    notifyOnNetworkStatusChange: true,
  });
};
