import React, { useEffect, useState } from 'react';
import { Card, Form, InputNumber, Switch, Button, Alert, Space, Modal, Typography, Divider, message } from 'antd';
import { SystemConfigGroup, SystemSettingKeys } from '@aquarush/common';
import type { SystemConfigVo } from '@aquarush/common';
import { useSystemConfigs, useSaveSystemConfigs } from '../../../services/system-graphql';
import SystemConfigAuditLog from '../components/SystemConfigAuditLog';

// 账号与安全策略配置页(F-SYS-004):密码强度/登录失败锁定/Token 有效期,保存即生效(登录校验类即时,Token 类重启生效)
// 仅超级管理员可读写(SYSTEM_CONFIG_WRITE,R-SYS-03);生效接线(登录锁定/密码强度校验)为后续项,PRD 目标形态
// 密钥类(微信 appsecret 等)不在此在线配置(R-SYS-02)

interface SecurityConfigValues {
  passwordMinLength?: number;
  passwordRequireAlphanumeric?: boolean;
  loginFailLockThreshold?: number;
  loginLockDurationMinutes?: number;
  tokenValidityHours?: number;
}

// 键 → 表单字段映射(种子默认值兜底:8/true/5/30/24)
const FIELD_KEYS: Record<keyof SecurityConfigValues, string> = {
  passwordMinLength: SystemSettingKeys.PASSWORD_MIN_LENGTH,
  passwordRequireAlphanumeric: SystemSettingKeys.PASSWORD_REQUIRE_ALPHANUMERIC,
  loginFailLockThreshold: SystemSettingKeys.LOGIN_FAIL_LOCK_THRESHOLD,
  loginLockDurationMinutes: SystemSettingKeys.LOGIN_LOCK_DURATION_MINUTES,
  tokenValidityHours: SystemSettingKeys.TOKEN_VALIDITY_HOURS,
};

const DEFAULTS: Record<keyof SecurityConfigValues, number | boolean> = {
  passwordMinLength: 8,
  passwordRequireAlphanumeric: true,
  loginFailLockThreshold: 5,
  loginLockDurationMinutes: 30,
  tokenValidityHours: 24,
};

const SystemSecurityConfig: React.FC = () => {
  const [form] = Form.useForm();
  const [loadConfigs, { data, loading }] = useSystemConfigs();
  const [saveConfigs, { loading: saving }] = useSaveSystemConfigs();
  const [canWrite] = useState(() => JSON.parse(localStorage.getItem('userInfo') || '{}').role === 'SUPER_ADMIN');

  useEffect(() => {
    loadConfigs({ variables: { group: SystemConfigGroup.SECURITY } });
  }, [loadConfigs]);

  // 回显:键 → 表单字段(未配置时按种子默认值兜底)
  useEffect(() => {
    const configs = data?.systemConfigs || [];
    if (configs.length === 0) return;
    const byKey = new Map<string, SystemConfigVo>(configs.map((c: SystemConfigVo) => [c.settingKey, c]));
    const values: Partial<SecurityConfigValues> = {};
    (Object.keys(FIELD_KEYS) as (keyof SecurityConfigValues)[]).forEach((field) => {
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
    const values = (await form.validateFields()) as SecurityConfigValues;
    const input: { settingKey: string; settingValue: string }[] = [];
    const pushNumber = (field: keyof SecurityConfigValues) => {
      const value = values[field] as number | undefined;
      if (value != null && Number.isFinite(value)) {
        input.push({ settingKey: FIELD_KEYS[field], settingValue: String(value) });
      }
    };
    pushNumber('passwordMinLength');
    pushNumber('loginFailLockThreshold');
    pushNumber('loginLockDurationMinutes');
    pushNumber('tokenValidityHours');
    if (values.passwordRequireAlphanumeric != null) {
      input.push({ settingKey: FIELD_KEYS.passwordRequireAlphanumeric, settingValue: String(values.passwordRequireAlphanumeric) });
    }
    if (input.length === 0) {
      message.warning('没有可保存的参数变更');
      return;
    }
    Modal.confirm({
      title: '确定保存吗?新策略将影响后续密码校验与登录校验;Token 有效期修改后重启生效,已签发凭证不受影响。',
      okText: '确定保存',
      cancelText: '取消',
      onOk: async () => {
        await saveConfigs({ variables: { input } });
      },
    });
  };

  return (
    <div>
      <Card title="账号与安全策略" loading={loading}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
          message="密码强度与登录锁定策略为后续生效接线项(PRD F-SYS-004 目标形态),Token 有效期修改后重启生效;每次修改留审计日志。"
        />
        <Form form={form} layout="vertical" style={{ maxWidth: 640 }}>
          <Divider plain>密码策略</Divider>
          <Form.Item
            name="passwordMinLength"
            label="密码最小长度"
            extra="新创建/重置密码按此长度校验"
            rules={[{ required: true, message: '参数无效' }, { type: 'number', min: 6, max: 32, message: '参数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={6} max={32} precision={0} disabled={!canWrite} />
          </Form.Item>
          <Form.Item
            name="passwordRequireAlphanumeric"
            label="要求字母数字混合"
            valuePropName="checked"
            extra="开启后密码须同时包含字母与数字"
          >
            <Switch disabled={!canWrite} checkedChildren="开启" unCheckedChildren="关闭" />
          </Form.Item>

          <Divider plain>登录安全</Divider>
          <Form.Item
            name="loginFailLockThreshold"
            label="登录失败锁定次数"
            extra="连续错误达该次数后账号锁定"
            rules={[{ required: true, message: '参数无效' }, { type: 'number', min: 1, max: 99, message: '参数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} max={99} precision={0} disabled={!canWrite} />
          </Form.Item>
          <Form.Item
            name="loginLockDurationMinutes"
            label="锁定时长(分钟)"
            rules={[{ required: true, message: '参数无效' }, { type: 'number', min: 1, message: '参数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} precision={0} disabled={!canWrite} />
          </Form.Item>

          <Divider plain>Token</Divider>
          <Form.Item
            name="tokenValidityHours"
            label="Token 有效期(小时)"
            extra="修改后重启生效,到期后需重新登录"
            rules={[{ required: true, message: '参数无效' }, { type: 'number', min: 1, max: 720, message: '参数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} max={720} precision={0} disabled={!canWrite} />
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

export default SystemSecurityConfig;
