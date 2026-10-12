import React, { useEffect, useState } from 'react';
import { Card, Form, InputNumber, Button, Alert, Space, Modal, Typography, message } from '@aquarush/admin-ui';
import { SystemConfigGroup, SystemSettingKeys } from '@aquarush/common';
import type { SystemConfigVo } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { useSystemConfigs, useSaveSystemConfigs } from '../../../services/system-graphql';

// 库存预警阈值配置页(F-SYS-003,R-PRO-02):低库存阈值 + 中档色阶阈值,保存即生效
// 仅超级管理员可读写(SYSTEM_CONFIG_WRITE,R-SYS-03);商品列表着色(F-PRO-006)与商品统计低库存明细(F-STA-006)按新阈值计算

const styles = stylex.create({
  alert: {
    marginBottom: '24px',
  },
  form: {
    maxWidth: '480px',
  },
  inputNumber: {
    width: '100%',
  },
});

interface StockConfigValues {
  lowStockThreshold?: number;
  midThreshold?: number;
}

const SystemStockConfig: React.FC = () => {
  const [form] = Form.useForm();
  const [loadConfigs, { data, loading }] = useSystemConfigs();
  const [saveConfigs, { loading: saving }] = useSaveSystemConfigs();
  const [canWrite] = useState(() => JSON.parse(localStorage.getItem('userInfo') || '{}').role === 'SUPER_ADMIN');

  useEffect(() => {
    loadConfigs({ variables: { group: SystemConfigGroup.INVENTORY } });
  }, [loadConfigs]);

  // 回显:键 → 表单字段(未配置时默认 10/50 由种子保证,此处兜底)
  useEffect(() => {
    const configs = data?.systemConfigs || [];
    if (configs.length === 0) return;
    const byKey = new Map<string, SystemConfigVo>(configs.map((c: SystemConfigVo) => [c.settingKey, c]));
    const valueOf = (key: string): number | undefined => {
      const raw = byKey.get(key)?.settingValue;
      return raw == null ? undefined : Number(raw);
    };
    form.setFieldsValue({
      lowStockThreshold: valueOf(SystemSettingKeys.LOW_STOCK_THRESHOLD) ?? 10,
      midThreshold: valueOf(SystemSettingKeys.STOCK_WARNING_TIER_MID) ?? 50,
    });
  }, [data, form]);

  const handleSave = async () => {
    const values = (await form.validateFields()) as StockConfigValues;
    // 仅收集有效值:字段缺失/非法时跳过,避免把 "undefined"/"NaN" 发往后端触发"参数值格式不正确"
    const input: { settingKey: string; settingValue: string }[] = [];
    if (values.lowStockThreshold != null && Number.isFinite(values.lowStockThreshold)) {
      input.push({ settingKey: SystemSettingKeys.LOW_STOCK_THRESHOLD, settingValue: String(values.lowStockThreshold) });
    }
    if (values.midThreshold != null && Number.isFinite(values.midThreshold)) {
      input.push({ settingKey: SystemSettingKeys.STOCK_WARNING_TIER_MID, settingValue: String(values.midThreshold) });
    }
    if (input.length === 0) {
      message.warning('没有可保存的参数变更');
      return;
    }
    Modal.confirm({
      title: '确定保存吗?将影响商品列表与商品统计的库存预警展示。',
      okText: '确定保存',
      cancelText: '取消',
      onOk: async () => {
        await saveConfigs({ variables: { input } });
      },
    });
  };

  return (
    <div>
      <Card title="库存预警阈值" loading={loading}>
        <Alert
          type="info"
          showIcon
          {...stylex.props(styles.alert)}
          message="低库存阈值与色阶档位配置后,商品列表库存着色(F-PRO-006)与商品统计低库存预警明细(F-STA-006)按新阈值计算。每次修改留审计日志。"
        />
        <Form form={form} layout="vertical" {...stylex.props(styles.form)}>
          <Form.Item
            name="lowStockThreshold"
            label="低库存阈值(件)"
            extra="色阶:库存 < 该值 显示红色预警"
            rules={[
              { required: true, message: '请输入低库存阈值' },
              { type: 'number', min: 0, max: 99999, message: '阈值无效' },
            ]}
          >
            <InputNumber {...stylex.props(styles.inputNumber)} min={0} max={99999} precision={0} disabled={!canWrite} />
          </Form.Item>
          <Form.Item
            name="midThreshold"
            label="中档预警阈值(件)"
            extra="色阶:库存 ≥ 低库存阈值 且 < 该值 显示橙色预警,其余绿色"
            dependencies={['lowStockThreshold']}
            rules={[
              { required: true, message: '请输入中档预警阈值' },
              { type: 'number', min: 0, max: 99999, message: '阈值无效' },
              ({ getFieldValue }) => ({
                validator: (_, value?: number) => {
                  const low = getFieldValue('lowStockThreshold') as number | undefined;
                  if (value != null && low != null && value <= low) {
                    return Promise.reject(new Error('档位需递增'));
                  }
                  return Promise.resolve();
                },
              }),
            ]}
          >
            <InputNumber {...stylex.props(styles.inputNumber)} min={0} max={99999} precision={0} disabled={!canWrite} />
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
    </div>
  );
};

export default SystemStockConfig;
