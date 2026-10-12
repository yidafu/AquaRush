import React, { useState } from 'react';
import { Card, Table, Tag, Empty, Alert, Image } from '@aquarush/admin-ui';
import { useQuery } from '@apollo/client';
import { USER_FAVORITES_QUERY } from '../../../../graphql/queries/user.graphql';
import type { FavoriteProductVo, ProductStatus } from '@aquarush/common';
import { displayCents, formatDateTime } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { pagination } from '@/utils/table';

const styles = stylex.create({
  productCell: {
    display: 'flex',
    alignItems: 'center',
    rowGap: '8px',
    columnGap: '8px',
  },
  cover: {
    objectFit: 'cover',
    borderRadius: '4px',
  },
  subtitle: {
    fontSize: tokens.fsNote,
    // 局部色值:辅助信息灰,token 无对应色
    color: '#999999',
  },
});

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

  const favorites: FavoriteProductVo[] = data?.favoriteProducts?.list || [];

  const columns = [
    {
      title: '商品',
      key: 'product',
      width: 260,
      render: (_: unknown, record: FavoriteProductVo) => (
        <div {...stylex.props(styles.productCell)}>
          <Image
            src={record.coverImageUrl}
            width={40}
            height={40}
            {...stylex.props(styles.cover)}
            preview={false}
          />
          <div>
            <div>{record.name}</div>
            {record.subtitle && (
              <div {...stylex.props(styles.subtitle)}>{record.subtitle}</div>
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
