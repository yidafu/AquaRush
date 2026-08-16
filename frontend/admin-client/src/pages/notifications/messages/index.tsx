import React, { useState, useCallback, useEffect } from 'react';
import {
  Table,
  Card,
  Select,
  Input,
  DatePicker,
  Space,
  Tag,
  Button,
  Modal,
  Descriptions,
  message,
} from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import {
  useNotificationsQuery,
  useNotificationDetail,
} from '../../../services/notification-graphql';

const { RangePicker } = DatePicker;
const { Option } = Select;

interface NotificationRecord {
  id: string;
  receiverType: 'USER' | 'WORKER';
  type: 'ORDER' | 'SYSTEM' | 'ANNOUNCEMENT' | 'FEEDBACK';
  scene: string;
  title: string;
  summary?: string | null;
  relatedNo?: string | null;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
}

const TYPE_LABELS: Record<string, { label: string; color: string }> = {
  ORDER: { label: '订单', color: 'blue' },
  SYSTEM: { label: '系统', color: 'default' },
  ANNOUNCEMENT: { label: '公告', color: 'green' },
  FEEDBACK: { label: '反馈', color: 'orange' },
};

const RECEIVER_LABELS: Record<string, string> = {
  USER: '用户',
  WORKER: '送水工',
};

/**
 * 后台消息列表页(F-NTF-006,PRD 13-notification.md)
 * 按类型/接收人/关键字/已读状态/时间范围组合筛选,默认近 30 天;详情弹窗展示全文与状态。
 * 权限:PERMISSION_NOTIFICATION_READ(管理员/超管,后端强制)。
 */
const NotificationMessages: React.FC = () => {
  const [filters, setFilters] = useState<{
    messageType?: string;
    receiverType?: string;
    keyword?: string;
    read?: string; // 'READ' | 'UNREAD'
    range?: [Dayjs | null, Dayjs | null] | null;
  }>({});
  const [pageNum, setPageNum] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [detail, setDetail] = useState<NotificationRecord | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [loadDetail, { data: detailData, loading: detailLoading }] = useNotificationDetail(null);

  const { data, loading, error } = useNotificationsQuery({
    messageType: filters.messageType,
    receiverType: filters.receiverType,
    keyword: filters.keyword,
    read: filters.read === 'READ' ? true : filters.read === 'UNREAD' ? false : undefined,
    startAt: filters.range?.[0]?.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    endAt: filters.range?.[1]?.endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    page: pageNum,
    size: pageSize,
  });

  useEffect(() => {
    if (error) {
      message.error(error.message || '消息列表加载失败');
    }
  }, [error]);

  const list: NotificationRecord[] = data?.notifications?.content ?? [];
  const total = data?.notifications?.totalElements ?? 0;

  const handleSearch = () => {
    setPageNum(0);
  };

  const handleReset = () => {
    setFilters({});
    setPageNum(0);
  };

  const openDetail = useCallback(
    (record: NotificationRecord) => {
      setDetail(record);
      setDetailVisible(true);
      loadDetail({ variables: { id: record.id } });
    },
    [loadDetail],
  );

  const detailRecord: NotificationRecord | null = detailData?.notification ?? detail;

  const columns: ColumnsType<NotificationRecord> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 90,
      render: (v: string) => {
        const info = TYPE_LABELS[v] ?? { label: v, color: 'default' };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: '标题',
      dataIndex: 'title',
      ellipsis: true,
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      ellipsis: true,
      render: (v?: string | null) => v || '-',
    },
    {
      title: '接收人',
      dataIndex: 'receiverType',
      width: 90,
      render: (v: string) => RECEIVER_LABELS[v] ?? v ?? '—',
    },
    {
      title: '关联单号',
      dataIndex: 'relatedNo',
      width: 130,
      render: (v?: string | null) => v || '-',
    },
    {
      title: '状态',
      dataIndex: 'read',
      width: 90,
      render: (v: boolean) => (v ? <Tag color='default'>已读</Tag> : <Tag color='red'>未读</Tag>),
    },
    {
      title: '操作',
      width: 80,
      render: (_, record) => <Button type='link' size='small' onClick={() => openDetail(record)}>详情</Button>,
    },
  ];

  return (
    <Card title='消息列表' extra={<Space>共 {total} 条</Space>}>
      {/* 筛选区 */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          placeholder='消息类型'
          allowClear
          style={{ width: 130 }}
          value={filters.messageType}
          onChange={(v) => setFilters((f) => ({ ...f, messageType: v }))}
        >
          {Object.entries(TYPE_LABELS).map(([value, info]) => (
            <Option key={value} value={value}>{info.label}</Option>
          ))}
        </Select>
        <Select
          placeholder='接收人'
          allowClear
          style={{ width: 120 }}
          value={filters.receiverType}
          onChange={(v) => setFilters((f) => ({ ...f, receiverType: v }))}
        >
          {Object.entries(RECEIVER_LABELS).map(([value, label]) => (
            <Option key={value} value={value}>{label}</Option>
          ))}
        </Select>
        <Input.Search
          placeholder='关键字(关联单号/标题/摘要)'
          allowClear
          style={{ width: 240 }}
          value={filters.keyword}
          onChange={(e) => setFilters((f) => ({ ...f, keyword: e.target.value }))}
          onSearch={handleSearch}
          enterButton
        />
        <Select
          placeholder='已读状态'
          allowClear
          style={{ width: 110 }}
          value={filters.read}
          onChange={(v: unknown) => setFilters((f) => ({ ...f, read: v === 'READ' || v === 'UNREAD' ? v : undefined }))}
        >
          <Option value='READ'>已读</Option>
          <Option value='UNREAD'>未读</Option>
        </Select>
        <RangePicker
          allowClear
          value={filters.range}
          onChange={(range) => setFilters((f) => ({ ...f, range }))}
        />

        <Button type='primary' onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
      </Space>

      <Table<NotificationRecord>
        rowKey='id'
        columns={columns}
        dataSource={list}
        loading={loading}
        pagination={{
          current: pageNum + 1,
          pageSize,
          total,
          showSizeChanger: true,
          pageSizeOptions: [10, 20, 50],
          onChange: (page, size) => {
            setPageNum(page - 1);
            setPageSize(size);
          },
          onShowSizeChange: (_current, size) => {
            setPageNum(0);
            setPageSize(size);
          },
        }}
      />

      {/* 详情弹窗 */}
      <Modal
        title='消息详情'
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={560}
      >
        {detailLoading && !detailRecord ? (
          <div style={{ textAlign: 'center', padding: 24 }}>加载中...</div>
        ) : detailRecord ? (
          <Descriptions column={1} size='middle'>
            <Descriptions.Item label='标题'>{detailRecord.title}</Descriptions.Item>
            <Descriptions.Item label='摘要'>{detailRecord.summary || '-'}</Descriptions.Item>
            <Descriptions.Item label='类型'>
              <Tag color={TYPE_LABELS[detailRecord.type]?.color}>{TYPE_LABELS[detailRecord.type]?.label ?? detailRecord.type}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label='场景'>{detailRecord.scene}</Descriptions.Item>
            <Descriptions.Item label='关联单号'>{detailRecord.relatedNo || '-'}</Descriptions.Item>
            <Descriptions.Item label='接收人'>
              {RECEIVER_LABELS[detailRecord.receiverType] ?? detailRecord.receiverType}
            </Descriptions.Item>
            <Descriptions.Item label='状态'>
              {detailRecord.read ? <Tag color='default'>已读</Tag> : <Tag color='red'>未读</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label='发送时间'>
              {dayjs(detailRecord.createdAt).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
            <Descriptions.Item label='已读时间'>
              {detailRecord.readAt ? dayjs(detailRecord.readAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
            </Descriptions.Item>
          </Descriptions>
        ) : (
          <div style={{ textAlign: 'center', padding: 24 }}>未找到该消息</div>
        )}
      </Modal>
    </Card>
  );
};

export default NotificationMessages;
