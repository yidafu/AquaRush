import React, { useState, useCallback } from 'react';
import { Card, Tabs, Modal, message } from 'antd';
import { useQuery, useMutation } from '@apollo/client';
import {
  DEPOSIT_PRICES_PAGE_QUERY,
  DEPOSIT_RECORDS_PAGE_QUERY,
  DEPOSIT_REFUND_APPLICATIONS_PAGE_QUERY,
  BARREL_LEDGER_PAGE_QUERY,
  DEPOSIT_PRICES_PAGE,
  DEPOSIT_RECORDS_PAGE,
  DEPOSIT_REFUND_APPLICATIONS_PAGE,
  BARREL_LEDGER_PAGE,
} from '../../graphql/queries/barrel.graphql';
import {
  UPSERT_DEPOSIT_PRICE_MUTATION,
  CONFIRM_BARREL_RECOVERED_MUTATION,
  APPROVE_DEPOSIT_REFUND_MUTATION,
  REJECT_DEPOSIT_REFUND_MUTATION,
} from '../../graphql/mutations/barrel.graphql';
import type {
  BarrelLedgerRow,
  DepositPrice,
  DepositRecord,
  DepositRefundApplication,
} from '../../types/barrel';
import {
  DepositPriceTable,
  DepositRecordTable,
  DepositRefundTable,
  BarrelLedgerTable,
  DepositPriceFormModal,
  RejectRefundModal,
} from './components';
import type { DepositPriceFormValues, RejectRefundFormValues } from './components';
import { yuanToCents } from '@aquarush/common';

interface PageData<T> {
  page: { content: T[]; totalElements: number; totalPages: number };
}

/**
 * 押金管理(G06 回退改造后:押金价目 / 押金记录 / 退押审核 / 桶账台账)
 * 对应 R-MON-03 押金桶模型与 F-PRO-012 桶账台账;路由 /barrels 保留。
 */
const BarrelManagementPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('prices');

  // Tab1 押金价目
  const [priceStatusFilter, setPriceStatusFilter] = useState<string | undefined>(undefined);
  const [priceKeyword, setPriceKeyword] = useState('');
  const [pricePage, setPricePage] = useState(0);
  const [priceModalOpen, setPriceModalOpen] = useState(false);
  const [editingPrice, setEditingPrice] = useState<DepositPrice | null>(null);

  // Tab2 押金记录
  const [recordStatusFilter, setRecordStatusFilter] = useState<string | undefined>(undefined);
  const [recordSpecFilter, setRecordSpecFilter] = useState('');
  const [recordPage, setRecordPage] = useState(0);

  // Tab3 退押审核
  const [refundStatusFilter, setRefundStatusFilter] = useState<string | undefined>(undefined);
  const [refundPage, setRefundPage] = useState(0);
  const [rejectTarget, setRejectTarget] = useState<DepositRefundApplication | null>(null);

  // Tab4 桶账台账
  const [ledgerKeyword, setLedgerKeyword] = useState('');
  const [ledgerOnlyOwed, setLedgerOnlyOwed] = useState(false);
  const [ledgerPage, setLedgerPage] = useState(0);

  // 单页单请求(规则 #10):每个 Tab 按需取回一次分页数据
  const priceQuery = useQuery<PageData<DepositPrice>>(DEPOSIT_PRICES_PAGE_QUERY, {
    variables: {
      status: priceStatusFilter || null,
      keyword: priceKeyword || null,
      page: pricePage,
      size: 10,
    },
    fetchPolicy: 'cache-and-network',
  });
  const recordQuery = useQuery<PageData<DepositRecord>>(DEPOSIT_RECORDS_PAGE_QUERY, {
    variables: {
      status: recordStatusFilter || null,
      specification: recordSpecFilter || null,
      page: recordPage,
      size: 10,
    },
    fetchPolicy: 'cache-and-network',
    skip: activeTab !== 'records',
  });
  const refundQuery = useQuery<PageData<DepositRefundApplication>>(
    DEPOSIT_REFUND_APPLICATIONS_PAGE_QUERY,
    {
      variables: { status: refundStatusFilter || null, page: refundPage, size: 10 },
      fetchPolicy: 'cache-and-network',
      skip: activeTab !== 'refunds',
    },
  );
  const ledgerQuery = useQuery<PageData<BarrelLedgerRow>>(BARREL_LEDGER_PAGE_QUERY, {
    variables: {
      keyword: ledgerKeyword || null,
      onlyOwed: ledgerOnlyOwed,
      page: ledgerPage,
      size: 10,
    },
    fetchPolicy: 'cache-and-network',
    skip: activeTab !== 'ledger',
  });

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
    async (values: RejectRefundFormValues) => {
      if (!rejectTarget) return;
      rejectDepositRefund({
        variables: { applicationId: rejectTarget.id, reason: values.reason },
      });
    },
    [rejectTarget, rejectDepositRefund],
  );

  return (
    <div>
      <h1 className="page-title" style={{ marginBottom: 24 }}>
        押金管理
      </h1>
      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'prices',
              label: '押金价目',
              children: (
                <DepositPriceTable
                  dataSource={priceQuery.data?.page.content ?? []}
                  loading={priceQuery.loading}
                  total={priceQuery.data?.page.totalElements ?? 0}
                  statusFilter={priceStatusFilter}
                  page={pricePage}
                  onStatusFilterChange={(v) => {
                    setPriceStatusFilter(v);
                    setPricePage(0);
                  }}
                  onKeywordChange={(v) => {
                    setPriceKeyword(v);
                    setPricePage(0);
                  }}
                  onPageChange={setPricePage}
                  onRefresh={() => priceQuery.refetch()}
                  onCreate={() => {
                    setEditingPrice(null);
                    setPriceModalOpen(true);
                  }}
                  onEdit={(price) => {
                    setEditingPrice(price);
                    setPriceModalOpen(true);
                  }}
                />
              ),
            },
            {
              key: 'records',
              label: '押金记录',
              children: (
                <DepositRecordTable
                  dataSource={recordQuery.data?.page.content ?? []}
                  loading={recordQuery.loading}
                  total={recordQuery.data?.page.totalElements ?? 0}
                  statusFilter={recordStatusFilter}
                  specification={recordSpecFilter}
                  page={recordPage}
                  onStatusFilterChange={(v) => {
                    setRecordStatusFilter(v);
                    setRecordPage(0);
                  }}
                  onSpecificationChange={(v) => {
                    setRecordSpecFilter(v);
                    setRecordPage(0);
                  }}
                  onPageChange={setRecordPage}
                  onRefresh={() => recordQuery.refetch()}
                />
              ),
            },
            {
              key: 'refunds',
              label: '退押审核',
              children: (
                <DepositRefundTable
                  dataSource={refundQuery.data?.page.content ?? []}
                  loading={refundQuery.loading}
                  total={refundQuery.data?.page.totalElements ?? 0}
                  statusFilter={refundStatusFilter}
                  page={refundPage}
                  onStatusFilterChange={(v) => {
                    setRefundStatusFilter(v);
                    setRefundPage(0);
                  }}
                  onPageChange={setRefundPage}
                  onRefresh={() => refundQuery.refetch()}
                  onConfirmRecovered={handleConfirmRecovered}
                  onApprove={handleApprove}
                  onReject={setRejectTarget}
                />
              ),
            },
            {
              key: 'ledger',
              label: '桶账台账',
              children: (
                <BarrelLedgerTable
                  dataSource={ledgerQuery.data?.page.content ?? []}
                  loading={ledgerQuery.loading}
                  total={ledgerQuery.data?.page.totalElements ?? 0}
                  onlyOwed={ledgerOnlyOwed}
                  page={ledgerPage}
                  onKeywordChange={(v) => {
                    setLedgerKeyword(v);
                    setLedgerPage(0);
                  }}
                  onOnlyOwedChange={(v) => {
                    setLedgerOnlyOwed(v);
                    setLedgerPage(0);
                  }}
                  onPageChange={setLedgerPage}
                  onRefresh={() => ledgerQuery.refetch()}
                />
              ),
            },
          ]}
        />
      </Card>

      {/* 押金价目新增/编辑弹窗(G06,金额输入元 → 提交转分) */}
      <DepositPriceFormModal
        open={priceModalOpen}
        editing={editingPrice}
        submitting={priceSaving}
        onCancel={() => {
          setPriceModalOpen(false);
          setEditingPrice(null);
        }}
        onSubmit={handlePriceSubmit}
      />

      {/* 驳回退押申请弹窗(必填原因) */}
      <RejectRefundModal
        target={rejectTarget}
        confirmLoading={rejecting}
        onCancel={() => setRejectTarget(null)}
        onSubmit={handleRejectSubmit}
      />
    </div>
  );
};

export default BarrelManagementPage;
