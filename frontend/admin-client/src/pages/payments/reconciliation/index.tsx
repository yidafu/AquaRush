import React, { useCallback, useMemo, useState } from 'react';
import {
  Form,
  Modal,
  message,
} from '@aquarush/admin-ui';
import { useQuery, useMutation } from '@apollo/client';
import dayjs, { type Dayjs } from 'dayjs';
import {
  RECONCILIATION_TASKS_PAGE_QUERY,
  RECONCILIATION_TASKS_PAGE,
  RECONCILIATION_DETAIL_QUERY,
  RECONCILIATION_DETAIL,
} from '../../../graphql/queries/reconciliation.graphql';
import {
  EXECUTE_RECONCILIATION_MUTATION,
  RESOLVE_DISCREPANCY_MUTATION,
} from '../../../graphql/mutations/reconciliation.graphql';
import type {
  DiscrepancyType,
  ReconciliationDiscrepancy,
  ReconciliationReport,
  ReconciliationTask,
  ReconciliationTaskPage,
  ReconciliationTaskStatus,
  ReconciliationTaskType,
} from '../../../types/reconciliation';
import {
  extractDiscrepancyAmounts,
  isWithinReconciliationWindow,
} from './helpers';
import PageHeader from './components/PageHeader';
import TaskFilterCard from './components/TaskFilterCard';
import TaskTableCard from './components/TaskTableCard';
import ReconciliationDetailModal from './components/ReconciliationDetailModal';
import ResolveDiscrepancyModal from './components/ResolveDiscrepancyModal';
import ExecuteManualModal from './components/ExecuteManualModal';
import { buildTaskColumns } from './components/taskColumns';
import { buildDiscrepancyColumns } from './components/discrepancyColumns';
import { PAGE_SIZE, type DiscrepancyRow } from './components/constants';

const PaymentReconciliationPage: React.FC = () => {
  // 无 PAYMENT_RECONCILIATION_EXECUTE 权限的账号(送水工)不展示重跑/处置入口;
  // 前端菜单隐藏为 UX 层,后端以 @PreAuthorize 强制(R-AUTH-04)
  const canExecute = useMemo(() => {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    return userInfo.role === 'SUPER_ADMIN' || userInfo.role === 'ADMIN';
  }, []);

  // 筛选状态(服务端分页 + 过滤,规则 #10)
  const [taskType, setTaskType] = useState<ReconciliationTaskType | undefined>(undefined);
  const [status, setStatus] = useState<ReconciliationTaskStatus | undefined>(undefined);
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [page, setPage] = useState(0);

  // 详情弹窗(单请求合并报表 + 差异首页,规则 #7)
  const [detailTask, setDetailTask] = useState<ReconciliationTask | null>(null);
  const [detailPage, setDetailPage] = useState(0);
  // 差异类型筛选(详情弹窗内)
  const [discrepancyType, setDiscrepancyType] = useState<DiscrepancyType | undefined>(undefined);
  // 处置弹窗(解决/忽略 + 备注必填)
  const [resolveTarget, setResolveTarget] = useState<DiscrepancyRow | null>(null);
  const [resolveAction, setResolveAction] = useState<'RESOLVE' | 'IGNORE'>('RESOLVE');
  const [resolveForm] = Form.useForm();
  // 手动对账弹窗
  const [executeOpen, setExecuteOpen] = useState(false);
  const [executeForm] = Form.useForm();

  // 单页单请求:任务列表一次取回(规则 #7)
  const { data, loading } = useQuery<{ reconciliationTasks: ReconciliationTaskPage }>(
    RECONCILIATION_TASKS_PAGE_QUERY,
    {
      variables: {
        taskType: taskType ?? null,
        status: status ?? null,
        dateFrom: dateRange?.[0] ? dateRange[0].format('YYYY-MM-DD') : null,
        dateTo: dateRange?.[1] ? dateRange[1].format('YYYY-MM-DD') : null,
        page,
        size: PAGE_SIZE,
      },
      fetchPolicy: 'cache-and-network',
    },
  );

  // 详情弹窗数据:一次请求同时取「报表 + 差异分页」(合并请求,不链式串行)
  const {
    data: detailData,
    loading: detailLoading,
    refetch: refetchDetail,
  } = useQuery<{
    reconciliationReports: ReconciliationReport[];
    reconciliationDiscrepancies: {
      content: ReconciliationDiscrepancy[];
      totalElements: number;
      totalPages: number;
    };
  }>(RECONCILIATION_DETAIL_QUERY, {
    variables: { taskId: detailTask?.taskId, page: detailPage, size: PAGE_SIZE },
    skip: !detailTask,
    fetchPolicy: 'cache-and-network',
  });

  const [executeReconciliation, { loading: executing }] = useMutation(EXECUTE_RECONCILIATION_MUTATION, {
    refetchQueries: [RECONCILIATION_TASKS_PAGE],
    onCompleted: (res) => {
      message.success(`对账任务已创建(${res.executeReconciliation.taskId}),异步执行中`);
      setExecuteOpen(false);
      executeForm.resetFields();
    },
    onError: (err) => message.error('执行对账失败: ' + err.message),
  });

  const [resolveDiscrepancy, { loading: resolving }] = useMutation(RESOLVE_DISCREPANCY_MUTATION, {
    refetchQueries: [RECONCILIATION_TASKS_PAGE, RECONCILIATION_DETAIL],
    onCompleted: () => {
      message.success(resolveAction === 'RESOLVE' ? '差异已标记解决' : '差异已忽略');
      setResolveTarget(null);
      resolveForm.resetFields();
    },
    onError: (err) => message.error('差异处置失败: ' + err.message),
  });

  const openDetail = useCallback((task: ReconciliationTask) => {
    setDetailTask(task);
    setDetailPage(0);
    setDiscrepancyType(undefined);
  }, []);

  /** 重跑:同账单日任务幂等重算(已处置差异保留留痕),二次确认 */
  const handleRerun = useCallback(
    (task: ReconciliationTask) => {
      const billDate = dayjs(task.taskDate).format('YYYY-MM-DD');
      Modal.confirm({
        title: '重跑对账',
        content: '重跑将重算该日对账结果，已处理的差异记录保留，确认继续？',
        okText: '确认重跑',
        cancelText: '取消',
        onOk: () =>
          executeReconciliation({
            variables: { input: { taskType: task.taskType, reconciliationDate: billDate } },
          }),
      });
    },
    [executeReconciliation],
  );

  /** 手动对账提交(账单日限 T-1~T-30,前端提示 + 后端强制) */
  const handleExecuteSubmit = useCallback(async () => {
    const values = await executeForm.validateFields();
    const billDate = (values.reconciliationDate as Dayjs).format('YYYY-MM-DD');
    if (!isWithinReconciliationWindow(billDate)) {
      message.error('仅支持近 30 天账单');
      return;
    }
    executeReconciliation({
      variables: { input: { taskType: values.taskType as ReconciliationTaskType, reconciliationDate: billDate } },
    });
  }, [executeForm, executeReconciliation]);

  /** 差异处置提交(备注必填「请填写处理说明」) */
  const handleResolveSubmit = useCallback(async () => {
    if (!resolveTarget) return;
    const values = await resolveForm.validateFields();
    resolveDiscrepancy({
      variables: {
        input: {
          discrepancyId: resolveTarget.id,
          action: resolveAction,
          resolutionNotes: values.resolutionNotes as string,
        },
      },
    });
  }, [resolveAction, resolveForm, resolveTarget, resolveDiscrepancy]);

  /** 详情弹窗差异行:金额提取 + 类型筛选(服务端经 refetchDetail 过滤) */
  const discrepancyRows: DiscrepancyRow[] = useMemo(
    () =>
      (detailData?.reconciliationDiscrepancies.content ?? []).map((d) => ({
        ...d,
        ...extractDiscrepancyAmounts(d.recordDetails),
      })),
    [detailData],
  );

  const handleDetailFilterChange = useCallback(
    (value: DiscrepancyType | DiscrepancyType[] | undefined) => {
      const next = typeof value === 'string' ? value : undefined;
      setDiscrepancyType(next);
      setDetailPage(0);
      refetchDetail({ taskId: detailTask?.taskId, page: 0, size: PAGE_SIZE, discrepancyType: next ?? null });
    },
    [detailTask, refetchDetail],
  );

  const columns = buildTaskColumns({
    canExecute,
    onOpenDetail: openDetail,
    onRerun: handleRerun,
  });

  const discrepancyColumns = buildDiscrepancyColumns({
    canExecute,
    onResolve: (record) => {
      setResolveTarget(record);
      setResolveAction('RESOLVE');
      resolveForm.resetFields();
    },
  });

  return (
    <div>
      <PageHeader
        canExecute={canExecute}
        onManualExecute={() => {
          executeForm.resetFields();
          setExecuteOpen(true);
        }}
      />

      <TaskFilterCard
        taskType={taskType}
        status={status}
        dateRange={dateRange}
        onTaskTypeChange={(v) => {
          setTaskType(v);
          setPage(0);
        }}
        onStatusChange={(v) => {
          setStatus(v);
          setPage(0);
        }}
        onDateRangeChange={(v) => {
          setDateRange(v);
          setPage(0);
        }}
        onReset={() => {
          setTaskType(undefined);
          setStatus(undefined);
          setDateRange(null);
          setPage(0);
        }}
      />

      <TaskTableCard
        columns={columns}
        dataSource={data?.reconciliationTasks.content ?? []}
        loading={loading}
        total={data?.reconciliationTasks.totalElements ?? 0}
        page={page}
        onPageChange={setPage}
      />

      {/* 任务详情弹窗:任务信息 + 三方校验汇总卡 + 差异列表 */}
      <ReconciliationDetailModal
        task={detailTask}
        columns={discrepancyColumns}
        rows={discrepancyRows}
        loading={detailLoading}
        total={detailData?.reconciliationDiscrepancies.totalElements ?? 0}
        page={detailPage}
        discrepancyType={discrepancyType}
        onDiscrepancyTypeChange={handleDetailFilterChange}
        onPageChange={setDetailPage}
        onClose={() => setDetailTask(null)}
      />

      {/* 差异处置弹窗:解决/忽略 + 备注必填 */}
      <ResolveDiscrepancyModal
        target={resolveTarget}
        action={resolveAction}
        form={resolveForm}
        resolving={resolving}
        onActionChange={setResolveAction}
        onCancel={() => setResolveTarget(null)}
        onOk={handleResolveSubmit}
      />

      {/* 手动对账弹窗:类型 + 账单日(T-1~T-30) */}
      <ExecuteManualModal
        open={executeOpen}
        form={executeForm}
        executing={executing}
        onCancel={() => setExecuteOpen(false)}
        onOk={handleExecuteSubmit}
      />
    </div>
  );
};

export default PaymentReconciliationPage;
