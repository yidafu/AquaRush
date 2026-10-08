import { useLazyQuery } from '@apollo/client';
import { GET_BUSINESS_LOGS_QUERY } from '../graphql/queries/log.graphql';

/**
 * 业务日志查询(F-LOG-003:恒 createdAt 倒序,PageInfo 分页)
 */
export const useBusinessLogs = () => {
  return useLazyQuery(GET_BUSINESS_LOGS_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};
