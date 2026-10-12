import React, { useEffect, useState } from 'react';
import {
  Alert,
  Card,
  Form,
  message,
  Modal,
  Tabs,
  Typography,
} from '@aquarush/admin-ui';
import { LogTableType } from '@aquarush/common';
import type { DataMaintenanceOverviewVo, LogBackupRecordVo, LogPartitionVo } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { useBackupLogPartition, useDataMaintenanceOverview, useSaveSystemConfigs } from '../../../services/system-graphql';
import {
  LogRetentionForm,
  PartitionTable,
  BackupRecordsTable,
  BackupRecordDetailModal,
} from './components';
import { FIELD_KEYS, DEFAULTS, TABLE_TABS, type DataMaintenanceValues } from './components/constants';

// 数据维护页(F-SYS-006):日志保留策略 + 日志分区管理(按月分区,离线备份 = 转储冷表 + 导出文件)
// 仅超级管理员可读写(SYSTEM_CONFIG_READ/WRITE,R-SYS-03);超期分区可手动备份,每日 03:30 自动备份(log_cleanup_enabled 关闭时跳过)。

const styles = stylex.create({
  card: {
    marginTop: '16px',
  },
  alert: {
    marginBottom: '16px',
  },
});

const SystemDataMaintenanceConfig: React.FC = () => {
  const [form] = Form.useForm<DataMaintenanceValues>();
  const [loadOverview, { data, loading }] = useDataMaintenanceOverview();
  const [saveConfigs, { loading: saving }] = useSaveSystemConfigs();
  const [backupPartition, { loading: backingUp }] = useBackupLogPartition();
  const [canWrite] = useState(() => JSON.parse(localStorage.getItem('userInfo') || '{}').role === 'SUPER_ADMIN');
  const [activeTable, setActiveTable] = useState<LogTableType>(LogTableType.API_LOG);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [detail, setDetail] = useState<LogBackupRecordVo | null>(null);

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
      okType: 'danger',
      onOk: () =>
        backupPartition({
          variables: { input: { logTable: partition.logTable, partitionName: partition.partitionName } },
        }),
    });
  };

  const activePartitions = (overview?.partitions || []).filter((p) => p.logTable === activeTable);
  const recordsPage = overview?.backupRecords;
  const recordList = recordsPage?.list || [];

  return (
    <div>
      <LogRetentionForm
        form={form}
        saving={saving}
        canWrite={canWrite}
        onSave={handleSave}
      />

      <Card title="日志分区管理" {...stylex.props(styles.card)} loading={loading}>
        {activePartitions.length === 0 && (
          <Alert type="warning" showIcon {...stylex.props(styles.alert)} message="未发现月度分区,日志表可能尚未按月分区,请确认已应用数据库迁移。" />
        )}
        <Tabs
          activeKey={activeTable}
          onChange={(key) => setActiveTable(key as LogTableType)}
          items={TABLE_TABS.map((tab) => ({
            key: tab.key,
            label: tab.label,
            children: (
              <PartitionTable
                partitions={activePartitions}
                canWrite={canWrite}
                backingUp={backingUp}
                onBackup={handleBackup}
              />
            ),
          }))}
        />
      </Card>

      <Card title="离线备份记录" {...stylex.props(styles.card)} loading={loading}>
        <BackupRecordsTable
          records={recordList}
          pageNum={recordsPage?.pageInfo.pageNum ?? 0}
          pageSize={recordsPage?.pageInfo.pageSize ?? pageSize}
          total={recordsPage?.pageInfo.total ?? 0}
          onPageChange={(nextPage, nextSize) => {
            setPage(nextPage);
            setPageSize(nextSize);
          }}
          onView={setDetail}
        />
      </Card>

      <BackupRecordDetailModal record={detail} onClose={() => setDetail(null)} />
    </div>
  );
};

export default SystemDataMaintenanceConfig;
