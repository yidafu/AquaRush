import React, { useState } from 'react';
import { Alert, Button, Card, Descriptions, Space, Spin, Tag } from 'antd';
import type { TablePaginationConfig } from 'antd/es/table';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useQuery } from '@apollo/client';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import * as stylex from '@stylexjs/stylex';
import { displayCents, formatDateTime } from '@aquarush/common';
import type { MarketingUsageVo, PromotionActivityVo } from '@aquarush/common';
import { PROMOTION_ACTIVITY_USAGE_PAGE_DATA_QUERY } from '../../../../graphql/queries/marketing.graphql';
import MarketingUsageSummaryCards from '../../components/MarketingUsageSummaryCards';
import MarketingUsageOrdersTable from '../../components/MarketingUsageOrdersTable';

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
});

const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  FULL_REDUCE: '满减',
  FULL_DISCOUNT: '满折',
};

const SCOPE_LABELS: Record<string, string> = {
  ALL: '全场',
  PRODUCT: '指定商品',
  GROUP: '指定分组',
};

const DISPLAY_STATUS_LABELS: Record<string, string> = {
  UPCOMING: '未开始',
  ONGOING: '进行中',
  ENDED: '已结束',
  DISABLED: '已停用',
};

const DISPLAY_STATUS_COLORS: Record<string, string> = {
  UPCOMING: 'blue',
  ONGOING: 'green',
  ENDED: 'default',
  DISABLED: 'red',
};

/**
 * 满减/折扣活动详情页:营销使用情况 + 相关订单记录(F-MKT-004)
 * 独立页面:单请求取回「活动配置(含档位)+ 使用汇总 + 订单分页」(规则 #10)。
 * 路由:/marketing/promotions/:id?name=
 */
const PromotionDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const name = searchParams.get('name') || '活动';
  const variables = id ? { id, input: { page, size } } : null;

  const { data, loading, error } = useQuery<{
    promotionActivity?: PromotionActivityVo | null;
    promotionActivityUsage?: MarketingUsageVo | null;
  }>(PROMOTION_ACTIVITY_USAGE_PAGE_DATA_QUERY, {
    variables: variables ?? undefined,
    skip: !id,
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });

  const config = data?.promotionActivity;
  const usage = data?.promotionActivityUsage;

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPage((pagination.current ?? 1) - 1);
    setSize(pagination.pageSize ?? 20);
  };

  const renderTier = (tier: { thresholdCents: number; discountAmountCents?: number | null; discountRatePermille?: number | null }) => {
    if (tier.discountAmountCents) {
      return `满${displayCents(tier.thresholdCents)}减${displayCents(tier.discountAmountCents)}`;
    }
    return tier.discountRatePermille != null
      ? `满${displayCents(tier.thresholdCents)}打${(tier.discountRatePermille / 1000).toFixed(1)}折`
      : `满${displayCents(tier.thresholdCents)}`;
  };

  return (
    <Card
      title={
        <Space>
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/marketing/promotions')} />
          <span>{name} · 使用情况与相关订单</span>
        </Space>
      }
    >
      {error && <Alert type="error" showIcon message="加载失败" description={error.message} />}
      <Spin spinning={loading}>
        <Space direction="vertical" size={16} {...stylex.props(styles.fullWidth)}>
          {config && (
            <Descriptions bordered size="small" column={3}>
              <Descriptions.Item label="活动名称">{config.name}</Descriptions.Item>
              <Descriptions.Item label="类型">
                <Tag color="blue">{ACTIVITY_TYPE_LABELS[config.activityType] ?? config.activityType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="适用范围">
                {SCOPE_LABELS[config.scope] ?? config.scope}
              </Descriptions.Item>
              <Descriptions.Item label="档位" span={2}>
                {(config.tiers ?? []).map((tier) => renderTier(tier)).join('; ')}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={DISPLAY_STATUS_COLORS[config.currentStatus] ?? 'default'}>
                  {DISPLAY_STATUS_LABELS[config.currentStatus] ?? config.currentStatus}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="时间窗" span={2}>
                {formatDateTime(config.validFrom)} ~ {formatDateTime(config.validTo)}
              </Descriptions.Item>
            </Descriptions>
          )}

          <MarketingUsageSummaryCards
            summary={usage?.summary}
            loading={loading}
            benefitLabel="优惠金额"
            usageLabel="使用订单数"
            usageSuffix="单"
          />

          <MarketingUsageOrdersTable
            orders={usage?.orders}
            loading={loading}
            page={page}
            size={size}
            onTableChange={handleTableChange}
            discountField="promotion"
          />
        </Space>
      </Spin>
    </Card>
  );
};

export default PromotionDetailPage;
