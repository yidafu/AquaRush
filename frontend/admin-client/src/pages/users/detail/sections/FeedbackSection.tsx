import React, { useState } from 'react';
import { Card, Table, Tag, Empty, Alert } from 'antd';
import { useQuery } from '@apollo/client';
import { GET_FEEDBACKS_QUERY } from '../../../../graphql/queries/ops.graphql';
import type { Feedback, FeedbackType, FeedbackStatus } from '@aquarush/common';
import { formatDateTime } from '@aquarush/common';
import {
  FEEDBACK_TYPE_LABELS,
} from '../../../ops/feedbacks/components/FeedbackDetailModal';

interface FeedbackSectionProps {
  userId: string;
}

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'orange',
  PROCESSING: 'blue',
  RESOLVED: 'green',
};

const STATUS_LABELS: Record<FeedbackStatus, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已处理',
};

/**
 * 用户详情页「意见反馈」Tab(F-OPS-003):展示该用户提交的反馈记录,按 userId 过滤。
 */
const FeedbackSection: React.FC<FeedbackSectionProps> = ({ userId }) => {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const { data, loading, error } = useQuery(GET_FEEDBACKS_QUERY, {
    variables: {
      input: { userId, page, size: pageSize },
    },
    skip: !userId,
    errorPolicy: 'all',
  });


  if (error) {
    return <Alert type="error" showIcon message="意见反馈加载失败" description={error.message} />;
  }

  const feedbacks: Feedback[] = data?.feedbacks?.content || [];

  const columns = [
    {
      title: '类型',
      dataIndex: 'feedbackType',
      key: 'feedbackType',
      width: 140,
      render: (type: FeedbackType) => <Tag color="blue">{FEEDBACK_TYPE_LABELS[type] ?? type}</Tag>,
    },
    {
      title: '内容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
    },
    {
      title: '联系方式',
      dataIndex: 'contactInfo',
      key: 'contactInfo',
      width: 150,
      render: (v: string | null | undefined) => v || '—',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: FeedbackStatus) => (
        <Tag color={STATUS_COLORS[status]}>
          {STATUS_LABELS[status] ?? status}
        </Tag>
      ),
    },
    {
      title: '处理结果',
      dataIndex: 'handleResult',
      key: 'handleResult',
      ellipsis: true,
      render: (v: string | null | undefined) => v || '—',
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (t: string) => formatDateTime(t),
    },
  ];

  return (
    <Card title="意见反馈">
      <Table
        columns={columns}
        dataSource={feedbacks}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize,
          total: data?.feedbacks?.totalElements ?? 0,
          showSizeChanger: true,
          showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条/共 ${total} 条`,
          onChange: (nextPage, nextPageSize) => {
            setPage(nextPage - 1);
            setPageSize(nextPageSize);
          },
        }}
        scroll={{ x: 900 }}
        locale={{ emptyText: <Empty description="该用户暂无意见反馈" /> }}
      />
    </Card>
  );
};

export default FeedbackSection;
