import React from 'react';
import { Table, Tag, Space, Button } from '@/ui';
import type { TableProps } from '@/ui';
import { displayCents } from '@aquarush/common';
import { getOrderStatusConfig } from '../../../constants/order';
import { toneToAntDColor } from '../../../constants/status';
import AssignDeliveryButton from '../../../components/AssignDeliveryButton';
import type { OrderData, DeliveryWorkerOption } from '../types';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  phone: {
    fontSize: '12px',
    // 辅助电话灰字 #999 为本表格局部色值,共享 token 无对应项
    color: '#999',
  },
});

export interface OrderTableProps {
  dataSource: OrderData[];
  loading: boolean;
  pagination: TableProps<OrderData>['pagination'];
  rowSelection: TableProps<OrderData>['rowSelection'];
  workers: DeliveryWorkerOption[];
  workersLoading: boolean;
  /** 点击「查看」跳转订单详情 */
  onView: (record: OrderData) => void;
  /** 派单成功后的刷新回调(单行派单) */
  onAssigned: () => void;
  onChange: TableProps<OrderData>['onChange'];
}

/**
 * 订单列表表格(orders/index.tsx 拆分):
 * 列定义 + 行选择(仅待派单可勾选)+ 单行派单按钮。数据与操作回调均由页面传入。
 */
const OrderTable: React.FC<OrderTableProps> = ({
  dataSource,
  loading,
  pagination,
  rowSelection,
  workers,
  workersLoading,
  onView,
  onAssigned,
  onChange,
}) => {
  const columns: TableProps<OrderData>['columns'] = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      sorter: true,
    },
    {
      title: '用户',
      dataIndex: ['user', 'nickname'],
      key: 'userName',
      render: (_: unknown, record: OrderData) => (
        <Space direction="vertical" size={0}>
          <span>{record.user?.nickname || '-'}</span>
          <span {...stylex.props(styles.phone)}>{record.user?.phone || '-'}</span>
        </Space>
      ),
    },
    {
      title: '收货地址',
      dataIndex: ['address', 'detailAddress'],
      key: 'address',
      render: (_: unknown, record: OrderData) => {
        const addr = record.address;
        if (!addr) return '-';
        return `${addr.province}${addr.city}${addr.district}${addr.detailAddress}`;
      },
      ellipsis: true,
    },
    {
      title: '金额',
      dataIndex: 'amount',
      key: 'amount',
      sorter: true,
      render: (amount: number | null | undefined) => (
        <span>{displayCents(amount)}</span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const { text, tone } = getOrderStatusConfig(status);
        return <Tag color={toneToAntDColor[tone]}>{text}</Tag>;
      },
    },
    {
      title: '配送员',
      dataIndex: ['deliveryWorker', 'name'],
      key: 'deliveryWorker',
      render: (_name: string | null, record: OrderData) =>
        record.deliveryWorker ? (
          <Space direction="vertical" size={0}>
            <span>{record.deliveryWorker.name || '-'}</span>
            <span {...stylex.props(styles.phone)}>{record.deliveryWorker.phone || '-'}</span>
          </Space>
        ) : (
          '-'
        ),
    },
    {
      title: '下单时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      sorter: true,
      render: (date: string) => (date ? new Date(date).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: OrderData) => (
        <Space>
          <Button type="link" size="small" onClick={() => onView(record)}>
            查看
          </Button>
          {/* 派单前置状态=待派单(R-ORD-01 状态机「待派单→配送中」,G09) */}
          {record.status === 'PENDING_DISPATCH' && (
            <AssignDeliveryButton
              orderId={record.id}
              workers={workers}
              workersLoading={workersLoading}
              onAssigned={onAssigned}
            />
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table<OrderData>
      columns={columns}
      dataSource={dataSource}
      loading={loading}
      rowKey="id"
      rowSelection={rowSelection}
      pagination={pagination}
      onChange={onChange}
    />
  );
};

export default OrderTable;
