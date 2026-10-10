import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Card, Col, Empty, Progress, Rate, Row, Spin, Statistic, Table, Tag } from 'antd';
import { useQuery } from '@apollo/client';
import {
  DELIVERY_WORKER_RATING_STATISTICS_QUERY,
  REVIEWS_BY_DELIVERY_WORKER_QUERY,
} from '../../../../../graphql/queries/user.graphql';
import type { DeliveryWorkerStatisticsResponse, ReviewVo } from '@aquarush/common';
import { formatDateTime } from '@aquarush/common';
import { pagination } from '@/utils/table';

interface WorkerReviewSectionProps {
  /** 配送身份 id(送水员档案并入 admins 单表后=账号 id;F-USR-021/023) */
  deliveryWorkerId: string;
}

/**
 * 账号详情页「评价记录」Tab(F-USR-020/021):
 * 展示该配送员/兼送水管理员收到的评价(reviewsByDeliveryWorker,分页)+
 * 评分分布与总评价数(deliveryWorkerRatingStatistics,1~5 星)。
 */
const WorkerReviewSection: React.FC<WorkerReviewSectionProps> = ({ deliveryWorkerId }) => {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const {
    data: reviewsData,
    loading: reviewsLoading,
    error: reviewsError,
  } = useQuery(REVIEWS_BY_DELIVERY_WORKER_QUERY, {
    variables: { deliveryWorkerId, page, size: pageSize },
    skip: !deliveryWorkerId,
    errorPolicy: 'all',
  });

  const {
    data: statsData,
    loading: statsLoading,
    error: statsError,
  } = useQuery(DELIVERY_WORKER_RATING_STATISTICS_QUERY, {
    variables: { deliveryWorkerId },
    skip: !deliveryWorkerId,
    errorPolicy: 'all',
  });

  useEffect(() => {
    if (deliveryWorkerId) {
      // 分页/用户变化时由 useQuery 变量变化自动重新请求
    }
  }, [deliveryWorkerId, page, pageSize]);

  const reviews: ReviewVo[] = reviewsData?.reviewsByDeliveryWorker?.content || [];
  const stats: DeliveryWorkerStatisticsResponse | undefined = statsData?.deliveryWorkerRatingStatistics;

  const totalReviews = stats?.totalReviews ?? 0;

  // 评分分布(1~5 星计数,无记录时全 0)
  const distribution = useMemo(() => {
    const counts: Record<number, number> = {
      5: stats?.fiveStarReviews ?? 0,
      4: stats?.fourStarReviews ?? 0,
      3: stats?.threeStarReviews ?? 0,
      2: stats?.twoStarReviews ?? 0,
      1: stats?.oneStarReviews ?? 0,
    };
    return [5, 4, 3, 2, 1].map((star) => ({
      star,
      count: counts[star],
      percent: totalReviews > 0 ? Math.round((counts[star] / totalReviews) * 100) : 0,
    }));
  }, [stats, totalReviews]);

  if (reviewsError || statsError) {
    return (
      <Alert
        type="error"
        showIcon
        message="评价记录加载失败"
        description={reviewsError?.message || statsError?.message}
      />
    );
  }

  const columns = [
    {
      title: '订单号',
      dataIndex: 'orderId',
      key: 'orderId',
      width: 120,
      render: (orderId: string) => `#${orderId}`,
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
      render: (_: unknown, record: ReviewVo) =>
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
      <Spin spinning={statsLoading}>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Statistic title="总评价数" value={totalReviews} />
          </Col>
          <Col span={6}>
            <Statistic title="平均评分" value={stats?.averageRating ?? 0} precision={2} suffix="/ 5.0" />
          </Col>
          <Col span={12}>
            {distribution.map((d) => (
              <div key={d.star} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                <span style={{ width: 36, flexShrink: 0 }}>{d.star} 星</span>
                <Progress percent={d.percent} size="small" style={{ flex: 1 }} />
                <span style={{ width: 24, textAlign: 'right' }}>{d.count}</span>
              </div>
            ))}
          </Col>
        </Row>
      </Spin>

      <Table
        columns={columns}
        dataSource={reviews}
        rowKey="reviewId"
        loading={reviewsLoading}
        pagination={pagination(reviewsData?.reviewsByDeliveryWorker?.totalElements ?? 0, {
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
        locale={{ emptyText: <Empty description="该账号暂无评价记录" /> }}
      />
    </Card>
  );
};

export default WorkerReviewSection;
