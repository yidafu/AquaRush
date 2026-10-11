import { useLazyQuery, useMutation } from '@apollo/client';
import { message } from '@/ui';
import { GET_DATA_MAINTENANCE_OVERVIEW_QUERY, GET_SYSTEM_CONFIGS_QUERY } from '../graphql/queries/system.graphql';
import { BACKUP_LOG_PARTITION_MUTATION, RESTORE_SYSTEM_CONFIG_MUTATION, SAVE_SYSTEM_CONFIGS_MUTATION } from '../graphql/mutations/system.graphql';

// ==================== 系统参数(F-SYS-001) ====================

export const useSystemConfigs = () => {
  return useLazyQuery(GET_SYSTEM_CONFIGS_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useSaveSystemConfigs = () => {
  return useMutation(SAVE_SYSTEM_CONFIGS_MUTATION, {
    onCompleted: () => {
      message.success('参数保存成功');
    },
    onError: (error) => {
      message.error(error.message || '参数保存失败');
    },
    refetchQueries: ['GetSystemConfigs', 'GetDataMaintenanceOverview'],
  });
};

export const useRestoreSystemConfig = () => {
  return useMutation(RESTORE_SYSTEM_CONFIG_MUTATION, {
    onCompleted: () => {
      message.success('已恢复默认值');
    },
    onError: (error) => {
      message.error(error.message || '恢复默认值失败');
    },
    refetchQueries: ['GetSystemConfigs'],
  });
};

// ==================== 数据维护-日志离线备份(F-SYS-006) ====================

// 数据维护页总览:保留策略 + 日志分区 + 离线备份记录(单请求)
export const useDataMaintenanceOverview = () => {
  return useLazyQuery(GET_DATA_MAINTENANCE_OVERVIEW_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

// 离线备份日志分区(转储冷表 + 导出文件;成功后 refetch 总览)
export const useBackupLogPartition = () => {
  return useMutation(BACKUP_LOG_PARTITION_MUTATION, {
    onCompleted: () => {
      message.success('分区已备份:已转储冷表并导出数据文件');
    },
    onError: (error) => {
      message.error(error.message || '分区备份失败');
    },
    refetchQueries: ['GetDataMaintenanceOverview'],
  });
};
