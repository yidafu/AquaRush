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

// 数据维护页总览(F-SYS-006):保留策略 + 日志分区 + 离线备份记录(单请求)
export const GET_DATA_MAINTENANCE_OVERVIEW_QUERY = gql`
  query GetDataMaintenanceOverview($input: LogBackupRecordListInput) {
    dataMaintenanceOverview(input: $input) {
      settings {
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
      partitions {
        logTable
        partitionName
        partitionMonth
        rowCount
        sizeBytes
        isDefault
        isCurrentMonth
        expired
        backupable
      }
      backupRecords {
        list {
          id
          logTable
          partitionName
          partitionMonth
          coldTableName
          rowCount
          filePath
          fileSizeBytes
          checksum
          status
          errorMessage
          operatorName
          startedAt
          finishedAt
        }
        pageInfo {
          total
          pageSize
          pageNum
          hasNext
          hasPrevious
          totalPages
        }
      }
    }
  }
`;
