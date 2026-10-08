import { gql } from '@apollo/client';

// 查询在线配送员
export const GET_ONLINE_DELIVERY_WORKERS_QUERY = gql`
  query GetOnlineDeliveryWorkers {
    onlineDeliveryWorkers {
      id
      name
      phone
      onlineStatus
      currentTaskCount
    }
  }
`;
