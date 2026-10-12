import { Button, Space, Tag } from '@/ui';
import type { ColumnsType } from '@/ui';
import dayjs from 'dayjs';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { TASK_STATUS_MAP } from '../helpers';
import type {
  ReconciliationTask,
  ReconciliationTaskStatus,
  ReconciliationTaskType,
} from '../../../../types/reconciliation';
import { TASK_TYPE_MAP } from './constants';

// 静态内联样式迁 StyleX;#999 为页面局部灰阶,共享 token 无对应项,保留字面量
const styles = stylex.create({
  cellStrong: {
    fontWeight: 600,
  },
  hint: {
    fontSize: '12px',
    color: '#999',
  },
});

export interface TaskColumnsOptions {
  canExecute: boolean;
  onOpenDetail: (task: ReconciliationTask) => void;
  onRerun: (task: ReconciliationTask) => void;
}

/** 对账任务列表列定义(由 index.tsx 拆分:账单日/类型/状态/汇总/操作) */
export function buildTaskColumns(options: TaskColumnsOptions): ColumnsType<ReconciliationTask> {
  const { canExecute, onOpenDetail, onRerun } = options;
  return [
    {
      title: '账单日',
      dataIndex: 'taskDate',
      key: 'taskDate',
      width: 110,
      render: (t: string) => (
        <span {...stylex.props(styles.cellStrong)}>{dayjs(t).format('YYYY-MM-DD')}</span>
      ),
    },
    {
      title: '类型',
      dataIndex: 'taskType',
      key: 'taskType',
      width: 100,
      render: (t: ReconciliationTaskType) => (
        <Tag color={TASK_TYPE_MAP[t]?.color}>{TASK_TYPE_MAP[t]?.text ?? t}</Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (s: ReconciliationTaskStatus) => {
        const info = TASK_STATUS_MAP[s] ?? { text: s, color: 'default' };
        return (
          <>
            <Tag color={info.color}>{info.text}</Tag>
            {s === 'SKIPPED' && (
              <div {...stylex.props(styles.hint)}>{'未配置商户参数'}</div>
            )}
          </>
        );
      },
    },
    {
      title: '微信总笔数',
      dataIndex: 'wechatTotalCount',
      key: 'wechatTotalCount',
      align: 'right',
      width: 110,
    },
    {
      title: '微信总金额',
      dataIndex: 'wechatTotalAmountCents',
      key: 'wechatTotalAmountCents',
      align: 'right',
      width: 120,
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '本地总金额',
      dataIndex: 'localTotalAmountCents',
      key: 'localTotalAmountCents',
      align: 'right',
      width: 120,
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '差异数',
      dataIndex: 'unmatchedRecords',
      key: 'unmatchedRecords',
      align: 'right',
      width: 90,
      // 差异数语义红强调(对账完成 ≠ 无差异);动态色依赖 n,保留 style
      render: (n: number) => (
        <span
          style={{ fontWeight: 600, color: n > 0 ? '#cf1322' : '#389e0d' }}
        >
          {n}
        </span>
      ),
    },
    {
      title: '执行时间',
      key: 'executedAt',
      width: 170,
      render: (_, record) =>
        record.endTime ? dayjs(record.endTime).format('MM-DD HH:mm:ss') : '-',
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 120,
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" onClick={() => onOpenDetail(record)}>
            详情
          </Button>
          {canExecute && record.status !== 'RUNNING' && record.status !== 'PENDING' && (
            <Button type="link" size="small" onClick={() => onRerun(record)}>
              重跑
            </Button>
          )}
        </Space>
      ),
    },
  ];
}
