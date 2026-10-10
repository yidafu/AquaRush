import React, { useState, useCallback } from 'react';
import { Card, Tabs, Modal, message } from 'antd';
import { useQuery, useMutation } from '@apollo/client';
import {
  BARREL_FEES_PAGE_QUERY,
  BARREL_PURCHASES_PAGE_QUERY,
  BARREL_FEES_PAGE,
  BARREL_PURCHASES_PAGE,
} from '../../graphql/queries/barrel.graphql';
import {
  CREATE_BARREL_FEE_MUTATION,
  UPDATE_BARREL_FEE_MUTATION,
  DISABLE_BARREL_FEE_MUTATION,
  REFUND_BARREL_PURCHASE_MUTATION,
} from '../../graphql/mutations/barrel.graphql';
import type { BarrelFee, BarrelPurchase } from '../../types/barrel';
import {
  BarrelFeeTable,
  BarrelPurchaseTable,
  BarrelFeeFormModal,
  RefundBarrelModal,
} from './components';
import type { BarrelFeeFormValues, RefundBarrelFormValues } from './components';
import { yuanToCents } from '@aquarush/common';


interface BarrelFeePageData {
  barrelFees: { content: BarrelFee[]; totalElements: number; totalPages: number };
}

interface BarrelPurchasePageData {
  barrelPurchases: { content: BarrelPurchase[]; totalElements: number; totalPages: number };
}

const BarrelManagementPage: React.FC = () => {
  const [feeStatusFilter, setFeeStatusFilter] = useState<string | undefined>(undefined);
  const [feePage, setFeePage] = useState(0);
  const [purchaseStatusFilter, setPurchaseStatusFilter] = useState<string | undefined>(undefined);
  const [purchasePage, setPurchasePage] = useState(0);

  const [feeModalOpen, setFeeModalOpen] = useState(false);
  const [editingFee, setEditingFee] = useState<BarrelFee | null>(null);

  const [refundTarget, setRefundTarget] = useState<BarrelPurchase | null>(null);

  // 单页单请求(规则 #10):价目与购桶记录各一次取回
  const feeQuery = useQuery<BarrelFeePageData>(BARREL_FEES_PAGE_QUERY, {
    variables: { status: feeStatusFilter || null, page: feePage, size: 10 },
    fetchPolicy: 'cache-and-network',
  });
  const purchaseQuery = useQuery<BarrelPurchasePageData>(BARREL_PURCHASES_PAGE_QUERY, {
    variables: { status: purchaseStatusFilter || null, page: purchasePage, size: 10 },
    fetchPolicy: 'cache-and-network',
  });

  const [createBarrelFee] = useMutation(CREATE_BARREL_FEE_MUTATION, {
    refetchQueries: [BARREL_FEES_PAGE],
    onCompleted: () => {
      message.success('价目已创建');
      setFeeModalOpen(false);
    },
    onError: (err) => message.error('创建失败: ' + err.message),
  });
  const [updateBarrelFee] = useMutation(UPDATE_BARREL_FEE_MUTATION, {
    refetchQueries: [BARREL_FEES_PAGE],
    onCompleted: () => {
      message.success('价目已更新');
      setFeeModalOpen(false);
    },
    onError: (err) => message.error('更新失败: ' + err.message),
  });
  const [disableBarrelFee] = useMutation(DISABLE_BARREL_FEE_MUTATION, {
    refetchQueries: [BARREL_FEES_PAGE],
    onCompleted: () => message.success('价目已停用'),
    onError: (err) => message.error('停用失败: ' + err.message),
  });
  const [refundBarrelPurchase, { loading: refunding }] = useMutation(REFUND_BARREL_PURCHASE_MUTATION, {
    refetchQueries: [BARREL_PURCHASES_PAGE],
    onCompleted: () => {
      message.success('水桶费已退还');
      setRefundTarget(null);
    },
    onError: (err) => message.error('退还失败: ' + err.message),
  });

  const handleFeeSubmit = useCallback(
    (values: BarrelFeeFormValues) => {
      const payload = {
        specification: values.specification,
        priceCents: yuanToCents(values.price),
        sortOrder: values.sortOrder ?? 999,
      };
      if (editingFee) {
        updateBarrelFee({ variables: { id: editingFee.id, input: payload } });
      } else {
        createBarrelFee({ variables: { input: payload } });
      }
    },
    [editingFee, createBarrelFee, updateBarrelFee],
  );

  const handleDisableFee = useCallback(
    (fee: BarrelFee) => {
      Modal.confirm({
        title: '停用水桶费价目',
        content: `停用后用户端不可再购买「${fee.specification}」水桶,已生成的购桶单不受影响。确认停用?`,
        onOk: () => disableBarrelFee({ variables: { id: fee.id } }),
      });
    },
    [disableBarrelFee],
  );

  const handleRefundSubmit = useCallback(
    (values: RefundBarrelFormValues) => {
      if (!refundTarget) return;
      refundBarrelPurchase({
        variables: {
          purchaseId: refundTarget.id,
          remark: values.refundRemark || null,
        },
      });
    },
    [refundTarget, refundBarrelPurchase],
  );

  return (
    <div>
      <h1 className="page-title" style={{ marginBottom: 24 }}>
        水桶费管理
      </h1>
      <Card>
        <Tabs
          defaultActiveKey="fees"
          items={[
            {
              key: 'fees',
              label: '价目表',
              children: (
                <BarrelFeeTable
                  dataSource={feeQuery.data?.barrelFees.content ?? []}
                  loading={feeQuery.loading}
                  total={feeQuery.data?.barrelFees.totalElements ?? 0}
                  statusFilter={feeStatusFilter}
                  page={feePage}
                  onFilterChange={(v) => {
                    setFeeStatusFilter(v);
                    setFeePage(0);
                  }}
                  onPageChange={setFeePage}
                  onRefresh={() => feeQuery.refetch()}
                  onCreate={() => {
                    setEditingFee(null);
                    setFeeModalOpen(true);
                  }}
                  onEdit={(fee) => {
                    setEditingFee(fee);
                    setFeeModalOpen(true);
                  }}
                  onDisable={handleDisableFee}
                />
              ),
            },
            {
              key: 'purchases',
              label: '购桶记录',
              children: (
                <BarrelPurchaseTable
                  dataSource={purchaseQuery.data?.barrelPurchases.content ?? []}
                  loading={purchaseQuery.loading}
                  total={purchaseQuery.data?.barrelPurchases.totalElements ?? 0}
                  statusFilter={purchaseStatusFilter}
                  page={purchasePage}
                  onFilterChange={(v) => {
                    setPurchaseStatusFilter(v);
                    setPurchasePage(0);
                  }}
                  onPageChange={setPurchasePage}
                  onRefresh={() => purchaseQuery.refetch()}
                  onRefund={setRefundTarget}
                />
              ),
            },
          ]}
        />
      </Card>

      {/* 价目新增/编辑弹窗(G06) */}
      <BarrelFeeFormModal
        open={feeModalOpen}
        editing={editingFee}
        onCancel={() => {
          setFeeModalOpen(false);
          setEditingFee(null);
        }}
        onSubmit={handleFeeSubmit}
      />

      {/* 退还水桶费弹窗(G06:管理员手动退还) */}
      <RefundBarrelModal
        target={refundTarget}
        confirmLoading={refunding}
        onCancel={() => setRefundTarget(null)}
        onSubmit={handleRefundSubmit}
      />
    </div>
  );
};

export default BarrelManagementPage;
