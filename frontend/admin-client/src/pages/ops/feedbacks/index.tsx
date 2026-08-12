import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Space, Button, Card, Select, Input, DatePicker } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { Dayjs } from 'dayjs';
import { formatDateTime } from '@aquarush/common';
import type { Feedback, FeedbackType, FeedbackStatus } from '@aquarush/common';
import { useFeedbacks, useReviewFeedback } from '../../../services/ops-graphql';
import FeedbackDetailModal, {
  FEEDBACK_TYPE_LABELS,
  FEEDBACK_STATUS_OPTIONS,
  type ReviewPayload,
} from './components/FeedbackDetailModal';

const { RangePicker } = DatePicker;

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'orange',
  PROCESSING: 'blue',
  RESOLVED: 'green',
};

const TYPE_OPTIONS = Object.entries(FEEDBACK_TYPE_LABELS).map(([value, label]) => ({ value, label }));

interface Filters {
  feedbackType?: FeedbackType;
  status?: FeedbackStatus;
  contactKeyword?: string;
  dateRange?: [Dayjs, Dayjs];
}

// 意见反馈管理页(F-OPS-003):筛选/列表/详情/处理闭环
const Feedbacks: React.FC = () => {
  const [loadFeedbacks, { data, loading }] = useFeedbacks();
  const [reviewFeedback, { loading: reviewing }] = useReviewFeedback();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [filters, setFilters] = useState<Filters>({});
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailRecord, setDetailRecord] = useState<Feedback | null>(null);

  const buildVariables = useCallback(
    (p: number, s: number, f: Filters) => {
      const [dateFrom, dateTo] = f.dateRange ?? [];
      return {
        input: {
          page: p,
          size: s,
          feedbackType: f.feedbackType,
          status: f.status,
          contactKeyword: f.contactKeyword || undefined,
          dateFrom: dateFrom ? dateFrom.startOf('day').format('YYYY-MM-DDTHH:mm:ss') : undefined,
          dateTo: dateTo ? dateTo.endOf('day').format('YYYY-MM-DDTHH:mm:ss') : undefined,
        },
      };
    },
    [],
  );

  const load = useCallback(
    (p = page, s = size, f = filters) => {
      loadFeedbacks({ variables: buildVariables(p, s, f) });
    },
    [loadFeedbacks, page, size, filters, buildVariables],
  );

  useEffect(() => {
    load();
  }, [load]);

  const handleSearch = () => {
    setPage(0);
    load(0, size, filters);
  };

  const handleReset = () => {
    setFilters({});
    setPage(0);
    load(0, size, {});
  };

  const handleTableChange = (pagination: any) => {
    const nextPage = pagination.current - 1;
    setPage(nextPage);
    setSize(pagination.pageSize);
    load(nextPage, pagination.pageSize, filters);
  };

  const handleOpenDetail = (record: Feedback) => {
    setDetailRecord(record);
    setDetailOpen(true);
  };

  const handleSubmit = async (payload: ReviewPayload, record: Feedback) => {
    await reviewFeedback({ variables: { id: record.id, input: payload } });
    setDetailOpen(false);
  };

  const columns: ColumnsType<Feedback> = [
    {
      title: '类型',
      dataIndex: 'feedbackType',
      width: 140,
      render: (type: FeedbackType) => <Tag color="blue">{FEEDBACK_TYPE_LABELS[type] ?? type}</Tag>,
    },
    {
      title: '内容',
      dataIndex: 'content',
      ellipsis: true,
      render: (content: string) => <span>{content}</span>,
    },
    { title: '联系方式', dataIndex: 'contactInfo', width: 140, render: (v: string) => v || '—' },
    {
      title: '图片',
      width: 70,
      render: (_, record) => <span>{record.images?.length ?? 0} 张</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: FeedbackStatus) => (
        <Tag color={STATUS_COLORS[status]}>{FEEDBACK_STATUS_OPTIONS.find((o) => o.value === status)?.label}</Tag>
      ),
    },
    { title: '提交时间', dataIndex: 'createdAt', width: 170, render: (v: string) => formatDateTime(v) },
    {
      title: '操作',
      width: 90,
      render: (_, record) => (
        <Button size="small" type="link" onClick={() => handleOpenDetail(record)}>
          详情
        </Button>
      ),
    },
  ];

  const feedbacks = data?.feedbacks?.content ?? [];
  const total = data?.feedbacks?.totalElements ?? 0;

  return (
    <Card title="意见反馈">
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          placeholder="反馈类型"
          allowClear
          style={{ width: 160 }}
          options={TYPE_OPTIONS}
          value={filters.feedbackType}
          onChange={(value: FeedbackType | undefined) => setFilters((f) => ({ ...f, feedbackType: value }))}
        />
        <Select
          placeholder="处理状态"
          allowClear
          style={{ width: 140 }}
          options={FEEDBACK_STATUS_OPTIONS}
          value={filters.status}
          onChange={(value: FeedbackStatus | undefined) => setFilters((f) => ({ ...f, status: value }))}
        />
        <Input
          placeholder="联系方式关键字"
          allowClear
          style={{ width: 180 }}
          value={filters.contactKeyword}
          onChange={(e) => setFilters((f) => ({ ...f, contactKeyword: e.target.value }))}
        />
        <RangePicker
          showTime
          value={filters.dateRange}
          onChange={(range) => setFilters((f) => ({ ...f, dateRange: range as [Dayjs, Dayjs] | undefined }))}
        />
        <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
          搜索
        </Button>
        <Button icon={<ReloadOutlined />} onClick={handleReset}>
          重置
        </Button>
      </Space>

      <Table<Feedback>
        rowKey="id"
        columns={columns}
        dataSource={feedbacks}
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize: size,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
        }}
        onChange={handleTableChange}
        locale={{ emptyText: '暂无反馈' }}
      />

      <FeedbackDetailModal
        open={detailOpen}
        record={detailRecord}
        submitting={reviewing}
        onCancel={() => setDetailOpen(false)}
        onSubmit={handleSubmit}
      />
    </Card>
  );
};

export default Feedbacks;
