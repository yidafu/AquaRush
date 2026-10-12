import { Button, Tag } from '@/ui';
import type { ColumnsType } from '@/ui';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { DISCREPANCY_STATUS_MAP, discrepancyLabel } from '../helpers';
import type { DiscrepancyStatus, DiscrepancyType } from '../../../../types/reconciliation';
import type { DiscrepancyRow } from './constants';

// 静态内联样式迁 StyleX;#999 为页面局部灰阶,共享 token 无对应项,保留字面量
const styles = stylex.create({
  hint: {
    fontSize: '12px',
    color: '#999',
  },
  mono: {
    fontFamily: 'monospace',
  },
  errorAmount: {
    fontWeight: 600,
    color: tokens.statusErrorFg,
  },
});

export interface DiscrepancyColumnsOptions {
  canExecute: boolean;
  onResolve: (record: DiscrepancyRow) => void;
}

/** 差异明细列定义(由 index.tsx 拆分:类型/标识/金额/处置状态/操作) */
export function buildDiscrepancyColumns(
  options: DiscrepancyColumnsOptions,
): ColumnsType<DiscrepancyRow> {
  const { canExecute, onResolve } = options;
  return [
    {
      title: '差异类型',
      dataIndex: 'discrepancyType',
      key: 'discrepancyType',
      width: 130,
      render: (t: DiscrepancyType, record) => {
        const label = discrepancyLabel(t, record.sourceSystem);
        return (
          <>
            <Tag color="error">{label.text}</Tag>
            {label.description && <div {...stylex.props(styles.hint)}>{label.description}</div>}
          </>
        );
      },
    },
    {
      title: '记录标识',
      dataIndex: 'recordId',
      key: 'recordId',
      ellipsis: true,
      render: (v: string) => <span {...stylex.props(styles.mono)}>{v}</span>,
    },
    {
      title: '微信金额',
      key: 'wechatCents',
      align: 'right',
      width: 110,
      render: (_, record) =>
        record.wechatCents != null ? (
          <span {...stylex.props(styles.errorAmount)}>{displayCents(record.wechatCents)}</span>
        ) : (
          '—'
        ),
    },
    {
      title: '本地金额',
      key: 'localCents',
      align: 'right',
      width: 110,
      render: (_, record) =>
        record.localCents != null ? displayCents(record.localCents) : '—',
    },
    {
      title: '处理状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: DiscrepancyStatus) => {
        const info = DISCREPANCY_STATUS_MAP[s] ?? { text: s, color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '处理说明',
      dataIndex: 'resolutionNotes',
      key: 'resolutionNotes',
      ellipsis: true,
      render: (note?: string | null) => note || '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_, record) =>
        canExecute && record.status === 'UNRESOLVED' ? (
          <Button
            type="link"
            size="small"
            onClick={() => {
              onResolve(record);
            }}
          >
            处理
          </Button>
        ) : null,
    },
  ];
}
