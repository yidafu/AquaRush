import React, { useState } from 'react';
import { Card, Table, Tag, Empty, Alert } from '@/ui';
import { useQuery } from '@apollo/client';
import { GET_FEEDBACKS_QUERY } from '../../../../graphql/queries/ops.graphql';
import type { FeedbackVo, FeedbackType, FeedbackStatus } from '@aquarush/common';
import { FEEDBACK_STATUS_LABELS, FEEDBACK_STATUS_TONE, FEEDBACK_TYPE_LABELS, formatDateTime } from '@aquarush/common';
import { getToneColor } from '../../../../constants/status';
import { pagination } from '@/utils/table';

interface FeedbackSectionProps {
  userId: string;
}

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

  const feedbacks: FeedbackVo[] = data?.feedbacks?.content || [];

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
        <Tag color={getToneColor(FEEDBACK_STATUS_TONE[status])}>
          {FEEDBACK_STATUS_LABELS[status] ?? status}
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
        pagination={pagination(data?.feedbacks?.totalElements ?? 0, {
          current: page + 1,
          pageSize,
          showSizeChanger: true,
          showTotal: 'range',
          onChange: (nextPage, nextPageSize) => {
            setPage(nextPage - 1);
            setPageSize(nextPageSize);
          },
        })}
        scroll={{ x: 900 }}
        locale={{ emptyText: <Empty description="该用户暂无意见反馈" /> }}
      />
    </Card>
  );
};

export default FeedbackSection;
