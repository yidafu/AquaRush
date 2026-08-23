import React, { useState } from 'react';
import { Card, Table, Tag, Empty, Alert, Image } from 'antd';
import { useQuery } from '@apollo/client';
import { USER_FAVORITES_QUERY } from '../../../../graphql/queries/user.graphql';
import type { FavoriteProduct, ProductStatus } from '@aquarush/common';
import { displayCents, formatDateTime } from '@aquarush/common';
import { pagination } from '@/utils/table';

interface FavoriteSectionProps {
  userId: string;
}

const STATUS_LABELS: Record<ProductStatus, string> = {
  ONLINE: '在售',
  OFFLINE: '已下架',
  OUT_OF_STOCK: '缺货',
};

const STATUS_COLORS: Record<ProductStatus, string> = {
  ONLINE: 'success',
  OFFLINE: 'default',
  OUT_OF_STOCK: 'warning',
};

/**
 * 用户详情页「收藏商品」Tab:展示该用户收藏的商品,分页。
 */
const FavoriteSection: React.FC<FavoriteSectionProps> = ({ userId }) => {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const { data, loading, error } = useQuery(USER_FAVORITES_QUERY, {
    variables: { userId, page, size: pageSize },
    skip: !userId,
    errorPolicy: 'all',
  });


  if (error) {
    return <Alert type="error" showIcon message="收藏商品加载失败" description={error.message} />;
  }

  const favorites: FavoriteProduct[] = data?.favoriteProducts?.list || [];

  const columns = [
    {
      title: '商品',
      key: 'product',
      width: 260,
      render: (_: unknown, record: FavoriteProduct) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Image
            src={record.coverImageUrl}
            width={40}
            height={40}
            style={{ objectFit: 'cover', borderRadius: 4 }}
            preview={false}
          />
          <div>
            <div>{record.name}</div>
            {record.subtitle && (
              <div style={{ fontSize: '12px', color: '#999' }}>{record.subtitle}</div>
            )}
          </div>
        </div>
      ),
    },
    {
      title: '价格',
      dataIndex: 'priceCents',
      key: 'priceCents',
      width: 110,
      render: (priceCents: number) => <span>{displayCents(priceCents)}</span>,
    },
    {
      title: '销量',
      dataIndex: 'salesVolume',
      key: 'salesVolume',
      width: 90,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: ProductStatus) => (
        <Tag color={STATUS_COLORS[status]}>{STATUS_LABELS[status] ?? status}</Tag>
      ),
    },
    {
      title: '收藏时间',
      dataIndex: 'addedAt',
      key: 'addedAt',
      width: 160,
      render: (t: string) => formatDateTime(t),
    },
  ];

  return (
    <Card title="收藏商品">
      <Table
        columns={columns}
        dataSource={favorites}
        rowKey="id"
        loading={loading}
        pagination={pagination(data?.favoriteProducts?.pageInfo?.total ?? 0, {
          current: page + 1,
          pageSize,
          showSizeChanger: true,
          showTotal: 'range',
          onChange: (nextPage, nextPageSize) => {
            setPage(nextPage - 1);
            setPageSize(nextPageSize);
          },
        })}
        scroll={{ x: 800 }}
        locale={{ emptyText: <Empty description="该用户暂无收藏商品" /> }}
      />
    </Card>
  );
};

export default FavoriteSection;
