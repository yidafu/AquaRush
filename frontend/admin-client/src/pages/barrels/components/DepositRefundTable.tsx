import React from 'react';
import { Space, Select, Button, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import type { DepositRefundApplication } from '../../../types/barrel';
import { REFUND_APPLICATION_STATUS_MAP } from './constants';
import { pagination } from '@/utils/table';

const { Text } = Typography;

const styles = stylex.create({
  timeline: {
    fontSize: '12px',
  },
  filterRow: {
    marginBottom: '16px',
  },
  statusSelect: {
    width: '140px',
  },
});

export interface DepositRefundTableProps {
  dataSource: DepositRefundApplication[];
  loading: boolean;
  total: number;
  statusFilter?: string;
  page: number;
  onStatusFilterChange: (value: string | undefined) => void;
  onPageChange: (page: number) => void;
  onRefresh: () => void;
  /** 空桶回收确认(仅 PENDING → RECOVERED) */
  onConfirmRecovered: (record: DepositRefundApplication) => void;
  /** 审核通过退押(仅 RECOVERED → REFUNDED) */
  onApprove: (record: DepositRefundApplication) => void;
  /** 驳回(PENDING/RECOVERED → REJECTED,需填原因) */
  onReject: (record: DepositRefundApplication) => void;
}

/** Tab3 退押审核:状态筛选 + 行操作(空桶回收确认 / 审核通过 / 驳回) */
const DepositRefundTable: React.FC<DepositRefundTableProps> = ({
  dataSource,
  loading,
  total,
  statusFilter,
  page,
  onStatusFilterChange,
  onPageChange,
  onRefresh,
  onConfirmRecovered,
  onApprove,
  onReject,
}) => {
  const columns: ColumnsType<DepositRefundApplication> = [
    {
      title: '申请时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (t: string) => (t ? new Date(t).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '规格',
      dataIndex: 'specification',
      key: 'specification',
      width: 120,
    },
    {
      title: '桶数',
      dataIndex: 'bucketCount',
      key: 'bucketCount',
      width: 80,
    },
    {
      title: '退押金额',
      dataIndex: 'depositCents',
      key: 'depositCents',
      width: 110,
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '回收地址',
      key: 'receiver',
      ellipsis: true,
      render: (_, record) => {
        const detail = [record.receiverName, record.receiverPhone, record.receiverDetail]
          .filter(Boolean)
          .join(' ');
        return detail ? <Tooltip title={detail}>{detail}</Tooltip> : '-';
      },
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      ellipsis: true,
      render: (remark?: string | null) => remark || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: DepositRefundApplication['status']) => {
        const info = REFUND_APPLICATION_STATUS_MAP[status] || { text: status, color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '驳回原因',
      dataIndex: 'rejectReason',
      key: 'rejectReason',
      ellipsis: true,
      render: (reason?: string | null) =>
        reason ? (
          <Tooltip title={reason}>
            <Text type="danger">{reason}</Text>
          </Tooltip>
        ) : (
          '-'
        ),
    },
    {
      title: '回收/退还时间',
      key: 'timeline',
      width: 170,
      render: (_, record) => (
        <span {...stylex.props(styles.timeline)}>
          {record.recoveredAt ? `回收: ${new Date(record.recoveredAt).toLocaleString('zh-CN')}` : ''}
          {record.recoveredAt && record.refundedAt ? <br /> : ''}
          {record.refundedAt ? `退还: ${new Date(record.refundedAt).toLocaleString('zh-CN')}` : ''}
          {!record.recoveredAt && !record.refundedAt ? '-' : ''}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 220,
      render: (_, record) => (
        <Space>
          {record.status === 'PENDING' && (
            <Button type="link" onClick={() => onConfirmRecovered(record)}>
              空桶回收确认
            </Button>
          )}
          {record.status === 'RECOVERED' && (
            <Button type="link" onClick={() => onApprove(record)}>
              审核通过
            </Button>
          )}
          {(record.status === 'PENDING' || record.status === 'RECOVERED') && (
            <Button type="link" danger onClick={() => onReject(record)}>
              驳回
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <Space {...stylex.props(styles.filterRow)}>
        <Select
          allowClear
          placeholder="状态筛选"
          {...stylex.props(styles.statusSelect)}
          value={statusFilter}
          onChange={onStatusFilterChange}
          options={Object.entries(REFUND_APPLICATION_STATUS_MAP).map(([value, info]) => ({
            value,
            label: info.text,
          }))}
        />
        <Button onClick={onRefresh}>刷新</Button>
      </Space>
      <Table
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1400 }}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: 10,
          onChange: (p) => onPageChange(p - 1),
        })}
      />
    </>
  );
};

export default DepositRefundTable;
