import React from 'react';
import { Alert, Card, Table } from '@/ui';
import type { ColumnsType } from '@/ui';
import * as stylex from '@stylexjs/stylex';
import { pagination } from '@/utils/table';
import type { ReconciliationTask } from '../../../../types/reconciliation';
import { PAGE_SIZE } from './constants';

const styles = stylex.create({
  alertWrap: {
    marginTop: '16px',
  },
});

export interface TaskTableCardProps {
  columns: ColumnsType<ReconciliationTask>;
  dataSource: ReconciliationTask[];
  loading: boolean;
  total: number;
  page: number;
  onPageChange: (page: number) => void;
}

/** 任务列表表格卡 + 对账口径说明(由 index.tsx 拆分) */
const TaskTableCard: React.FC<TaskTableCardProps> = ({
  columns,
  dataSource,
  loading,
  total,
  page,
  onPageChange,
}) => (
  <Card>
    <Table
      columns={columns}
      dataSource={dataSource}
      loading={loading}
      rowKey="id"
      scroll={{ x: 1100 }}
      locale={{ emptyText: '暂无对账记录' }}
      pagination={pagination(total, {
        current: page + 1,
        pageSize: PAGE_SIZE,
        onChange: (p) => onPageChange(p - 1),
      })}
    />
    <div {...stylex.props(styles.alertWrap)}>
      <Alert
        type="info"
        showIcon={false}
        message="对账口径（R-PAY-04）"
        description="以微信支付官方账单（T-1 交易账单 + 退款账单）为基准，与本地支付记录和退款记录逐笔比对，产出「短款（微信有本地无）/ 长款（本地有微信无）/ 金额不一致」差异；差异须人工处置（解决 / 忽略并留备注）后闭环，不允许静默吞掉。差异数 > 0 的任务状态仍为「成功」——对账完成 ≠ 无差异。与送水工收款日报对账（F-COL-011）相互独立。"
      />
    </div>
  </Card>
);

export default TaskTableCard;
