import React, { useState } from 'react';
import { Card, Table, Rate, Tag, Empty, Alert } from 'antd';
import { useQuery } from '@apollo/client';
import { REVIEWS_BY_USER_QUERY } from '../../../../graphql/queries/user.graphql';
import type { Review } from '@aquarush/common';
import { formatDateTime } from '@aquarush/common';

interface ReviewSectionProps {
  userId: string;
}

/**
 * 用户详情页「评价记录」Tab(F-ORD-007):展示该用户对配送员的历史评价,分页。
 */
const ReviewSection: React.FC<ReviewSectionProps> = ({ userId }) => {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const { data, loading, error } = useQuery(REVIEWS_BY_USER_QUERY, {
    variables: { userId, page, size: pageSize },
    skip: !userId,
    errorPolicy: 'all',
  });


  if (error) {
    return <Alert type="error" showIcon message="评价记录加载失败" description={error.message} />;
  }

  const reviews: Review[] = data?.reviewsByUser?.content || [];

  const columns = [
    {
      title: '配送员',
      dataIndex: 'deliveryWorkerName',
      key: 'deliveryWorkerName',
      width: 140,
      render: (name: string | null | undefined) => name || '未知配送员',
    },
    {
      title: '评分',
      dataIndex: 'rating',
      key: 'rating',
      width: 160,
      render: (rating: number) => <Rate disabled value={rating} />,
    },
    {
      title: '评价内容',
      dataIndex: 'comment',
      key: 'comment',
      ellipsis: true,
      render: (comment: string | null | undefined) => comment || '—',
    },
    {
      title: '匿名',
      key: 'isAnonymous',
      width: 80,
      render: (_: unknown, record: Review) =>
        record.isAnonymous ? <Tag color="default">匿名</Tag> : <Tag color="green">实名</Tag>,
    },
    {
      title: '评价时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (t: string) => formatDateTime(t),
    },
  ];

  return (
    <Card title="评价记录">
      <Table
        columns={columns}
        dataSource={reviews}
        rowKey="reviewId"
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize,
          total: data?.reviewsByUser?.totalElements ?? 0,
          showSizeChanger: true,
          showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条/共 ${total} 条`,
          onChange: (nextPage, nextPageSize) => {
            setPage(nextPage - 1);
            setPageSize(nextPageSize);
          },
        }}
        scroll={{ x: 800 }}
        locale={{ emptyText: <Empty description="该用户暂无评价记录" /> }}
      />
    </Card>
  );
};

export default ReviewSection;
