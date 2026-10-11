import React from 'react';
import { Table, Tag, Button, Typography } from '@/ui';
import type { ColumnsType } from '@/ui';
import type { OrderVo } from '@aquarush/common';
import { OrderStatus } from '@aquarush/common';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { pagination } from '@/utils/table';

const styles = stylex.create({
  linkBtn: {
    paddingTop: '0px',
    paddingRight: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
  },
  meta: {
    fontSize: tokens.fsNote,
    // 局部色值:辅助信息灰,token 无对应色
    color: '#999999',
  },
});

const { Text } = Typography;

const getStatusColor = (status: OrderStatus) => {
  switch (status) {
    case OrderStatus.PENDING_PAYMENT:
      return 'default';
    case OrderStatus.PENDING_DELIVERY:
      return 'processing';
    case OrderStatus.DELIVERING:
      return 'processing';
    case OrderStatus.COMPLETED:
      return 'success';
    case OrderStatus.CANCELLED:
      return 'error';
    case OrderStatus.REFUNDED:
      return 'error';
    default:
      return 'default';
  }
};

const getStatusText = (status: OrderStatus) => {
  switch (status) {
    case OrderStatus.PENDING_PAYMENT:
      return '待支付';
    case OrderStatus.PENDING_DELIVERY:
      return '待配送';
    case OrderStatus.DELIVERING:
      return '配送中';
    case OrderStatus.COMPLETED:
      return '已完成';
    case OrderStatus.CANCELLED:
      return '已取消';
    case OrderStatus.REFUNDED:
      return '已退款';
    default:
      return status;
  }
};

export interface OrderHistoryTableProps {
  orders: OrderVo[];
  loading: boolean;
  /** 0 基页码 */
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number, pageSize: number) => void;
  onViewOrder: (orderId: string) => void;
}

/** 用户订单记录表格(users/detail/index.tsx 拆分) */
const OrderHistoryTable: React.FC<OrderHistoryTableProps> = ({
  orders,
  loading,
  page,
  pageSize,
  total,
  onPageChange,
  onViewOrder,
}) => {
  const columns: ColumnsType<OrderVo> = [
    {
      title: '订单编号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 150,
      render: (orderNo: string, record: OrderVo) => (
        <Button
          type="link"
          size="small"
          {...stylex.props(styles.linkBtn)}
          onClick={(e) => {
            e.stopPropagation();
            onViewOrder(record.id);
          }}
        >
          {orderNo}
        </Button>
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
            <div {...stylex.props(styles.meta)}>
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
      width: 120,
      render: (amountCents: number | null | undefined) => (
        <span>{displayCents(amountCents)}</span>
      ),
    },
    {
      title: '收货地址',
      dataIndex: ['address', 'detailAddress'],
      key: 'address',
      width: 250,
      render: (detailAddress: string, record: OrderVo) => (
        <div>
          <div>{detailAddress}</div>
          <div {...stylex.props(styles.meta)}>
            {record.address?.receiverName} - {record.address?.phone}
          </div>
        </div>
      ),
    },
    {
      title: '配送员',
      dataIndex: ['deliveryWorker', 'name'],
      key: 'deliveryWorker',
      width: 100,
      render: (name: string, record: OrderVo) =>
        name ? (
          <div>
            <div>{name}</div>
            <div {...stylex.props(styles.meta)}>
              {record.deliveryWorker?.phone}
            </div>
          </div>
        ) : (
          <Text type="secondary">未分配</Text>
        ),
    },
    {
      title: '支付方式',
      dataIndex: 'paymentMethod',
      key: 'paymentMethod',
      width: 100,
      render: (method: string) => {
        const methodMap: Record<string, string> = {
          WECHAT_PAY: '微信支付',
          ALIPAY: '支付宝',
          CASH: '货到付款',
          CASH_ON_DELIVERY: '货到付款',
        };
        return methodMap[method] || method;
      },
    },
    {
      title: '下单时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (date: string) => new Date(date).toLocaleString(),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: OrderStatus) => (
        <Tag color={getStatusColor(status)}>
          {getStatusText(status)}
        </Tag>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={orders}
      rowKey="id"
      loading={loading}
      onRow={(record) => ({
        onClick: () => onViewOrder(record.id),
      })}
      rowClassName={() => 'clickable-row'}
      pagination={pagination(total, {
        current: page + 1,
        pageSize,
        showSizeChanger: true,
        showQuickJumper: true,
        showTotal: 'range',
        onChange: (nextPage, nextPageSize) => {
          onPageChange(nextPage - 1, nextPageSize);
        },
      })}
      scroll={{ x: 1400 }}
      locale={{
        emptyText: '该用户暂无订单记录',
      }}
    />
  );
};

export default OrderHistoryTable;
