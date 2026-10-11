import React, { useState } from 'react';
import { Alert, Button, Card, Descriptions, Space, Spin, Tag } from 'antd';
import type { TablePaginationConfig } from 'antd/es/table';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useQuery } from '@apollo/client';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { displayCents, formatDateTime, COUPON_TYPE_LABELS } from '@aquarush/common';
import type { CouponTemplateAdminVo, MarketingUsageVo } from '@aquarush/common';
import { COUPON_TEMPLATE_USAGE_PAGE_DATA_QUERY } from '../../../../graphql/queries/marketing.graphql';
import MarketingUsageSummaryCards from '../../components/MarketingUsageSummaryCards';
import MarketingUsageOrdersTable from '../../components/MarketingUsageOrdersTable';

/**
 * 优惠券模板详情页:营销使用情况 + 相关订单记录(F-MKT-003)
 * 独立页面(替代抽屉):单请求取回「券配置 + 使用汇总 + 订单分页」(规则 #10)。
 * 路由:/marketing/coupons/:id?name=
 */
const CouponDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const name = searchParams.get('name') || '优惠券';
  const variables = id ? { id, input: { page, size } } : null;

  const { data, loading, error } = useQuery<{
    couponTemplate?: CouponTemplateAdminVo | null;
    couponTemplateUsage?: MarketingUsageVo | null;
  }>(COUPON_TEMPLATE_USAGE_PAGE_DATA_QUERY, {
    variables: variables ?? undefined,
    skip: !id,
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });

  const config = data?.couponTemplate;
  const usage = data?.couponTemplateUsage;

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPage((pagination.current ?? 1) - 1);
    setSize(pagination.pageSize ?? 20);
  };

  const renderDiscount = (record: CouponTemplateAdminVo) => {
    if (record.couponType === 'DISCOUNT') {
      return record.discountRatePermille != null ? `${(record.discountRatePermille / 1000).toFixed(1)}折` : '—';
    }
    return displayCents(record.discountAmountCents);
  };

  const renderValidity = (record: CouponTemplateAdminVo) => {
    if (record.validityType === 'FIXED_RANGE') {
      return record.validFrom && record.validTo
        ? `${formatDateTime(record.validFrom)} ~ ${formatDateTime(record.validTo)}`
        : '—';
    }
    return `领取后${record.validDays ?? '—'}天内有效`;
  };

  return (
    <Card
      title={
        <Space>
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/marketing/coupons')} />
          <span>{name} · 使用情况与相关订单</span>
        </Space>
      }
    >
      {error && <Alert type="error" showIcon message="加载失败" description={error.message} />}
      <Spin spinning={loading}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          {config && (
            <Descriptions bordered size="small" column={3}>
              <Descriptions.Item label="券名称">{config.name}</Descriptions.Item>
              <Descriptions.Item label="类型">
                <Tag color="blue">{COUPON_TYPE_LABELS[config.couponType] ?? config.couponType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="面额或折扣率">{renderDiscount(config)}</Descriptions.Item>
              <Descriptions.Item label="门槛">
                {config.couponType === 'NO_THRESHOLD_CASH' || !config.thresholdCents
                  ? '无门槛'
                  : displayCents(config.thresholdCents)}
              </Descriptions.Item>
              <Descriptions.Item label="适用范围">{config.scope === 'ALL' ? '全场' : '指定商品'}</Descriptions.Item>
              <Descriptions.Item label="有效期">{renderValidity(config)}</Descriptions.Item>
              <Descriptions.Item label="发放方式">
                {config.issuanceType === 'AUTO_CLAIM' ? '自动领取' : '定向发放'}
              </Descriptions.Item>
              <Descriptions.Item label="限领数">{config.perUserLimit}</Descriptions.Item>
              <Descriptions.Item label="已领 / 已核销">
                {config.receivedCount} / {config.usedCount}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                {config.status === 'ACTIVE' ? <Tag color="green">上架</Tag> : <Tag>停用</Tag>}
              </Descriptions.Item>
            </Descriptions>
          )}

          <MarketingUsageSummaryCards
            summary={usage?.summary}
            loading={loading}
            benefitLabel="优惠金额"
            usageLabel="已核销券"
            usageSuffix="张"
          />

          <MarketingUsageOrdersTable
            orders={usage?.orders}
            loading={loading}
            page={page}
            size={size}
            onTableChange={handleTableChange}
            discountField="coupon"
          />
        </Space>
      </Spin>
    </Card>
  );
};

export default CouponDetailPage;
