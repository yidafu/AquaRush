import React, { useEffect, useState } from 'react';
import { Card, Form, InputNumber, Button, Alert, Space, Modal, Typography } from 'antd';
import { SystemConfigGroup } from '@aquarush/common';
import type { SystemConfigVo } from '@aquarush/common';
import { useSystemConfigs, useSaveSystemConfigs } from '../../../services/system-graphql';
import SystemConfigAuditLog from '../components/SystemConfigAuditLog';

// 库存预警阈值配置页(F-SYS-003,R-PRO-02):低库存阈值 + 中档色阶阈值,保存即生效
// 仅超级管理员可读写(SYSTEM_CONFIG_WRITE,R-SYS-03);商品列表着色(F-PRO-006)与商品统计低库存明细(F-STA-006)按新阈值计算

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
      lowStockThreshold: valueOf('low_stock_threshold') ?? 10,
      midThreshold: valueOf('stock_warning_tier_mid') ?? 50,
    });
  }, [data, form]);

  const handleSave = async () => {
    const values = (await form.validateFields()) as StockConfigValues;
    Modal.confirm({
      title: '确定保存吗?将影响商品列表与商品统计的库存预警展示。',
      okText: '确定保存',
      cancelText: '取消',
      onOk: async () => {
        await saveConfigs({
          variables: {
            input: [
              { settingKey: 'low_stock_threshold', settingValue: String(values.lowStockThreshold) },
              { settingKey: 'stock_warning_tier_mid', settingValue: String(values.midThreshold) },
            ],
          },
        });
      },
    });
  };

  return (
    <div>
      <Card title="库存预警阈值" loading={loading}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
          message="低库存阈值与色阶档位配置后,商品列表库存着色(F-PRO-006)与商品统计低库存预警明细(F-STA-006)按新阈值计算。每次修改留审计日志。"
        />
        <Form form={form} layout="vertical" style={{ maxWidth: 480 }} preserve={false}>
          <Form.Item
            name="lowStockThreshold"
            label="低库存阈值(件)"
            extra="色阶:库存 < 该值 显示红色预警"
            rules={[
              { required: true, message: '请输入低库存阈值' },
              { type: 'number', min: 0, max: 99999, message: '阈值无效' },
            ]}
          >
            <InputNumber style={{ width: '100%' }} min={0} max={99999} precision={0} disabled={!canWrite} />
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
                  const low = getFieldValue('lowStockThreshold');
                  if (value != null && low != null && value <= low) {
                    return Promise.reject(new Error('档位需递增'));
                  }
                  return Promise.resolve();
                },
              }),
            ]}
          >
            <InputNumber style={{ width: '100%' }} min={0} max={99999} precision={0} disabled={!canWrite} />
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

export default SystemStockConfig;
