import React from 'react';
import { Select, Input, DatePicker, Space, Button } from '@aquarush/admin-ui';
import type { Dayjs } from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import { TYPE_LABELS, RECEIVER_LABELS } from './constants';

const { RangePicker } = DatePicker;
const { Option } = Select;

const styles = stylex.create({
  filterBar: {
    marginBottom: '16px',
  },
  typeSelect: {
    width: '130px',
  },
  receiverSelect: {
    width: '120px',
  },
  keywordSearch: {
    width: '240px',
  },
  readSelect: {
    width: '110px',
  },
});

export interface NotificationFiltersState {
  messageType?: string;
  receiverType?: string;
  keyword?: string;
  read?: string; // 'READ' | 'UNREAD'
  range?: [Dayjs | null, Dayjs | null] | null;
}

export interface NotificationFiltersProps {
  filters: NotificationFiltersState;
  onChange: (partial: Partial<NotificationFiltersState>) => void;
  onSearch: () => void;
  onReset: () => void;
}

/** 消息列表筛选区(notifications/messages/index.tsx 拆分) */
const NotificationFilters: React.FC<NotificationFiltersProps> = ({
  filters,
  onChange,
  onSearch,
  onReset,
}) => (
  <Space wrap {...stylex.props(styles.filterBar)}>
    <Select
      placeholder='消息类型'
      allowClear
      {...stylex.props(styles.typeSelect)}
      value={filters.messageType}
      onChange={(v: unknown) => onChange({ messageType: v as string | undefined })}
    >
      {Object.entries(TYPE_LABELS).map(([value, info]) => (
        <Option key={value} value={value}>{info.label}</Option>
      ))}
    </Select>
    <Select
      placeholder='接收人'
      allowClear
      {...stylex.props(styles.receiverSelect)}
      value={filters.receiverType}
      onChange={(v: unknown) => onChange({ receiverType: v as string | undefined })}
    >
      {Object.entries(RECEIVER_LABELS).map(([value, label]) => (
        <Option key={value} value={value}>{label}</Option>
      ))}
    </Select>
    <Input.Search
      placeholder='关键字(关联单号/标题/摘要)'
      allowClear
      {...stylex.props(styles.keywordSearch)}
      value={filters.keyword}
      onChange={(e) => onChange({ keyword: e.target.value })}
      onSearch={onSearch}
      enterButton
    />
    <Select
      placeholder='已读状态'
      allowClear
      {...stylex.props(styles.readSelect)}
      value={filters.read}
      onChange={(v: unknown) => onChange({ read: v === 'READ' || v === 'UNREAD' ? v : undefined })}
    >
      <Option value='READ'>已读</Option>
      <Option value='UNREAD'>未读</Option>
    </Select>
    <RangePicker
      allowClear
      value={filters.range}
      onChange={(range) => onChange({ range })}
    />

    <Button type='primary' onClick={onSearch}>查询</Button>
    <Button onClick={onReset}>重置</Button>
  </Space>
);

export default NotificationFilters;
