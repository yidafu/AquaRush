import { networkManager } from '@/utils/network-manager';
import type { Scalars } from '@aquarush/common';

/**
 * 配送轨迹上报服务(F-ADR-012,R-ADR-03)
 *
 * 送水工在订单「配送中」期间按设定频率(默认 5s)调 reportTrajectory 上报位置点;
 * 服务端校验仅本人 DELIVERING 订单可上报(非配送中上报丢弃)。
 */
export const reportTrajectory = (
  orderId: Scalars['PrimaryId']['input'],
  longitude: number,
  latitude: number,
  accuracy?: number | null,
  reportedAt?: string,
): Promise<{ reportTrajectory: boolean }> => {
  return networkManager.mutate<{ reportTrajectory: boolean }>(
    `
    mutation ReportTrajectory($input: ReportTrajectoryInput!) {
      reportTrajectory(input: $input)
    }
  `,
    {
      input: {
        orderId,
        longitude,
        latitude,
        accuracy: accuracy ?? null,
        reportedAt: reportedAt ?? null,
      },
    },
  );
};
