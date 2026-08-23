import React from 'react';
import { Button, Space, Table, Tag } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import { OrderStatus, displayCents, formatDateTime } from '@aquarush/common';
import type { Order, OrderVoPage } from '@aquarush/common';
import { pagination } from '@/utils/table';

const getStatusColor = (status: OrderStatus) => {
  switch (status) {
    case OrderStatus.PENDING_PAYMENT:
      return 'default';
    case OrderStatus.PENDING_DELIVERY:
    case OrderStatus.DELIVERING:
      return 'processing';
    case OrderStatus.COMPLETED:
      return 'success';
    case OrderStatus.CANCELLED:
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

/** 营销方式详情页订单表的「营销优惠」列取哪个字段 */
export type MarketingDiscountField = 'coupon' | 'promotion' | 'firstOrder' | null;

interface MarketingUsageOrdersTableProps {
  /** 相关订单分页(为 null 时展示空表) */
  orders?: OrderVoPage | null;
  loading?: boolean;
  /** 0 基页码 */
  page: number;
  size: number;
  onTableChange: (pagination: TablePaginationConfig) => void;
  /** 营销优惠列:coupon=券名+券优惠;promotion=满减活动名+优惠;firstOrder=首单活动名+立减;null=不展示(特价/新人价/赠品) */
  discountField?: MarketingDiscountField;
  /** 是否展示赠品列(买赠/满赠订单) */
  showGifts?: boolean;
}

/**
 * 营销方式详情页「相关订单记录」分页表格(F-MKT-003~009):
 * 订单号/下单时间/用户/订单金额/营销优惠金额/状态,行内可跳转订单详情。
 */
const MarketingUsageOrdersTable: React.FC<MarketingUsageOrdersTableProps> = ({
  orders,
  loading,
  page,
  size,
  onTableChange,
  discountField = null,
  showGifts = false,
}) => {
  const navigate = useNavigate();
  const content = orders?.content ?? [];

  const columns: ColumnsType<Order> = [
    { title: '订单号', dataIndex: 'orderNo', width: 190, ellipsis: true },
    {
      title: '下单时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '用户',
      width: 170,
      render: (_, record) => {
        const user = record.user;
        if (!user) return '—';
        return (
          <Space size={4} direction="vertical" style={{ gap: 0 }}>
            <span>{user.nickname || '未命名'}</span>
            <span style={{ color: '#999', fontSize: 12 }}>{user.phone || ''}</span>
          </Space>
        );
      },
    },
    {
      title: '订单金额',
      width: 110,
      render: (_, record) => displayCents(record.amountCents),
    },
    ...(discountField
      ? [
          {
            title: '营销优惠',
            width: 170,
            render: (_: unknown, record: Order) => {
              if (discountField === 'coupon') {
                return record.couponDiscountCents
                  ? `${record.couponName || '优惠券'} -${displayCents(record.couponDiscountCents)}`
                  : '—';
              }
              if (discountField === 'promotion') {
                return record.promotionDiscountCents
                  ? `${record.promotionActivityName || '满减/折扣'} -${displayCents(record.promotionDiscountCents)}`
                  : '—';
              }
              return record.firstOrderDiscountCents
                ? `${record.firstOrderActivityName || '首单立减'} -${displayCents(record.firstOrderDiscountCents)}`
                : '—';
            },
          } as ColumnsType<Order>[number],
        ]
      : []),
    ...(showGifts
      ? [
          {
            title: '赠品',
            width: 160,
            render: (_: unknown, record: Order) => {
              const gifts = record.giftItems ?? [];
              if (gifts.length === 0) return '—';
              return gifts.map((g) => `${g.productName} ×${g.quantity}`).join('、');
            },
          } as ColumnsType<Order>[number],
        ]
      : []),
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status: OrderStatus) => <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>,
    },
    {
      title: '操作',
      width: 90,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => navigate(`/orders/${record.id}`)}>
          查看订单
        </Button>
      ),
    },
  ];

  return (
    <Table<Order>
      rowKey="id"
      columns={columns}
      dataSource={content}
      loading={loading}
      pagination={pagination(orders?.totalElements ?? 0, {
        current: page + 1,
        pageSize: size,
        showSizeChanger: true,
      })}
      onChange={onTableChange}
      scroll={{ x: 1100 }}
    />
  );
};

export default MarketingUsageOrdersTable;
