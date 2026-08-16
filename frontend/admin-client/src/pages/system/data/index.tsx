import React, { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Divider,
  Form,
  InputNumber,
  message,
  Modal,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { LogBackupStatus, LogTableType, SystemSettingKeys } from '@aquarush/common';
import type { DataMaintenanceOverviewVo, LogBackupRecord, LogPartitionVo } from '@aquarush/common';
import { useBackupLogPartition, useDataMaintenanceOverview, useSaveSystemConfigs } from '../../../services/system-graphql';

// 数据维护页(F-SYS-006):日志保留策略 + 日志分区管理(按月分区,离线备份 = 转储冷表 + 导出文件)
// 仅超级管理员可读写(SYSTEM_CONFIG_READ/WRITE,R-SYS-03);超期分区可手动备份,每日 03:30 自动备份(log_cleanup_enabled 关闭时跳过)。

interface DataMaintenanceValues {
  apiLogRetentionDays?: number;
  userActionLogRetentionDays?: number;
  businessLogRetentionDays?: number;
  cleanupEnabled?: boolean;
}

// 键 → 表单字段映射(种子默认值兜底:90/90/90/true)
const FIELD_KEYS: Record<keyof DataMaintenanceValues, string> = {
  apiLogRetentionDays: SystemSettingKeys.LOG_RETENTION_API_DAYS,
  userActionLogRetentionDays: SystemSettingKeys.LOG_RETENTION_USER_ACTION_DAYS,
  businessLogRetentionDays: SystemSettingKeys.LOG_RETENTION_BUSINESS_DAYS,
  cleanupEnabled: SystemSettingKeys.LOG_CLEANUP_ENABLED,
};

const DEFAULTS: Record<keyof DataMaintenanceValues, number | boolean> = {
  apiLogRetentionDays: 90,
  userActionLogRetentionDays: 90,
  businessLogRetentionDays: 90,
  cleanupEnabled: true,
};

const TABLE_TABS: { key: LogTableType; label: string }[] = [
  { key: LogTableType.API_LOG, label: 'API 日志' },
  { key: LogTableType.USER_ACTION_LOG, label: '用户操作日志' },
  { key: LogTableType.BUSINESS_LOG, label: '业务日志' },
];

const LOG_TABLE_LABEL: Record<LogTableType, string> = {
  [LogTableType.API_LOG]: 'API 日志',
  [LogTableType.USER_ACTION_LOG]: '用户操作日志',
  [LogTableType.BUSINESS_LOG]: '业务日志',
};

const BACKUP_STATUS_CONFIG: Record<LogBackupStatus, { label: string; color: string }> = {
  [LogBackupStatus.RUNNING]: { label: '进行中', color: 'blue' },
  [LogBackupStatus.SUCCEEDED]: { label: '成功', color: 'green' },
  [LogBackupStatus.FAILED]: { label: '失败', color: 'red' },
};

const formatBytes = (bytes?: number | null): string => {
  if (bytes == null) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
};

const formatDateTime = (date?: string | null): string => (date ? new Date(date).toLocaleString('zh-CN') : '-');

const SystemDataMaintenanceConfig: React.FC = () => {
  const [form] = Form.useForm();
  const [loadOverview, { data, loading }] = useDataMaintenanceOverview();
  const [saveConfigs, { loading: saving }] = useSaveSystemConfigs();
  const [backupPartition, { loading: backingUp }] = useBackupLogPartition();
  const [canWrite] = useState(() => JSON.parse(localStorage.getItem('userInfo') || '{}').role === 'SUPER_ADMIN');
  const [activeTable, setActiveTable] = useState<LogTableType>(LogTableType.API_LOG);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [detail, setDetail] = useState<LogBackupRecord | null>(null);

  const overview = data?.dataMaintenanceOverview as DataMaintenanceOverviewVo | undefined;

  // 单请求加载总览(遵守「单页面单 GraphQL 请求」规则 10)
  useEffect(() => {
    loadOverview({ variables: { input: { page, size: pageSize } } });
  }, [loadOverview, page, pageSize]);

  // 回显:键 → 表单字段(未配置时按种子默认值兜底)
  useEffect(() => {
    const settings = overview?.settings || [];
    if (settings.length === 0) return;
    const byKey = new Map(settings.map((c) => [c.settingKey, c]));
    const values: Partial<DataMaintenanceValues> = {};
    (Object.keys(FIELD_KEYS) as (keyof DataMaintenanceValues)[]).forEach((field) => {
      const raw = byKey.get(FIELD_KEYS[field])?.settingValue;
      // 经 Record<string, unknown> 写入,避免 indexed-access 赋值被 `never` 分支污染类型
      const target = values as Record<string, unknown>;
      if (raw == null) {
        target[field] = DEFAULTS[field];
      } else if (typeof DEFAULTS[field] === 'boolean') {
        target[field] = raw === 'true';
      } else {
        target[field] = Number(raw);
      }
    });
    form.setFieldsValue(values);
  }, [overview, form]);

  const handleSave = async () => {
    const values = (await form.validateFields()) as DataMaintenanceValues;
    const input: { settingKey: string; settingValue: string }[] = [];
    const pushNumber = (field: keyof DataMaintenanceValues) => {
      const value = values[field] as number | undefined;
      if (value != null && Number.isFinite(value)) {
        input.push({ settingKey: FIELD_KEYS[field], settingValue: String(value) });
      }
    };
    pushNumber('apiLogRetentionDays');
    pushNumber('userActionLogRetentionDays');
    pushNumber('businessLogRetentionDays');
    if (values.cleanupEnabled != null) {
      input.push({ settingKey: FIELD_KEYS.cleanupEnabled, settingValue: String(values.cleanupEnabled) });
    }
    if (input.length === 0) {
      message.warning('没有可保存的参数变更');
      return;
    }
    Modal.confirm({
      title: '确定保存吗?超期日志将按新保留策略在自动备份任务中处理,已保留数据不受影响。',
      okText: '确定保存',
      cancelText: '取消',
      onOk: async () => {
        await saveConfigs({ variables: { input } });
      },
    });
  };

  const partitionStatus = (partition: LogPartitionVo): { label: string; color: string } => {
    if (partition.isDefault) return { label: '默认分区', color: 'default' };
    if (partition.isCurrentMonth) return { label: '当前月', color: 'blue' };
    if (partition.expired) return { label: '已超期', color: 'orange' };
    return { label: '未超期', color: 'green' };
  };

  const backupDisabledReason = (partition: LogPartitionVo): string => {
    if (!canWrite) return '无系统配置修改权限(SYSTEM_CONFIG_WRITE)';
    if (partition.isDefault) return 'DEFAULT 兜底分区不可备份';
    if (partition.isCurrentMonth) return '当前月分区仍在写入,不可备份';
    if (!partition.expired) return '未达到保留期,暂不可备份';
    return '该分区已备份';
  };

  const handleBackup = (partition: LogPartitionVo) => {
    Modal.confirm({
      title: '离线备份分区',
      content: (
        <span>
          将把分区 <Typography.Text code>{partition.partitionName}</Typography.Text> 转储为冷表(
          <Typography.Text code>log_archive</Typography.Text>)并导出数据文件,在线日志将不再包含该月数据。
          <br />
          数据保留于冷表与备份文件中,确定继续吗?
        </span>
      ),
      okText: '确认备份',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: () =>
        backupPartition({
          variables: { input: { logTable: partition.logTable, partitionName: partition.partitionName } },
        }),
    });
  };

  const partitionColumns: ColumnsType<LogPartitionVo> = [
    { title: '分区名', dataIndex: 'partitionName', width: 220 },
    { title: '月份', dataIndex: 'partitionMonth', width: 120, render: (v: string | null) => v || '-' },
    { title: '行数', dataIndex: 'rowCount', width: 120, render: (v: number) => v.toLocaleString() },
    { title: '大小', dataIndex: 'sizeBytes', width: 120, render: (v: number) => formatBytes(v) },
    {
      title: '状态',
      key: 'status',
      width: 120,
      render: (_, record) => {
        const s = partitionStatus(record);
        return <Tag color={s.color}>{s.label}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_, record) => (
        <Tooltip title={record.backupable ? '' : backupDisabledReason(record)}>
          <Button
            type="link"
            size="small"
            danger
            disabled={!record.backupable || !canWrite || backingUp}
            onClick={() => handleBackup(record)}
          >
            离线备份
          </Button>
        </Tooltip>
      ),
    },
  ];

  const recordColumns: ColumnsType<LogBackupRecord> = [
    { title: '时间', dataIndex: 'startedAt', width: 170, render: (v: string) => formatDateTime(v) },
    {
      title: '日志表',
      dataIndex: 'logTable',
      width: 120,
      render: (v: LogTableType) => LOG_TABLE_LABEL[v] || v,
    },
    { title: '分区', dataIndex: 'partitionName', width: 180 },
    { title: '冷表', dataIndex: 'coldTableName', width: 220, ellipsis: true },
    { title: '行数', dataIndex: 'rowCount', width: 100, render: (v: number) => v.toLocaleString() },
    { title: '文件', dataIndex: 'filePath', ellipsis: true, render: (v?: string | null) => v || '-' },
    { title: '大小', dataIndex: 'fileSizeBytes', width: 100, render: (v?: number | null) => formatBytes(v) },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v: LogBackupStatus) => {
        const c = BACKUP_STATUS_CONFIG[v];
        return <Tag color={c.color}>{c.label}</Tag>;
      },
    },
    { title: '操作人', dataIndex: 'operatorName', width: 110, render: (v?: string | null) => v || '-' },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => setDetail(record)}>
          详情
        </Button>
      ),
    },
  ];

  const activePartitions = (overview?.partitions || []).filter((p) => p.logTable === activeTable);
  const recordsPage = overview?.backupRecords;
  const recordList = recordsPage?.list || [];

  return (
    <div>
      <Card title="数据维护" loading={loading}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
          message="日志表按月分区;超期分区(超过保留天数)在每日 03:30 与启动时自动备份(转储冷表 + 导出数据文件,log_cleanup_enabled 关闭时跳过),也可在下文手动备份。备份后数据保留于冷表(log_archive)与备份文件中,不再在线查询。每次修改留审计日志。"
        />
        <Form form={form} layout="vertical" style={{ maxWidth: 640 }}>
          <Divider plain>日志保留策略</Divider>
          <Form.Item
            name="apiLogRetentionDays"
            label="API 日志保留天数"
            rules={[{ required: true, message: '保留天数无效' }, { type: 'number', min: 1, max: 3650, message: '保留天数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} max={3650} precision={0} disabled={!canWrite} />
          </Form.Item>
          <Form.Item
            name="userActionLogRetentionDays"
            label="用户操作日志保留天数"
            rules={[{ required: true, message: '保留天数无效' }, { type: 'number', min: 1, max: 3650, message: '保留天数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} max={3650} precision={0} disabled={!canWrite} />
          </Form.Item>
          <Form.Item
            name="businessLogRetentionDays"
            label="业务日志保留天数"
            rules={[{ required: true, message: '保留天数无效' }, { type: 'number', min: 1, max: 3650, message: '保留天数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} max={3650} precision={0} disabled={!canWrite} />
          </Form.Item>
          <Form.Item
            name="cleanupEnabled"
            label="日志自动备份开关"
            valuePropName="checked"
            extra="关闭后不再按保留策略自动备份超期日志(手动备份不受影响)"
          >
            <Switch disabled={!canWrite} checkedChildren="开启" unCheckedChildren="关闭" />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" onClick={handleSave} loading={saving} disabled={!canWrite}>
                保存
              </Button>
              <Button onClick={() => form.resetFields()} disabled={!canWrite}>重置</Button>
            </Space>
          </Form.Item>
          {!canWrite && (
            <Typography.Text type="warning">当前账号无系统配置修改权限(SYSTEM_CONFIG_WRITE),仅可查看。</Typography.Text>
          )}
        </Form>
      </Card>

      <Card title="日志分区管理" style={{ marginTop: 16 }} loading={loading}>
        {activePartitions.length === 0 && (
          <Alert type="warning" showIcon style={{ marginBottom: 16 }} message="未发现月度分区,日志表可能尚未按月分区,请确认已应用数据库迁移。" />
        )}
        <Tabs
          activeKey={activeTable}
          onChange={(key) => setActiveTable(key as LogTableType)}
          items={TABLE_TABS.map((tab) => ({
            key: tab.key,
            label: tab.label,
            children: (
              <Table<LogPartitionVo>
                rowKey="partitionName"
                columns={partitionColumns}
                dataSource={activePartitions}
                pagination={false}
                size="middle"
                locale={{ emptyText: '暂无分区' }}
              />
            ),
          }))}
        />
      </Card>

      <Card title="离线备份记录" style={{ marginTop: 16 }} loading={loading}>
        <Table<LogBackupRecord>
          rowKey="id"
          columns={recordColumns}
          dataSource={recordList}
          size="middle"
          pagination={{
            current: (recordsPage?.pageInfo.pageNum ?? 0) + 1,
            pageSize: recordsPage?.pageInfo.pageSize ?? pageSize,
            total: recordsPage?.pageInfo.total ?? 0,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
            onChange: (nextPage, nextSize) => {
              setPage(nextPage - 1);
              setPageSize(nextSize);
            },
          }}
        />
      </Card>

      <Modal
        title="备份记录详情"
        open={detail != null}
        onCancel={() => setDetail(null)}
        footer={null}
        width={640}
      >
        {detail && (
          <Descriptions column={2} size="small" bordered>
            <Descriptions.Item label="日志表">{LOG_TABLE_LABEL[detail.logTable] || detail.logTable}</Descriptions.Item>
            <Descriptions.Item label="分区">{detail.partitionName}</Descriptions.Item>
            <Descriptions.Item label="冷表" span={2}>{detail.coldTableName}</Descriptions.Item>
            <Descriptions.Item label="行数">{detail.rowCount.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={BACKUP_STATUS_CONFIG[detail.status]?.color}>{BACKUP_STATUS_CONFIG[detail.status]?.label || detail.status}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="文件" span={2}>{detail.filePath || '-'}</Descriptions.Item>
            <Descriptions.Item label="文件大小">{formatBytes(detail.fileSizeBytes)}</Descriptions.Item>
            <Descriptions.Item label="SHA-256">{detail.checksum || '-'}</Descriptions.Item>
            <Descriptions.Item label="操作人">{detail.operatorName || '-'}</Descriptions.Item>
            <Descriptions.Item label="开始时间">{formatDateTime(detail.startedAt)}</Descriptions.Item>
            <Descriptions.Item label="完成时间">{formatDateTime(detail.finishedAt)}</Descriptions.Item>
            {detail.errorMessage && (
              <Descriptions.Item label="失败原因" span={2}>
                <Typography.Text type="danger">{detail.errorMessage}</Typography.Text>
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default SystemDataMaintenanceConfig;
