import { networkManager } from '@/utils/network-manager';
import type {
  DailyCollectionRecord,
  DailyReconciliation,
  MyTodayCollectionVo,
  SubmitDailyCollectionInput,
} from '@aquarush/common';

// ==================== 每日收款与对账相关 API ====================

// 获取今日收款预计算结果（配送员使用）
export const getTodayCollection = () => {
  return networkManager.query<{ myTodayCollection: MyTodayCollectionVo | null }>(`
    query GetMyTodayCollection {
      myTodayCollection {
        orderCount
        orderAmountCents
        cashAmountCents
        waterTicketCount
        qrCodeAmountCents
        differenceAmountCents
        calculatedCashAmountCents
        calculatedQrCodeAmountCents
        calculatedWaterTicketCount
        calculatedOrderAmountCents
        status
      }
    }
  `);
};

// 配送员提交每日收款
export const submitDailyCollection = (input: SubmitDailyCollectionInput) => {
  return networkManager.mutate<{ submitDailyCollection: DailyCollectionRecord }>(
    `
    mutation SubmitDailyCollection($input: SubmitDailyCollectionInput!) {
      submitDailyCollection(input: $input) {
        id
        deliveryWorkerId
        collectionDate
        cashAmountCents
        waterTicketCount
        qrCodeAmountCents
        orderCount
        orderAmountCents
        differenceAmountCents
        status
        createdAt
        updatedAt
      }
    }
  `,
    { input },
  );
};

// 获取对账记录列表
export const getDailyReconciliations = () => {
  return networkManager.query<{ dailyReconciliations: readonly DailyReconciliation[] }>(`
    query GetDailyReconciliations {
      dailyReconciliations {
        id
        reconciliationDate
        totalOrderCount
        totalOrderAmountCents
        totalCollectionAmountCents
        totalCashAmountCents
        totalWaterTicketCount
        totalQrCodeAmountCents
        discrepancyAmountCents
        status
        reviewedAt
        reviewedBy
        reviewNotes
      }
    }
  `);
};
