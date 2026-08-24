import { networkManager } from '@/utils/network-manager';
import type {
  DailyCollectionRecord,
  DailyReconciliation,
  DailyReconciliationPage,
  MyTodayCollectionVo,
  ReconciliationOrderDetailPage,
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

// 获取对账记录列表(服务端分页,规则 #13;按对账日期倒序)
export const getDailyReconciliations = (page: number, size: number) => {
  return networkManager.query<{ dailyReconciliations: DailyReconciliationPage }>(`
    query GetDailyReconciliations($input: DailyReconciliationListInput) {
      dailyReconciliations(input: $input) {
        list {
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
  `, { input: { page, size } });
};

// 对账逐单明细(G29):按对账日期分页取回当日已送达订单流水
export const getDailyReconciliationOrderDetails = (
  reconciliationDate: string,
  page: number,
  size: number,
) => {
  return networkManager.query<{
    dailyReconciliationOrderDetails: ReconciliationOrderDetailPage;
  }>(`
    query GetReconciliationOrderDetails($reconciliationDate: String!, $page: Int!, $size: Int!) {
      dailyReconciliationOrderDetails(reconciliationDate: $reconciliationDate, page: $page, size: $size) {
        list {
          orderId
          orderNo
          deliveryWorkerId
          amountCents
          paymentType
          status
          deliveredAt
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
  `, { reconciliationDate, page, size });
};
