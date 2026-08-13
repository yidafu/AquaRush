import { gql } from '@apollo/client';

// 批量保存系统参数(按声明类型/范围校验,即时生效;每次修改留审计日志)
export const SAVE_SYSTEM_CONFIGS_MUTATION = gql`
  mutation SaveSystemConfigs($input: [SaveSystemConfigInput!]!) {
    saveSystemConfigs(input: $input) {
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

// 恢复参数默认值(留审计日志)
export const RESTORE_SYSTEM_CONFIG_MUTATION = gql`
  mutation RestoreSystemConfig($settingKey: String!) {
    restoreSystemConfig(settingKey: $settingKey) {
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
