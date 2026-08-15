import React, { useState } from 'react';
import { Card, Select, Table, Tag, Empty, Alert } from 'antd';
import { useQuery } from '@apollo/client';
import { USER_COUPONS_QUERY } from '../../../../graphql/queries/user.graphql';
import type { UserCouponVo, CouponType, UserCouponStatus } from '@aquarush/common';
import { CouponType as CouponTypeEnum, UserCouponStatus as UserCouponStatusEnum } from '@aquarush/common';
import { formatDateTime } from '@aquarush/common';
import { displayCents } from '@aquarush/common';

interface CouponSectionProps {
  userId: string;
}

const COUPON_TYPE_LABELS: Record<CouponType, string> = {
  CASH: '现金券',
  DISCOUNT: '折扣券',
  NO_THRESHOLD_CASH: '无门槛现金券',
};

const STATUS_OPTIONS = [
  { value: 'ALL', label: '全部' },
  { value: UserCouponStatusEnum.RECEIVED, label: '未使用' },
  { value: UserCouponStatusEnum.USED, label: '已使用' },
  { value: UserCouponStatusEnum.RETURNED, label: '已退回' },
  { value: UserCouponStatusEnum.EXPIRED, label: '已过期' },
];

const STATUS_COLORS: Record<string, string> = {
  RECEIVED: 'processing',
  USED: 'success',
  RETURNED: 'warning',
  EXPIRED: 'default',
};

const STATUS_LABELS: Record<string, string> = {
  RECEIVED: '未使用',
  USED: '已使用',
  RETURNED: '已退回',
  EXPIRED: '已过期',
};

/**
 * 用户详情页「优惠券」Tab(F-MKT-003):展示该用户持有的优惠券,支持状态筛选。
 */
const CouponSection: React.FC<CouponSectionProps> = ({ userId }) => {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const status: UserCouponStatus | undefined =
    statusFilter === 'ALL' ? undefined : (statusFilter as UserCouponStatus);

  const { data, loading, error } = useQuery(USER_COUPONS_QUERY, {
    variables: { userId, status },
    skip: !userId,
    errorPolicy: 'all',
  });

  const coupons: UserCouponVo[] = data?.userCoupons || [];

  if (error) {
    return <Alert type="error" showIcon message="优惠券加载失败" description={error.message} />;
  }

  const columns = [
    {
      title: '券名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '类型',
      dataIndex: 'couponType',
      key: 'couponType',
      width: 140,
      render: (type: CouponType) => <Tag color="blue">{COUPON_TYPE_LABELS[type] ?? type}</Tag>,
    },
    {
      title: '面额/折扣',
      key: 'value',
      width: 130,
      render: (_: unknown, record: UserCouponVo) => {
        if (record.couponType === CouponTypeEnum.DISCOUNT) {
          return record.discountRatePermille
            ? `${(record.discountRatePermille / 100).toFixed(1)}折`
            : '-';
        }
        return record.discountAmountCents != null ? displayCents(record.discountAmountCents) : '-';
      },
    },
    {
      title: '使用门槛',
      key: 'threshold',
      width: 120,
      render: (_: unknown, record: UserCouponVo) =>
        record.thresholdCents != null && record.thresholdCents > 0
          ? `满${displayCents(record.thresholdCents)}可用`
          : '无门槛',
    },
    {
      title: '适用范围',
      key: 'scope',
      width: 120,
      render: (_: unknown, record: UserCouponVo) =>
        record.scope === 'PRODUCT' ? '指定商品' : '全场',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: UserCouponStatus) => (
        <Tag color={STATUS_COLORS[s]}>{STATUS_LABELS[s] ?? s}</Tag>
      ),
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      key: 'expireTime',
      width: 160,
      render: (t: string) => formatDateTime(t),
    },
  ];

  return (
    <Card
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span>优惠券</span>
          <Select
            value={statusFilter}
            onChange={setStatusFilter}
            options={STATUS_OPTIONS}
            style={{ width: 130 }}
            size="small"
          />
        </div>
      }
    >
      <Table
        columns={columns}
        dataSource={coupons}
        rowKey="id"
        loading={loading}
        pagination={false}
        scroll={{ x: 1000 }}
        locale={{ emptyText: <Empty description="该用户暂无优惠券" /> }}
      />
    </Card>
  );
};

export default CouponSection;
