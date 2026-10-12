/**
 * 押金管理页操作逻辑(barrels/index.tsx 拆分):
 * 押金价目保存 / 空桶回收确认 / 退押审核(通过/驳回)mutation 与提交处理。
 */
import { useCallback } from 'react';
import { Modal, message } from '@/ui';
import { useMutation } from '@apollo/client';
import {
  UPSERT_DEPOSIT_PRICE_MUTATION,
  CONFIRM_BARREL_RECOVERED_MUTATION,
  APPROVE_DEPOSIT_REFUND_MUTATION,
  REJECT_DEPOSIT_REFUND_MUTATION,
} from '../../../graphql/mutations/barrel.graphql';
import {
  DEPOSIT_PRICES_PAGE,
  DEPOSIT_RECORDS_PAGE,
  DEPOSIT_REFUND_APPLICATIONS_PAGE,
  BARREL_LEDGER_PAGE,
} from '../../../graphql/queries/barrel.graphql';
import type {
  DepositPrice,
  DepositRefundApplication,
} from '../../../types/barrel';
import type { DepositPriceFormValues, RejectRefundFormValues } from '../components';
import { yuanToCents } from '@aquarush/common';

export interface UseBarrelActionsOptions {
  editingPrice: DepositPrice | null;
  rejectTarget: DepositRefundApplication | null;
  setPriceModalOpen: (open: boolean) => void;
  setEditingPrice: (price: DepositPrice | null) => void;
  setRejectTarget: (application: DepositRefundApplication | null) => void;
}

export interface UseBarrelActionsResult {
  priceSaving: boolean;
  rejecting: boolean;
  handlePriceSubmit: (values: DepositPriceFormValues) => void;
  handleConfirmRecovered: (record: DepositRefundApplication) => void;
  handleApprove: (record: DepositRefundApplication) => void;
  handleRejectSubmit: (values: RejectRefundFormValues) => Promise<void>;
}

export function useBarrelActions(options: UseBarrelActionsOptions): UseBarrelActionsResult {
  const { editingPrice, rejectTarget, setPriceModalOpen, setEditingPrice, setRejectTarget } = options;

  const [upsertDepositPrice, { loading: priceSaving }] = useMutation(
    UPSERT_DEPOSIT_PRICE_MUTATION,
    {
      refetchQueries: [DEPOSIT_PRICES_PAGE],
      onCompleted: () => {
        message.success('押金价目已保存');
        setPriceModalOpen(false);
        setEditingPrice(null);
      },
      onError: (err) => message.error('保存失败: ' + err.message),
    },
  );

  const [confirmBarrelRecovered] = useMutation(CONFIRM_BARREL_RECOVERED_MUTATION, {
    refetchQueries: [DEPOSIT_REFUND_APPLICATIONS_PAGE],
    onCompleted: () => message.success('空桶回收已确认'),
    onError: (err) => message.error('确认失败: ' + err.message),
  });

  const [approveDepositRefund] = useMutation(APPROVE_DEPOSIT_REFUND_MUTATION, {
    // 审核通过后押金记录与桶账余额同步变化,一并刷新(规则 #10 仍为分页查询)
    refetchQueries: [DEPOSIT_REFUND_APPLICATIONS_PAGE, DEPOSIT_RECORDS_PAGE, BARREL_LEDGER_PAGE],
    onCompleted: () => message.success('退押已审核通过,押金已退还'),
    onError: (err) => message.error('审核失败: ' + err.message),
  });

  const [rejectDepositRefund, { loading: rejecting }] = useMutation(
    REJECT_DEPOSIT_REFUND_MUTATION,
    {
      refetchQueries: [DEPOSIT_REFUND_APPLICATIONS_PAGE],
      onCompleted: () => {
        message.success('已驳回退押申请');
        setRejectTarget(null);
      },
      onError: (err) => message.error('驳回失败: ' + err.message),
    },
  );

  const handlePriceSubmit = useCallback(
    (values: DepositPriceFormValues) => {
      upsertDepositPrice({
        variables: {
          input: {
            ...(editingPrice ? { id: editingPrice.id } : {}),
            specification: values.specification,
            depositCents: yuanToCents(values.deposit),
            status: values.status,
            sortOrder: values.sortOrder ?? 999,
          },
        },
      });
    },
    [editingPrice, upsertDepositPrice],
  );

  const handleConfirmRecovered = useCallback(
    (record: DepositRefundApplication) => {
      Modal.confirm({
        title: '确认空桶已回收',
        content: `确认已回收「${record.specification} × ${record.bucketCount} 桶」空桶?确认后申请进入「已回收」,可继续审核退押。`,
        onOk: () => confirmBarrelRecovered({ variables: { applicationId: record.id } }),
      });
    },
    [confirmBarrelRecovered],
  );

  const handleApprove = useCallback(
    (record: DepositRefundApplication) => {
      Modal.confirm({
        title: '审核通过退押',
        content: `通过后将退还「${record.specification} × ${record.bucketCount} 桶」押金,金额以押金记录幂等,确认通过?`,
        onOk: () => approveDepositRefund({ variables: { applicationId: record.id } }),
      });
    },
    [approveDepositRefund],
  );

  const handleRejectSubmit = useCallback(
    async (values: RejectRefundFormValues): Promise<void> => {
      if (!rejectTarget) return;
      rejectDepositRefund({
        variables: { applicationId: rejectTarget.id, reason: values.reason },
      });
    },
    [rejectTarget, rejectDepositRefund],
  );

  return {
    priceSaving,
    rejecting,
    handlePriceSubmit,
    handleConfirmRecovered,
    handleApprove,
    handleRejectSubmit,
  };
}
