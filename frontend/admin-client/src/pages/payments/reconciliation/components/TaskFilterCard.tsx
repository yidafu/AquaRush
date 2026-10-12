import React from 'react';
import { Button, Card, DatePicker, Select, Space } from '@/ui';
import type { Dayjs } from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import { TASK_STATUS_MAP } from '../helpers';
import type {
  ReconciliationTaskStatus,
  ReconciliationTaskType,
} from '../../../../types/reconciliation';
import { TASK_TYPE_MAP } from './constants';

const styles = stylex.create({
  filterCard: {
    marginBottom: '16px',
  },
  selectType: {
    width: '140px',
  },
  selectStatus: {
    width: '120px',
  },
});

export interface TaskFilterCardProps {
  taskType: ReconciliationTaskType | undefined;
  status: ReconciliationTaskStatus | undefined;
  dateRange: [Dayjs | null, Dayjs | null] | null;
  onTaskTypeChange: (value: ReconciliationTaskType | undefined) => void;
  onStatusChange: (value: ReconciliationTaskStatus | undefined) => void;
  onDateRangeChange: (value: [Dayjs | null, Dayjs | null] | null) => void;
  onReset: () => void;
}

/** 任务列表筛选卡:账单类型/账单日区间/任务状态/重置(由 index.tsx 拆分) */
const TaskFilterCard: React.FC<TaskFilterCardProps> = ({
  taskType,
  status,
  dateRange,
  onTaskTypeChange,
  onStatusChange,
  onDateRangeChange,
  onReset,
}) => (
  <Card {...stylex.props(styles.filterCard)}>
    <Space wrap>
      <Select
        allowClear
        placeholder="账单类型"
        {...stylex.props(styles.selectType)}
        value={taskType}
        onChange={(v) => {
          onTaskTypeChange(typeof v === 'string' ? (v as ReconciliationTaskType) : undefined);
        }}
        options={Object.entries(TASK_TYPE_MAP).map(([value, info]) => ({ value, label: info.text }))}
      />
      <DatePicker.RangePicker
        value={dateRange}
        onChange={(v) => {
          onDateRangeChange(v);
        }}
      />
      <Select
        allowClear
        placeholder="任务状态"
        {...stylex.props(styles.selectStatus)}
        value={status}
        onChange={(v) => {
          onStatusChange(typeof v === 'string' ? (v as ReconciliationTaskStatus) : undefined);
        }}
        options={Object.entries(TASK_STATUS_MAP).map(([value, info]) => ({ value, label: info.text }))}
      />
      <Button onClick={onReset}>重置</Button>
    </Space>
  </Card>
);

export default TaskFilterCard;
