/**
 * 每日收款/对账常量单测(纯业务逻辑,无 UI)
 * 覆盖:容差阈值、状态文案映射、状态 tone 映射。
 */
import {
  DAILY_COLLECTION_STATUS_MAP,
  DAILY_COLLECTION_STATUS_TONE,
  RECONCILIATION_STATUS_MAP,
  RECONCILIATION_STATUS_TONE,
  TOLERANCE_CENTS,
} from '@/constants/collection';

describe('每日收款常量', () => {
  it('容差阈值为 100 分(1 元)', () => {
    expect(TOLERANCE_CENTS).toBe(100);
  });

  it('状态文案映射完整', () => {
    expect(DAILY_COLLECTION_STATUS_MAP).toEqual({
      DRAFT: '草稿',
      SUBMITTED: '已提交',
      CONFIRMED: '已确认',
      DISPUTED: '有争议',
    });
  });

  it('状态 tone 映射完整(从 common re-export,DRAFT 为 neutral)', () => {
    expect(DAILY_COLLECTION_STATUS_TONE).toEqual({
      DRAFT: 'neutral',
      SUBMITTED: 'info',
      CONFIRMED: 'success',
      DISPUTED: 'error',
    });
  });

  it('对账状态文案与 tone 映射完整', () => {
    expect(RECONCILIATION_STATUS_MAP).toEqual({
      PENDING: '待对账',
      MATCHED: '匹配',
      DISCREPANCY: '有差异',
      REVIEWED: '已复核',
    });
    expect(RECONCILIATION_STATUS_TONE).toEqual({
      PENDING: 'warning',
      MATCHED: 'success',
      DISCREPANCY: 'error',
      REVIEWED: 'info',
    });
  });
});
