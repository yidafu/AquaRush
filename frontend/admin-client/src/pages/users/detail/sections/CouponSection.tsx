import React, { useState } from 'react';
import { Card, Select, Table, Tag, Empty, Alert } from '@/ui';
import { useQuery } from '@apollo/client';
import { USER_COUPONS_QUERY } from '../../../../graphql/queries/user.graphql';
import type { UserCouponVo, CouponType, UserCouponStatus } from '@aquarush/common';
import {
  COUPON_STATUS_LABELS,
  COUPON_STATUS_TONE,
  COUPON_TYPE_LABELS,
  CouponType as CouponTypeEnum,
  UserCouponStatus as UserCouponStatusEnum,
  formatDateTime,
  getCouponDiscountText,
  getCouponThresholdText,
} from '@aquarush/common';
import { getToneColor } from '../../../../constants/status';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  titleRow: {
    display: 'flex',
    alignItems: 'center',
    rowGap: '12px',
    columnGap: '12px',
  },
  select: {
    width: '130px',
  },
});

interface CouponSectionProps {
  userId: string;
}

const STATUS_OPTIONS = [
  { value: 'ALL', label: '全部' },
  { value: UserCouponStatusEnum.RECEIVED, label: '未使用' },
  { value: UserCouponStatusEnum.USED, label: '已使用' },
  { value: UserCouponStatusEnum.RETURNED, label: '已退回' },
  { value: UserCouponStatusEnum.EXPIRED, label: '已过期' },
];

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
          // 折扣率万分比 ÷1000 转折数(8500‰ = 8.5折),共用 common 实现
          return record.discountRatePermille != null
            ? getCouponDiscountText({ couponType: record.couponType, discountRatePermille: record.discountRatePermille })
            : '-';
        }
        return record.discountAmountCents != null
          ? getCouponDiscountText({ couponType: record.couponType, discountAmountCents: record.discountAmountCents })
          : '-';
      },
    },
    {
      title: '使用门槛',
      key: 'threshold',
      width: 120,
      render: (_: unknown, record: UserCouponVo) => getCouponThresholdText(record.thresholdCents),
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
        <Tag color={getToneColor(COUPON_STATUS_TONE[s])}>{COUPON_STATUS_LABELS[s] ?? s}</Tag>
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
        <div {...stylex.props(styles.titleRow)}>
          <span>优惠券</span>
          <Select
            value={statusFilter}
            onChange={(value) => setStatusFilter(value as string)}
            options={STATUS_OPTIONS}
            {...stylex.props(styles.select)}
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
