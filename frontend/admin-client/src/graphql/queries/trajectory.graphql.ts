import { gql } from '@apollo/client';

// 送水工配送轨迹查询(F-ADR-012,仅管理员 PERMISSION_DELIVERY_WORKER_READ;
// orderId 指定=查该订单轨迹,否则按时间范围;startTime/endTime 为空=全部)
export const GET_DELIVERY_TRAJECTORIES_QUERY = gql`
  query GetDeliveryTrajectories(
    $workerId: PrimaryId!
    $orderId: PrimaryId
    $startTime: LocalDateTime
    $endTime: LocalDateTime
  ) {
    deliveryTrajectories(
      workerId: $workerId
      orderId: $orderId
      startTime: $startTime
      endTime: $endTime
    ) {
      id
      deliveryWorkerId
      orderId
      longitude
      latitude
      accuracy
      reportedAt
      createdAt
    }
  }
`;

// 送水工最近轨迹点(实时位置,F-ADR-012;无轨迹返回 null)
export const GET_DELIVERY_WORKER_LATEST_LOCATION_QUERY = gql`
  query GetDeliveryWorkerLatestLocation($workerId: PrimaryId!) {
    deliveryWorkerLatestLocation(workerId: $workerId) {
      id
      deliveryWorkerId
      orderId
      longitude
      latitude
      accuracy
      reportedAt
      createdAt
    }
  }
`;
