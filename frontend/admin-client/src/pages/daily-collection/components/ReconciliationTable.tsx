import React from 'react';
import { Table, Tag, Button } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import type { DailyReconciliation } from '../../../types/daily-collection';
import { reconciliationStatusColorMap, reconciliationStatusTextMap } from './constants';

const styles = stylex.create({
  wrap: {
    display: 'flex',
    flexDirection: 'column',
    rowGap: '16px',
  },
  actionsRow: {
    display: 'flex',
    justifyContent: 'flex-end',
  },
  diffOk: {
    color: tokens.success,
  },
  diffBad: {
    color: tokens.error,
  },
});

export interface ReconciliationTableProps {
  dataSource: DailyReconciliation[];
  loading: boolean;
  executing: boolean;
  onExecute: () => void;
  onView: (record: DailyReconciliation) => void;
  /** 服务端分页配置(规则 #13) */
  pagination?: {
    current: number;
    pageSize: number;
    total: number;
    showSizeChanger?: boolean;
    onChange: (page: number, pageSize: number) => void;
  };
}

/** 对账记录表格 + 手动执行对账按钮(daily-collection/index.tsx 拆分) */
const ReconciliationTable: React.FC<ReconciliationTableProps> = ({
  dataSource,
  loading,
  executing,
  onExecute,
  onView,
  pagination,
}) => {
  const columns: ColumnsType<DailyReconciliation> = [
    {
      title: '对账日期',
      dataIndex: 'reconciliationDate',
      key: 'reconciliationDate',
    },
    {
      title: '订单总数',
      dataIndex: 'totalOrderCount',
      key: 'totalOrderCount',
    },
    {
      title: '订单金额',
      dataIndex: 'totalOrderAmountCents',
      key: 'totalOrderAmountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '总收款金额',
      dataIndex: 'totalCollectionAmountCents',
      key: 'totalCollectionAmountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '现金收款',
      dataIndex: 'totalCashAmountCents',
      key: 'totalCashAmountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '水票数量',
      dataIndex: 'totalWaterTicketCount',
      key: 'totalWaterTicketCount',
    },
    {
      title: '扫码收款',
      dataIndex: 'totalQrCodeAmountCents',
      key: 'totalQrCodeAmountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '差异金额',
      dataIndex: 'discrepancyAmountCents',
      key: 'discrepancyAmountCents',
      render: (cents: number | string) => {
        const value = Number(cents);
        return (
          <span {...stylex.props(Math.abs(value) <= 100 ? styles.diffOk : styles.diffBad)}>
            {displayCents(value)}
          </span>
        );
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: DailyReconciliation['status']) => (
        <Tag color={reconciliationStatusColorMap[status]}>
          {reconciliationStatusTextMap[status] || status}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button type="link" onClick={() => onView(record)}>
          查看详情
        </Button>
      ),
    },
  ];

  return (
    <div {...stylex.props(styles.wrap)}>
      <div {...stylex.props(styles.actionsRow)}>
        <Button type="primary" onClick={onExecute} loading={executing}>
          手动执行对账
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        rowKey="id"
        pagination={
          pagination ?? {
            pageSize: 10,
          }
        }
      />
    </div>
  );
};

export default ReconciliationTable;
