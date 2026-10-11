import React, { useCallback, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Descriptions,
  Form,
  Input,
  Modal,
  Radio,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation } from '@apollo/client';
import dayjs, { type Dayjs } from 'dayjs';
import { displayCents } from '@aquarush/common';
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
  DiscrepancyStatus,
  DiscrepancyType,
  ReconciliationDiscrepancy,
  ReconciliationReport,
  ReconciliationTask,
  ReconciliationTaskPage,
  ReconciliationTaskStatus,
  ReconciliationTaskType,
  SourceSystem,
} from '../../../types/reconciliation';
import {
  DISCREPANCY_STATUS_MAP,
  TASK_STATUS_MAP,
  discrepancyLabel,
  extractDiscrepancyAmounts,
  isWithinReconciliationWindow,
} from './helpers';
import { pagination } from '@/utils/table';

const PAGE_SIZE = 10;

/** 任务类型 → 标签文案(设计稿:支付账单/退款账单/结算账单) */
const TASK_TYPE_MAP: Record<ReconciliationTaskType, { text: string; color: string }> = {
  PAYMENT: { text: '支付账单', color: 'blue' },
  REFUND: { text: '退款账单', color: 'purple' },
  SETTLEMENT: { text: '结算账单', color: 'geekblue' },
};

/** 触发方式文案(F-ORD-031 详情「触发方式」) */
const TRIGGER_TYPE_TEXT: Record<string, string> = {
  SCHEDULED: '每日调度(02:00/02:05)',
  MANUAL: '管理员手动',
};

/** 差异类型筛选选项 */
const DISCREPANCY_TYPE_OPTIONS: { value: DiscrepancyType; label: string }[] = [
  { value: 'MISSING', label: '缺失(短款/长款)' },
  { value: 'MISMATCH', label: '金额不一致' },
  { value: 'EXTRA', label: '多余记录' },
];

interface DiscrepancyRow extends ReconciliationDiscrepancy {
  wechatCents: number | null;
  localCents: number | null;
}

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
    (value?: DiscrepancyType) => {
      setDiscrepancyType(value);
      setDetailPage(0);
      refetchDetail({ taskId: detailTask?.taskId, page: 0, size: PAGE_SIZE, discrepancyType: value ?? null });
    },
    [detailTask, refetchDetail],
  );

  const columns: ColumnsType<ReconciliationTask> = [
    {
      title: '账单日',
      dataIndex: 'taskDate',
      key: 'taskDate',
      width: 110,
      render: (t: string) => <span style={{ fontWeight: 600 }}>{dayjs(t).format('YYYY-MM-DD')}</span>,
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
              <div style={{ fontSize: 12, color: '#999' }}>{'未配置商户参数'}</div>
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
      // 差异数语义红强调(对账完成 ≠ 无差异)
      render: (n: number) => (
        <span style={{ fontWeight: 600, color: n > 0 ? '#cf1322' : '#389e0d' }}>{n}</span>
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
          <Button type="link" size="small" onClick={() => openDetail(record)}>
            详情
          </Button>
          {canExecute && record.status !== 'RUNNING' && record.status !== 'PENDING' && (
            <Button type="link" size="small" onClick={() => handleRerun(record)}>
              重跑
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const discrepancyColumns: ColumnsType<DiscrepancyRow> = [
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
            {label.description && <div style={{ fontSize: 12, color: '#999' }}>{label.description}</div>}
          </>
        );
      },
    },
    {
      title: '记录标识',
      dataIndex: 'recordId',
      key: 'recordId',
      ellipsis: true,
      render: (v: string) => <span style={{ fontFamily: 'monospace' }}>{v}</span>,
    },
    {
      title: '微信金额',
      key: 'wechatCents',
      align: 'right',
      width: 110,
      render: (_, record) =>
        record.wechatCents != null ? (
          <span style={{ color: '#cf1322', fontWeight: 600 }}>{displayCents(record.wechatCents)}</span>
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
              setResolveTarget(record);
              setResolveAction('RESOLVE');
              resolveForm.resetFields();
            }}
          >
            处理
          </Button>
        ) : null,
    },
  ];

  return (
    <div>
      <h1 className="page-title" style={{ marginBottom: 8 }}>
        微信支付对账
      </h1>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{ color: '#666' }}>
          每日凌晨自动对账（支付 02:00 / 退款 02:05）｜ 以微信官方 T-1 账单为基准逐笔比对 ｜ 差异人工处置留痕（R-PAY-04）
        </div>
        {canExecute && (
          <Button
            type="primary"
            onClick={() => {
              executeForm.resetFields();
              setExecuteOpen(true);
            }}
          >
            手动对账
          </Button>
        )}
      </div>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            allowClear
            placeholder="账单类型"
            style={{ width: 140 }}
            value={taskType}
            onChange={(v) => {
              setTaskType(v);
              setPage(0);
            }}
            options={Object.entries(TASK_TYPE_MAP).map(([value, info]) => ({ value, label: info.text }))}
          />
          <DatePicker.RangePicker
            value={dateRange}
            onChange={(v) => {
              setDateRange(v);
              setPage(0);
            }}
          />
          <Select
            allowClear
            placeholder="任务状态"
            style={{ width: 120 }}
            value={status}
            onChange={(v) => {
              setStatus(v);
              setPage(0);
            }}
            options={Object.entries(TASK_STATUS_MAP).map(([value, info]) => ({ value, label: info.text }))}
          />
          <Button
            onClick={() => {
              setTaskType(undefined);
              setStatus(undefined);
              setDateRange(null);
              setPage(0);
            }}
          >
            重置
          </Button>
        </Space>
      </Card>

      <Card>
        <Table
          columns={columns}
          dataSource={data?.reconciliationTasks.content}
          loading={loading}
          rowKey="id"
          scroll={{ x: 1100 }}
          locale={{ emptyText: '暂无对账记录' }}
          pagination={pagination(data?.reconciliationTasks.totalElements ?? 0, {
            current: page + 1,
            pageSize: PAGE_SIZE,
            onChange: (p) => setPage(p - 1),
          })}
        />
        <div style={{ marginTop: 16 }}>
          <Alert
            type="info"
            showIcon={false}
            message="对账口径（R-PAY-04）"
            description="以微信支付官方账单（T-1 交易账单 + 退款账单）为基准，与本地支付记录和退款记录逐笔比对，产出「短款（微信有本地无）/ 长款（本地有微信无）/ 金额不一致」差异；差异须人工处置（解决 / 忽略并留备注）后闭环，不允许静默吞掉。差异数 > 0 的任务状态仍为「成功」——对账完成 ≠ 无差异。与送水工收款日报对账（F-COL-011）相互独立。"
          />
        </div>
      </Card>

      {/* 任务详情弹窗:任务信息 + 三方校验汇总卡 + 差异列表 */}
      <Modal
        title={detailTask ? `对账详情 · ${dayjs(detailTask.taskDate).format('YYYY-MM-DD')} ${TASK_TYPE_MAP[detailTask.taskType]?.text}` : '对账详情'}
        open={!!detailTask}
        onCancel={() => setDetailTask(null)}
        footer={<Button onClick={() => setDetailTask(null)}>关闭</Button>}
        width={880}
      >
        {detailTask && (
          <>
            <Descriptions
              size="small"
              column={2}
              bordered
              style={{ marginBottom: 16 }}
              items={[
                { key: 'date', label: '账单日 / 类型', children: `${dayjs(detailTask.taskDate).format('YYYY-MM-DD')} · ${TASK_TYPE_MAP[detailTask.taskType]?.text}` },
                {
                  key: 'status',
                  label: '任务状态',
                  children: (
                    <>
                      <Tag color={TASK_STATUS_MAP[detailTask.status]?.color}>
                        {TASK_STATUS_MAP[detailTask.status]?.text}
                      </Tag>
                      {detailTask.status === 'SUCCESS' && detailTask.unmatchedRecords > 0 && (
                        <span style={{ fontSize: 12, color: '#999' }}>差异数 &gt; 0 仍为成功</span>
                      )}
                    </>
                  ),
                },
                {
                  key: 'time',
                  label: '执行时间',
                  children: detailTask.endTime ? dayjs(detailTask.endTime).format('YYYY-MM-DD HH:mm:ss') : '-',
                },
                { key: 'trigger', label: '触发方式', children: TRIGGER_TYPE_TEXT[detailTask.triggerType] ?? detailTask.triggerType },
              ]}
            />

            <div style={{ fontWeight: 600, margin: '12px 0 8px' }}>三方校验汇总</div>
            <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
              <Card size="small" style={{ flex: 1 }}>
                <Statistic title="微信账单汇总" value={Number(detailTask.wechatTotalAmountCents) / 100} precision={2} prefix="¥" />
                <div style={{ fontSize: 12, color: '#999' }}>{detailTask.wechatTotalCount} 笔</div>
              </Card>
              <Card size="small" style={{ flex: 1 }}>
                <Statistic title="本地合计" value={Number(detailTask.localTotalAmountCents) / 100} precision={2} prefix="¥" />
                <div style={{ fontSize: 12, color: '#999' }}>{detailTask.localTotalCount} 笔</div>
              </Card>
              <Card size="small" style={{ flex: 1 }}>
                <Statistic
                  title="差异数"
                  value={detailTask.unmatchedRecords}
                  valueStyle={{ color: detailTask.unmatchedRecords > 0 ? '#cf1322' : '#389e0d' }}
                />
                <div style={{ fontSize: 12, color: '#999' }}>匹配 {detailTask.matchedRecords} / 总 {detailTask.totalRecords} 笔</div>
              </Card>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '12px 0 8px' }}>
              <span style={{ fontWeight: 600 }}>差异明细（{detailData?.reconciliationDiscrepancies.totalElements ?? 0}）</span>
              <Select
                allowClear
                placeholder="差异类型"
                style={{ width: 160 }}
                value={discrepancyType}
                onChange={handleDetailFilterChange}
                options={DISCREPANCY_TYPE_OPTIONS}
              />
            </div>
            <Table
              columns={discrepancyColumns}
              dataSource={discrepancyRows}
              loading={detailLoading}
              rowKey="id"
              size="small"
              scroll={{ x: 760 }}
              locale={{ emptyText: '无差异（全部匹配）' }}
              pagination={pagination(detailData?.reconciliationDiscrepancies.totalElements ?? 0, {
                current: detailPage + 1,
                pageSize: PAGE_SIZE,
                onChange: (p) => setDetailPage(p - 1),
              })}
            />
            <div style={{ marginTop: 8, fontSize: 12, color: '#999' }}>
              资金安全底线：漏单（微信已收、本地无记录）与金额不一致必须显式暴露；执行与处置均记业务日志（操作人 / 任务 / 差异）。
            </div>
          </>
        )}
      </Modal>

      {/* 差异处置弹窗:解决/忽略 + 备注必填 */}
      <Modal
        title={resolveTarget ? `处置差异 · ${discrepancyLabel(resolveTarget.discrepancyType, resolveTarget.sourceSystem as SourceSystem).text} ${resolveTarget.recordId}` : '处置差异'}
        open={!!resolveTarget}
        onCancel={() => setResolveTarget(null)}
        onOk={handleResolveSubmit}
        okText="提交处置"
        cancelText="取消"
        confirmLoading={resolving}
      >
        {resolveTarget && (
          <div style={{ marginBottom: 12, color: '#666', fontSize: 13 }}>
            记录标识: {resolveTarget.recordId}
            {resolveTarget.wechatCents != null && (
              <>
                {' ｜ '}微信金额: <span style={{ color: '#cf1322' }}>{displayCents(resolveTarget.wechatCents)}</span>
              </>
            )}
            {resolveTarget.localCents != null && <> ｜ 本地金额: {displayCents(resolveTarget.localCents)}</>}
          </div>
        )}
        <Form form={resolveForm} layout="vertical">
          <Form.Item label="处置结果" required>
            <Radio.Group
              value={resolveAction}
              onChange={(e) => setResolveAction(e.target.value as 'RESOLVE' | 'IGNORE')}
            >
              <Radio value="RESOLVE">解决（已核实无资金损失或已修复）</Radio>
              <Radio value="IGNORE">忽略（跨日边界等可解释差异）</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item
            name="resolutionNotes"
            label="处理说明（备注必填）"
            rules={[{ required: true, message: '请填写处理说明' }]}
          >
            <Input.TextArea rows={3} maxLength={500} placeholder="请填写处理说明" />
          </Form.Item>
          <div style={{ fontSize: 12, color: '#999' }}>
            处置留痕可审计；重跑幂等保留已处置记录；已处置差异不可再改。
          </div>
        </Form>
      </Modal>

      {/* 手动对账弹窗:类型 + 账单日(T-1~T-30) */}
      <Modal
        title="手动对账"
        open={executeOpen}
        onCancel={() => setExecuteOpen(false)}
        onOk={handleExecuteSubmit}
        okText="开始对账"
        cancelText="取消"
        confirmLoading={executing}
      >
        <Form form={executeForm} layout="vertical" initialValues={{ taskType: 'PAYMENT' }}>
          <Form.Item name="taskType" label="账单类型" rules={[{ required: true }]}>
            <Radio.Group>
              <Radio value="PAYMENT">支付账单</Radio>
              <Radio value="REFUND">退款账单</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item name="reconciliationDate" label="账单日" rules={[{ required: true, message: '请选择账单日' }]}>
            <DatePicker
              style={{ width: '100%' }}
              disabledDate={(d) => {
                const day = d.endOf('day');
                return day.isAfter(dayjs().subtract(1, 'day')) || d.isBefore(dayjs().subtract(30, 'day'), 'day');
              }}
            />
          </Form.Item>
          <div style={{ fontSize: 12, color: '#999' }}>仅支持近 30 天账单（T-1 ~ T-30）；同账单日重跑幂等，已处置差异保留。</div>
        </Form>
      </Modal>
    </div>
  );
};

export default PaymentReconciliationPage;
