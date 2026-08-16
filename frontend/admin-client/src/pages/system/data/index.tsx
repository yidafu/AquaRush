import React, { useEffect, useState } from 'react';
import { Card, Form, InputNumber, Switch, Button, Alert, Space, Modal, Typography, Divider, message } from 'antd';
import { SystemConfigGroup, SystemSettingKeys } from '@aquarush/common';
import type { SystemConfigVo } from '@aquarush/common';
import { useSystemConfigs, useSaveSystemConfigs } from '../../../services/system-graphql';
import SystemConfigAuditLog from '../components/SystemConfigAuditLog';

// 数据维护配置页(F-SYS-006):各类日志保留天数 + 清理开关,保存即生效(策略存储)
// 仅超级管理员可读写(SYSTEM_CONFIG_WRITE,R-SYS-03);超期清理执行(定时任务/立即清理)为后续项;
// 数据备份为运维职责(scripts/backup.sh),不在本功能范围

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

const SystemDataMaintenanceConfig: React.FC = () => {
  const [form] = Form.useForm();
  const [loadConfigs, { data, loading }] = useSystemConfigs();
  const [saveConfigs, { loading: saving }] = useSaveSystemConfigs();
  const [canWrite] = useState(() => JSON.parse(localStorage.getItem('userInfo') || '{}').role === 'SUPER_ADMIN');

  useEffect(() => {
    loadConfigs({ variables: { group: SystemConfigGroup.DATA_MAINTENANCE } });
  }, [loadConfigs]);

  // 回显:键 → 表单字段(未配置时按种子默认值兜底)
  useEffect(() => {
    const configs = data?.systemConfigs || [];
    if (configs.length === 0) return;
    const byKey = new Map<string, SystemConfigVo>(configs.map((c: SystemConfigVo) => [c.settingKey, c]));
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
  }, [data, form]);

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
      title: '确定保存吗?超期日志将按新保留策略在清理任务中处理,已保留数据不受影响。',
      okText: '确定保存',
      cancelText: '取消',
      onOk: async () => {
        await saveConfigs({ variables: { input } });
      },
    });
  };

  return (
    <div>
      <Card title="数据维护" loading={loading}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
          message="保留策略保存后由清理任务按策略执行(超期清理执行为后续项);数据备份为运维职责(scripts/backup.sh),不在此配置。每次修改留审计日志。"
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
            label="日志清理开关"
            valuePropName="checked"
            extra="关闭后不再按保留策略自动清理超期日志"
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

      <Card title="修改记录" style={{ marginTop: 16 }}>
        <SystemConfigAuditLog />
      </Card>
    </div>
  );
};

export default SystemDataMaintenanceConfig;
