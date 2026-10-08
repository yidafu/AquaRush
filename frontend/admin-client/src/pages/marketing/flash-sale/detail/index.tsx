import React, { useState } from 'react';
import { Alert, Button, Card, Descriptions, Space, Spin, Tag } from 'antd';
import type { TablePaginationConfig } from 'antd/es/table';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useQuery } from '@apollo/client';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { displayCents, formatDateTime } from '@aquarush/common';
import type { FlashSaleActivity, MarketingUsageVo } from '@aquarush/common';
import { FLASH_SALE_USAGE_PAGE_DATA_QUERY } from '../../../../graphql/queries/marketing.graphql';
import MarketingUsageSummaryCards from '../../components/MarketingUsageSummaryCards';
import MarketingUsageOrdersTable from '../../components/MarketingUsageOrdersTable';

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
 * 限时特价活动详情页:营销使用情况 + 相关订单记录(F-MKT-005)
 * 独立页面:单请求取回「活动配置 + 使用汇总 + 订单分页」(规则 #10);
 * 订单按「商品+活动价+时间窗」匹配(订单未落活动 ID 快照,近似口径)。
 * 路由:/marketing/flash-sale/:id?name=
 */
const FlashSaleDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const name = searchParams.get('name') || '活动';
  const variables = id ? { id, input: { page, size } } : null;

  const { data, loading, error } = useQuery<{
    flashSaleActivity?: FlashSaleActivity | null;
    flashSaleActivityUsage?: MarketingUsageVo | null;
  }>(FLASH_SALE_USAGE_PAGE_DATA_QUERY, {
    variables: variables ?? undefined,
    skip: !id,
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });

  const config = data?.flashSaleActivity;
  const usage = data?.flashSaleActivityUsage;

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPage(pagination.current - 1);
    setSize(pagination.pageSize);
  };

  return (
    <Card
      title={
        <Space>
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/marketing/flash-sale')} />
          <span>{name} · 使用情况与相关订单</span>
        </Space>
      }
    >
      {error && <Alert type="error" showIcon message="加载失败" description={error.message} />}
      <Spin spinning={loading}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          {config && (
            <Descriptions bordered size="small" column={3}>
              <Descriptions.Item label="活动名称">{config.name}</Descriptions.Item>
              <Descriptions.Item label="商品" span={2}>
                {config.productName || `商品#${config.productId}`}
              </Descriptions.Item>
              <Descriptions.Item label="目录价">{displayCents(config.catalogPriceCents)}</Descriptions.Item>
              <Descriptions.Item label="特价">
                <span style={{ color: '#f5222d' }}>{displayCents(config.salePriceCents)}</span>
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
            benefitLabel="让利金额(估算)"
            usageLabel="特价商品件数"
            usageSuffix="件"
          />

          <MarketingUsageOrdersTable
            orders={usage?.orders}
            loading={loading}
            page={page}
            size={size}
            onTableChange={handleTableChange}
          />
        </Space>
      </Spin>
    </Card>
  );
};

export default FlashSaleDetailPage;
