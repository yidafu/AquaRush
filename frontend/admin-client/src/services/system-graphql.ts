import { useLazyQuery, useMutation } from '@apollo/client';
import { message } from 'antd';
import { GET_SYSTEM_CONFIGS_QUERY } from '../graphql/queries/system.graphql';
import { RESTORE_SYSTEM_CONFIG_MUTATION, SAVE_SYSTEM_CONFIGS_MUTATION } from '../graphql/mutations/system.graphql';

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
    refetchQueries: ['GetSystemConfigs'],
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
