import React from 'react';
import { Alert, Card, Divider, Form, InputNumber, Space, Switch, Button, Typography } from '@aquarush/admin-ui';
import type { FormInstance } from '@aquarush/admin-ui';
import * as stylex from '@stylexjs/stylex';
import type { DataMaintenanceValues } from './constants';

const styles = stylex.create({
  alert: {
    marginBottom: '24px',
  },
  form: {
    maxWidth: '640px',
  },
  inputNumber: {
    width: '100%',
  },
});

export interface LogRetentionFormProps {
  form: FormInstance<DataMaintenanceValues>;
  saving: boolean;
  canWrite: boolean;
  onSave: () => void;
}

/** 日志保留策略表单(F-SYS-006,system/data/index.tsx 拆分) */
const LogRetentionForm: React.FC<LogRetentionFormProps> = ({
  form,
  saving,
  canWrite,
  onSave,
}) => (
  <Card>
    <Alert
      type="info"
      showIcon
      {...stylex.props(styles.alert)}
      message="日志表按月分区;超期分区(超过保留天数)在每日 03:30 与启动时自动备份(转储冷表 + 导出数据文件,log_cleanup_enabled 关闭时跳过),也可在下文手动备份。备份后数据保留于冷表(log_archive)与备份文件中,不再在线查询。每次修改留审计日志。"
    />
    <Form form={form} layout="vertical" {...stylex.props(styles.form)}>
      <Divider plain>日志保留策略</Divider>
      <Form.Item
        name="apiLogRetentionDays"
        label="API 日志保留天数"
        rules={[{ required: true, message: '保留天数无效' }, { type: 'number', min: 1, max: 3650, message: '保留天数无效' }]}
      >
        <InputNumber {...stylex.props(styles.inputNumber)} min={1} max={3650} precision={0} disabled={!canWrite} />
      </Form.Item>
      <Form.Item
        name="userActionLogRetentionDays"
        label="用户操作日志保留天数"
        rules={[{ required: true, message: '保留天数无效' }, { type: 'number', min: 1, max: 3650, message: '保留天数无效' }]}
      >
        <InputNumber {...stylex.props(styles.inputNumber)} min={1} max={3650} precision={0} disabled={!canWrite} />
      </Form.Item>
      <Form.Item
        name="businessLogRetentionDays"
        label="业务日志保留天数"
        rules={[{ required: true, message: '保留天数无效' }, { type: 'number', min: 1, max: 3650, message: '保留天数无效' }]}
      >
        <InputNumber {...stylex.props(styles.inputNumber)} min={1} max={3650} precision={0} disabled={!canWrite} />
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
          <Button type="primary" onClick={onSave} loading={saving} disabled={!canWrite}>
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
);

export default LogRetentionForm;
