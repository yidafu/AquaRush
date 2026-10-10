import type { DeliveryTrajectoryVo } from '@aquarush/common';

export interface TrajectoryPoint {
  lat: number;
  lng: number;
  time: string;
  orderId: string;
}

/** 将后端轨迹点转换为地图坐标序列 */
export const toPoints = (list: readonly DeliveryTrajectoryVo[]): TrajectoryPoint[] =>
  list.map((t) => ({
    lat: t.latitude,
    lng: t.longitude,
    time: t.reportedAt,
    orderId: t.orderId,
  }));
