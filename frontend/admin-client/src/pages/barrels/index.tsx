import React, { useState } from 'react';
import { Card, Tabs } from '@aquarush/admin-ui';
import { useQuery } from '@apollo/client';
import {
  DEPOSIT_PRICES_PAGE_QUERY,
  DEPOSIT_RECORDS_PAGE_QUERY,
  DEPOSIT_REFUND_APPLICATIONS_PAGE_QUERY,
  BARREL_LEDGER_PAGE_QUERY,
} from '../../graphql/queries/barrel.graphql';
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
import { useBarrelActions } from './hooks/useBarrelActions';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

// .page-title(index.css 自研类)内联进组件;原 margin: 0 拆 longhand,
// 再叠加原内联 marginBottom 24px
const styles = stylex.create({
  pageTitle: {
    marginTop: '0px',
    marginRight: '0px',
    marginBottom: '24px',
    marginLeft: '0px',
    fontSize: tokens.fsPage,
    lineHeight: tokens.lhPage,
    fontWeight: 600,
  },
});

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

  // 价目保存 / 空桶回收 / 退押审核 mutation 与提交处理(index.tsx 拆分)
  const {
    priceSaving,
    rejecting,
    handlePriceSubmit,
    handleConfirmRecovered,
    handleApprove,
    handleRejectSubmit,
  } = useBarrelActions({
    editingPrice,
    rejectTarget,
    setPriceModalOpen,
    setEditingPrice,
    setRejectTarget,
  });

  return (
    <div>
      <h1 {...stylex.props(styles.pageTitle)}>
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
