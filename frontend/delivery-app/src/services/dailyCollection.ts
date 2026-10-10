import { networkManager } from '@/utils/network-manager';
import { todayDateString } from '@/utils/date';
import type {
  ConfirmDailyCollectionInput,
  DailyCollectionVoPage,
  DailyCollectionRecordVo,
  DailyCollectionSummaryVo,
  DailyReconciliationVo,
  DailyReconciliationVoPage,
  DailyReconciliationStatus,
  MyTodayCollectionVo,
  ReconciliationOrderDetailVoPage,
  RejectDailyCollectionInput,
  ReviewReconciliationInput,
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
  return networkManager.mutate<{ submitDailyCollection: DailyCollectionRecordVo }>(
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
  return networkManager.query<{ dailyReconciliations: DailyReconciliationVoPage }>(`
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
    dailyReconciliationOrderDetails: ReconciliationOrderDetailVoPage;
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

// ==================== 管理员收银对账处置(F-COL-010/011) ====================

/** 返回给服务端分页查询的对账记录状态(用于复核入口判断) */
export type { DailyReconciliationStatus };

/**
 * 收银对账管理页单请求(规则 #10):今日汇总卡 + 每日收款记录(分页) + 对账记录(分页)一次取回。
 */
export const getAdminCollectionPage = (
  collectionsPage: number,
  reconciliationsPage: number,
  size: number,
  collectionDate?: string,
) => {
  return networkManager.query<{
    dailyCollectionSummary: DailyCollectionSummaryVo;
    dailyCollections: DailyCollectionVoPage;
    dailyReconciliations: DailyReconciliationVoPage;
  }>(
    `
    query GetAdminCollectionPage(
      $collectionsInput: DailyCollectionListInput
      $reconciliationsInput: DailyReconciliationListInput
      $summaryDate: String
    ) {
      dailyCollectionSummary(collectionDate: $summaryDate) {
        collectionDate
        totalCashAmountCents
        totalQrCodeAmountCents
        totalWaterTicketCount
        totalOrderCount
        totalOrderAmountCents
        totalSubmittedCount
      }
      dailyCollections(input: $collectionsInput) {
        list {
          id
          deliveryWorkerId
          collectionDate
          cashAmountCents
          waterTicketCount
          qrCodeAmountCents
          orderCount
          orderAmountCents
          differenceAmountCents
          calculatedCashAmountCents
          calculatedQrCodeAmountCents
          calculatedWaterTicketCount
          calculatedOrderAmountCents
          status
          confirmedAt
          confirmedBy
          notes
          rejectedAt
          rejectedBy
          rejectReason
          createdAt
          updatedAt
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
      dailyReconciliations(input: $reconciliationsInput) {
        list {
          id
          reconciliationDate
          totalCashAmountCents
          totalWaterTicketCount
          totalQrCodeAmountCents
          totalOrderCount
          totalOrderAmountCents
          totalCollectionAmountCents
          discrepancyAmountCents
          status
          reviewedAt
          reviewedBy
          reviewNotes
          createdAt
          updatedAt
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
  `,
    {
      collectionsInput: { page: collectionsPage, size, collectionDate: collectionDate || null },
      reconciliationsInput: { page: reconciliationsPage, size },
      summaryDate: collectionDate || todayDateString(),
    },
  );
};

/** 管理员确认每日收款 */
export const confirmDailyCollection = (input: ConfirmDailyCollectionInput) => {
  return networkManager.mutate<{ confirmDailyCollection: DailyCollectionRecordVo }>(
    `
    mutation ConfirmDailyCollection($input: ConfirmDailyCollectionInput!) {
      confirmDailyCollection(input: $input) {
        id
        deliveryWorkerId
        collectionDate
        status
        confirmedAt
        confirmedBy
        notes
      }
    }
  `,
    { input },
  );
};

/** 管理员驳回每日收款(G28:驳回退回草稿,配送员可改可重提) */
export const rejectDailyCollection = (input: RejectDailyCollectionInput) => {
  return networkManager.mutate<{ rejectDailyCollection: DailyCollectionRecordVo }>(
    `
    mutation RejectDailyCollection($input: RejectDailyCollectionInput!) {
      rejectDailyCollection(input: $input) {
        id
        deliveryWorkerId
        collectionDate
        status
        rejectedAt
        rejectedBy
        rejectReason
      }
    }
  `,
    { input },
  );
};

/** 管理员复核对账记录 */
export const reviewReconciliation = (input: ReviewReconciliationInput) => {
  return networkManager.mutate<{ reviewReconciliation: DailyReconciliationVo }>(
    `
    mutation ReviewReconciliation($input: ReviewReconciliationInput!) {
      reviewReconciliation(input: $input) {
        id
        reconciliationDate
        status
        reviewedAt
        reviewedBy
        reviewNotes
      }
    }
  `,
    { input },
  );
};

/** 管理员手动执行对账(默认昨日) */
export const executeDailyReconciliation = (reconciliationDate?: string) => {
  return networkManager.mutate<{ executeDailyReconciliation: DailyReconciliationVo }>(
    `
    mutation ExecuteDailyReconciliation($reconciliationDate: String) {
      executeDailyReconciliation(reconciliationDate: $reconciliationDate) {
        id
        reconciliationDate
        status
        reviewedAt
        reviewNotes
      }
    }
  `,
    { reconciliationDate: reconciliationDate || undefined },
  );
};
