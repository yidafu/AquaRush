import { gql } from '@apollo/client';

// 系统参数列表(按参数组返回,未配置组返回空列表)
export const GET_SYSTEM_CONFIGS_QUERY = gql`
  query GetSystemConfigs($group: SystemConfigGroup!) {
    systemConfigs(group: $group) {
      id
      settingKey
      settingType
      settingGroup
      settingValue
      defaultValue
      description
      minValue
      maxValue
      options
      updatedBy
      updatedAt
    }
  }
`;
