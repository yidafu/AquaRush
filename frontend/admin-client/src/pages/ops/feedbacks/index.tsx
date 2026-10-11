import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Space, Button, Card, Select, Input, DatePicker } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import {
  FEEDBACK_STATUS_OPTIONS,
  FEEDBACK_STATUS_TONE,
  FEEDBACK_TYPE_LABELS,
  formatDateTime,
} from '@aquarush/common';
import type { FeedbackVo, FeedbackType, FeedbackStatus } from '@aquarush/common';
import { getToneColor } from '../../../constants/status';
import { useFeedbacks, useReviewFeedback } from '../../../services/ops-graphql';
import FeedbackDetailModal, { type ReviewPayload } from './components/FeedbackDetailModal';
import { pagination } from '@/utils/table';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  filterSpace: {
    marginBottom: '16px',
  },
  typeSelect: {
    width: '160px',
  },
  statusSelect: {
    width: '140px',
  },
  contactInput: {
    width: '180px',
  },
});

const { RangePicker } = DatePicker;

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
  const [detailRecord, setDetailRecord] = useState<FeedbackVo | null>(null);

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

  const handleOpenDetail = (record: FeedbackVo) => {
    setDetailRecord(record);
    setDetailOpen(true);
  };

  const handleSubmit = async (payload: ReviewPayload, record: FeedbackVo) => {
    await reviewFeedback({ variables: { id: record.id, input: payload } });
    setDetailOpen(false);
  };

  const columns: ColumnsType<FeedbackVo> = [
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
        <Tag color={getToneColor(FEEDBACK_STATUS_TONE[status])}>
          {FEEDBACK_STATUS_OPTIONS.find((o) => o.value === status)?.label}
        </Tag>
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
      <Space wrap {...stylex.props(styles.filterSpace)}>
        <Select
          placeholder="反馈类型"
          allowClear
          {...stylex.props(styles.typeSelect)}
          options={TYPE_OPTIONS}
          value={filters.feedbackType}
          onChange={(value: FeedbackType | undefined) => setFilters((f) => ({ ...f, feedbackType: value }))}
        />
        <Select
          placeholder="处理状态"
          allowClear
          {...stylex.props(styles.statusSelect)}
          options={FEEDBACK_STATUS_OPTIONS}
          value={filters.status}
          onChange={(value: FeedbackStatus | undefined) => setFilters((f) => ({ ...f, status: value }))}
        />
        <Input
          placeholder="联系方式关键字"
          allowClear
          {...stylex.props(styles.contactInput)}
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

      <Table<FeedbackVo>
        rowKey="id"
        columns={columns}
        dataSource={feedbacks}
        loading={loading}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: size,
          showSizeChanger: true,
        })}
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
