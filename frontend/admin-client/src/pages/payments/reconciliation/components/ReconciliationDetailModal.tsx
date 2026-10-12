import React from 'react';
import { Button, Card, Descriptions, Modal, Select, Statistic, Table, Tag } from '@/ui';
import type { ColumnsType } from '@/ui';
import dayjs from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import { pagination } from '@/utils/table';
import { TASK_STATUS_MAP } from '../helpers';
import type {
  DiscrepancyType,
  ReconciliationTask,
} from '../../../../types/reconciliation';
import {
  DISCREPANCY_TYPE_OPTIONS,
  PAGE_SIZE,
  TASK_TYPE_MAP,
  TRIGGER_TYPE_TEXT,
  type DiscrepancyRow,
} from './constants';

// 静态内联样式迁 StyleX;#999 为页面局部灰阶,共享 token 无对应项,保留字面量
const styles = stylex.create({
  detailCard: {
    marginBottom: '16px',
  },
  sectionTitle: {
    fontWeight: 600,
    marginTop: '12px',
    marginRight: '0px',
    marginBottom: '8px',
    marginLeft: '0px',
  },
  statRow: {
    display: 'flex',
    columnGap: '16px',
    rowGap: '16px',
    marginBottom: '16px',
  },
  statCard: {
    flexGrow: 1,
  },
  detailHeaderRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: '12px',
    marginRight: '0px',
    marginBottom: '8px',
    marginLeft: '0px',
  },
  detailHeaderLabel: {
    fontWeight: 600,
  },
  footerNote: {
    marginTop: '8px',
    fontSize: '12px',
    color: '#999',
  },
  hint: {
    fontSize: '12px',
    color: '#999',
  },
  selectDiscrepancy: {
    width: '160px',
  },
});

export interface ReconciliationDetailModalProps {
  task: ReconciliationTask | null;
  columns: ColumnsType<DiscrepancyRow>;
  rows: DiscrepancyRow[];
  loading: boolean;
  total: number;
  page: number;
  discrepancyType: DiscrepancyType | undefined;
  onDiscrepancyTypeChange: (value: DiscrepancyType | DiscrepancyType[] | undefined) => void;
  onPageChange: (page: number) => void;
  onClose: () => void;
}

/** 任务详情弹窗:任务信息 + 三方校验汇总卡 + 差异列表(由 index.tsx 拆分) */
const ReconciliationDetailModal: React.FC<ReconciliationDetailModalProps> = ({
  task,
  columns,
  rows,
  loading,
  total,
  page,
  discrepancyType,
  onDiscrepancyTypeChange,
  onPageChange,
  onClose,
}) => (
  <Modal
    title={task ? `对账详情 · ${dayjs(task.taskDate).format('YYYY-MM-DD')} ${TASK_TYPE_MAP[task.taskType]?.text}` : '对账详情'}
    open={!!task}
    onCancel={onClose}
    footer={<Button onClick={onClose}>关闭</Button>}
    width={880}
  >
    {task && (
      <>
        <Descriptions
          size="small"
          column={2}
          bordered
          {...stylex.props(styles.detailCard)}
          items={[
            { key: 'date', label: '账单日 / 类型', children: `${dayjs(task.taskDate).format('YYYY-MM-DD')} · ${TASK_TYPE_MAP[task.taskType]?.text}` },
            {
              key: 'status',
              label: '任务状态',
              children: (
                <>
                  <Tag color={TASK_STATUS_MAP[task.status]?.color}>
                    {TASK_STATUS_MAP[task.status]?.text}
                  </Tag>
                  {task.status === 'SUCCESS' && task.unmatchedRecords > 0 && (
                    <span {...stylex.props(styles.hint)}>差异数 &gt; 0 仍为成功</span>
                  )}
                </>
              ),
            },
            {
              key: 'time',
              label: '执行时间',
              children: task.endTime ? dayjs(task.endTime).format('YYYY-MM-DD HH:mm:ss') : '-',
            },
            { key: 'trigger', label: '触发方式', children: TRIGGER_TYPE_TEXT[task.triggerType] ?? task.triggerType },
          ]}
        />

        <div {...stylex.props(styles.sectionTitle)}>三方校验汇总</div>
        <div {...stylex.props(styles.statRow)}>
          <Card size="small" {...stylex.props(styles.statCard)}>
            <Statistic title="微信账单汇总" value={Number(task.wechatTotalAmountCents) / 100} precision={2} prefix="¥" />
            <div {...stylex.props(styles.hint)}>{task.wechatTotalCount} 笔</div>
          </Card>
          <Card size="small" {...stylex.props(styles.statCard)}>
            <Statistic title="本地合计" value={Number(task.localTotalAmountCents) / 100} precision={2} prefix="¥" />
            <div {...stylex.props(styles.hint)}>{task.localTotalCount} 笔</div>
          </Card>
          <Card size="small" {...stylex.props(styles.statCard)}>
            <Statistic
              title="差异数"
              value={task.unmatchedRecords}
              valueStyle={{ color: task.unmatchedRecords > 0 ? '#cf1322' : '#389e0d' }}
            />
            <div {...stylex.props(styles.hint)}>匹配 {task.matchedRecords} / 总 {task.totalRecords} 笔</div>
          </Card>
        </div>

        <div {...stylex.props(styles.detailHeaderRow)}>
          <span {...stylex.props(styles.detailHeaderLabel)}>差异明细（{total}）</span>
          <Select
            allowClear
            placeholder="差异类型"
            {...stylex.props(styles.selectDiscrepancy)}
            value={discrepancyType}
            onChange={onDiscrepancyTypeChange}
            options={DISCREPANCY_TYPE_OPTIONS}
          />
        </div>
        <Table
          columns={columns}
          dataSource={rows}
          loading={loading}
          rowKey="id"
          size="small"
          scroll={{ x: 760 }}
          locale={{ emptyText: '无差异（全部匹配）' }}
          pagination={pagination(total, {
            current: page + 1,
            pageSize: PAGE_SIZE,
            onChange: (p) => onPageChange(p - 1),
          })}
        />
        <div {...stylex.props(styles.footerNote)}>
          资金安全底线：漏单（微信已收、本地无记录）与金额不一致必须显式暴露；执行与处置均记业务日志（操作人 / 任务 / 差异）。
        </div>
      </>
    )}
  </Modal>
);

export default ReconciliationDetailModal;
