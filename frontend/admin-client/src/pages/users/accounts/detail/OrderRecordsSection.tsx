import React, { useEffect, useState } from 'react';
import { Card, Select, Table, Tag } from '@/ui';
import { useQuery } from '@apollo/client';
import { DELIVERY_WORKER_ORDERS_QUERY } from '../../../../graphql/queries/user.graphql';
import type { OrderVo } from '@aquarush/common';
import { OrderStatus } from '@aquarush/common';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { pagination } from '@/utils/table';

// 局部色值 #999 为辅助信息灰字(手机号/数量/收货人),共享 token 无对应值,保持原样(报告 (a))
const styles = stylex.create({
  sub: {
    fontSize: '12px',
    color: '#999',
  },
  filterWrap: {
    marginBottom: '16px',
  },
  statusSelect: {
    width: '160px',
  },
});

interface OrderRecordsSectionProps {
  /** 配送身份 id(送水员档案并入 admins 单表后=账号 id;F-USR-021/023) */
  deliveryWorkerId: string;
  /** 空态文案 */
  emptyText?: string;
}

/** 订单状态筛选参数(2026-08 起替代订单区原 Tab 切换;2026-09 起置于详情页「配送记录」Tab 内,spec/prd/07-user.md §F-USR-021) */
const STATUS_FILTER_OPTIONS = [
  { value: 'ALL', label: '全部' },
  { value: OrderStatus.DELIVERING, label: '送货中' },
  { value: OrderStatus.PENDING_DELIVERY, label: '待派送' },
  { value: OrderStatus.COMPLETED, label: '已完成' },
];

const STATUS_MAP: Record<string, { color: string; text: string }> = {
  [OrderStatus.DELIVERING]: { color: 'processing', text: '配送中' },
  [OrderStatus.PENDING_DELIVERY]: { color: 'warning', text: '待配送' },
  [OrderStatus.COMPLETED]: { color: 'success', text: '已完成' },
  [OrderStatus.PENDING_DISPATCH]: { color: 'orange', text: '待派单' },
  [OrderStatus.PENDING_PAYMENT]: { color: 'default', text: '待支付' },
  [OrderStatus.CANCELLED]: { color: 'error', text: '已取消' },
  [OrderStatus.REFUNDED]: { color: 'error', text: '已退款' },
};

/**
 * 配送记录区(账号详情页共享组件,2026-09 起作为「配送记录」Tab 内容):
 * 「订单状态」下拉过滤参数 + 单张分页表格(2026-08 设计调整,替代原订单区 Tab 切换)。
 * 订单按 deliveryWorkerId(=账号 id)过滤,status 可空=全部。
 */
const OrderRecordsSection: React.FC<OrderRecordsSectionProps> = ({
  deliveryWorkerId,
  emptyText = '暂无配送记录',
}) => {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const status: OrderStatus | undefined =
    statusFilter === 'ALL' ? undefined : (statusFilter as OrderStatus);

  const {
    data: ordersData,
    loading: ordersLoading,
    refetch: refetchOrders,
  } = useQuery(DELIVERY_WORKER_ORDERS_QUERY, {
    variables: {
      deliveryWorkerId,
      status,
      page,
      size: pageSize,
    },
    skip: !deliveryWorkerId,
    errorPolicy: 'all',
  });

  useEffect(() => {
    if (deliveryWorkerId) {
      refetchOrders({ deliveryWorkerId, status, page, size: pageSize });
    }
  }, [status, page, pageSize, deliveryWorkerId, refetchOrders]);

  const orders: OrderVo[] = ordersData?.orders.content || [];

  const handleStatusChange = (value: string) => {
    setStatusFilter(value);
    setPage(0);
  };

  const orderColumns = [
    {
      title: '订单编号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 150,
    },
    {
      title: '下单用户',
      dataIndex: ['user', 'nickname'],
      key: 'userNickname',
      width: 120,
      render: (nickname: string, record: OrderVo) => (
        <div>
          <div>{nickname || '未知用户'}</div>
          <div {...stylex.props(styles.sub)}>
            {record.user?.phone}
          </div>
        </div>
      ),
    },
    {
      title: '商品信息',
      dataIndex: 'items',
      key: 'items',
      render: (items: OrderVo['items']) => {
        const productNames = (items ?? []).map((item) => item.product?.name).filter(Boolean);
        const quantity = (items ?? []).reduce((sum, item) => sum + item.quantity, 0);
        return (
          <div>
            <div>{productNames.join('、') || '-'}</div>
            <div {...stylex.props(styles.sub)}>
              数量: {quantity}
            </div>
          </div>
        );
      },
    },
    {
      title: '订单金额',
      dataIndex: 'amountCents',
      key: 'amountCents',
      width: 100,
      render: (amountCents: number | null | undefined) => (
        <span>{displayCents(amountCents)}</span>
      ),
    },
    {
      title: '收货地址',
      dataIndex: ['address', 'detailAddress'],
      key: 'address',
      width: 200,
      render: (detailAddress: string, record: OrderVo) => (
        <div>
          <div>{detailAddress}</div>
          <div {...stylex.props(styles.sub)}>
            {record.address?.receiverName} - {record.address?.phone}
          </div>
        </div>
      ),
    },
    {
      title: '下单时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120,
      render: (date: string) => new Date(date).toLocaleString(),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (orderStatus: OrderStatus) => {
        const config = STATUS_MAP[orderStatus];
        return config ? (
          <Tag color={config.color}>{config.text}</Tag>
        ) : (
          <Tag>{orderStatus}</Tag>
        );
      },
    },
  ];

  return (
    <Card title="配送记录">
      <div {...stylex.props(styles.filterWrap)}>
        <Select
          value={statusFilter}
          onChange={(value) => handleStatusChange(value as string)}
          options={STATUS_FILTER_OPTIONS}
          {...stylex.props(styles.statusSelect)}
        />
      </div>
      <Table
        columns={orderColumns}
        dataSource={orders}
        rowKey="id"
        loading={ordersLoading}
        pagination={pagination(ordersData?.orders.totalElements ?? 0, {
          current: page + 1,
          pageSize,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: 'range',
          onChange: (nextPage: number, nextPageSize: number) => {
            setPage(nextPage - 1);
            setPageSize(nextPageSize);
          },
        })}
        scroll={{ x: 1200 }}
        locale={{ emptyText }}
      />
    </Card>
  );
};

export default OrderRecordsSection;
