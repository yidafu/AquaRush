import React, { useEffect, useState } from 'react';
import { Card, Form, InputNumber, Switch, TimePicker, Button, Alert, Space, Divider, Modal, Typography, message } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import { SystemConfigGroup, SystemSettingKeys } from '@aquarush/common';
import type { SystemConfigVo } from '@aquarush/common';
import { useSystemConfigs, useSaveSystemConfigs } from '../../../services/system-graphql';
import SystemConfigAuditLog from '../components/SystemConfigAuditLog';

// 订单与履约参数配置页(F-SYS-002):支付超时/配送时段/派单与确认,保存即生效
// 仅超级管理员可读写(SYSTEM_CONFIG_WRITE,R-SYS-03);已生成订单不受影响(快照原则,R-SYS-01)

interface SlotConfig {
  enabledKey: string;
  startKey: string;
  endKey: string;
}

const SLOTS: { label: string; config: SlotConfig }[] = [
  {
    label: '上午',
    config: {
      enabledKey: SystemSettingKeys.DELIVERY_SLOT_MORNING_ENABLED,
      startKey: SystemSettingKeys.DELIVERY_SLOT_MORNING_START,
      endKey: SystemSettingKeys.DELIVERY_SLOT_MORNING_END,
    },
  },
  {
    label: '下午',
    config: {
      enabledKey: SystemSettingKeys.DELIVERY_SLOT_AFTERNOON_ENABLED,
      startKey: SystemSettingKeys.DELIVERY_SLOT_AFTERNOON_START,
      endKey: SystemSettingKeys.DELIVERY_SLOT_AFTERNOON_END,
    },
  },
  {
    label: '晚上',
    config: {
      enabledKey: SystemSettingKeys.DELIVERY_SLOT_EVENING_ENABLED,
      startKey: SystemSettingKeys.DELIVERY_SLOT_EVENING_START,
      endKey: SystemSettingKeys.DELIVERY_SLOT_EVENING_END,
    },
  },
];

interface OrderConfigValues {
  paymentTimeoutMinutes?: number;
  immediateMinutes?: number;
  dispatchTimeoutReminderMinutes?: number;
  autoConfirmReceiptDays?: number;
  slots: Record<string, { enabled: boolean; range?: [Dayjs, Dayjs] }>;
}

const SystemOrderConfig: React.FC = () => {
  const [form] = Form.useForm();
  const [loadConfigs, { data, loading }] = useSystemConfigs();
  const [saveConfigs, { loading: saving }] = useSaveSystemConfigs();
  const [canWrite] = useState(() => JSON.parse(localStorage.getItem('userInfo') || '{}').role === 'SUPER_ADMIN');

  useEffect(() => {
    loadConfigs({ variables: { group: SystemConfigGroup.ORDER_AND_FULFILLMENT } });
  }, [loadConfigs]);

  // 回显:键 → 表单字段(配送时段成对映射为 RangePicker)
  useEffect(() => {
    const configs = data?.systemConfigs || [];
    if (configs.length === 0) return;
    const byKey = new Map<string, SystemConfigVo>(configs.map((c: SystemConfigVo) => [c.settingKey, c]));
    const valueOf = (key: string): string | null => byKey.get(key)?.settingValue ?? null;
    const numberValue = (key: string): number | undefined => {
      const raw = valueOf(key);
      return raw == null ? undefined : Number(raw);
    };

    const slotValues: OrderConfigValues['slots'] = {};
    for (const slot of SLOTS) {
      const start = valueOf(slot.config.startKey);
      const end = valueOf(slot.config.endKey);
      slotValues[slot.label] = {
        enabled: valueOf(slot.config.enabledKey) !== 'false',
        range:
          start != null && end != null
            ? [dayjs(start, 'HH:mm'), dayjs(end, 'HH:mm')]
            : undefined,
      };
    }

    form.setFieldsValue({
      paymentTimeoutMinutes: numberValue(SystemSettingKeys.PAYMENT_TIMEOUT_MINUTES),
      immediateMinutes: numberValue(SystemSettingKeys.DELIVERY_SLOT_IMMEDIATE_MINUTES),
      dispatchTimeoutReminderMinutes: numberValue(SystemSettingKeys.DISPATCH_TIMEOUT_REMINDER_MINUTES),
      autoConfirmReceiptDays: numberValue(SystemSettingKeys.AUTO_CONFIRM_RECEIPT_DAYS),
      slots: slotValues,
    });
  }, [data, form]);

  const handleSave = async () => {
    const values = (await form.validateFields()) as OrderConfigValues;
    // 仅收集有效值:数值字段缺失/非法时跳过,避免把 "undefined"/"NaN" 发往后端触发"参数值格式不正确"
    const input: { settingKey: string; settingValue: string }[] = [];
    const pushNumber = (key: string, value: number | undefined) => {
      if (value != null && Number.isFinite(value)) {
        input.push({ settingKey: key, settingValue: String(value) });
      }
    };
    pushNumber(SystemSettingKeys.PAYMENT_TIMEOUT_MINUTES, values.paymentTimeoutMinutes);
    pushNumber(SystemSettingKeys.DELIVERY_SLOT_IMMEDIATE_MINUTES, values.immediateMinutes);
    pushNumber(SystemSettingKeys.DISPATCH_TIMEOUT_REMINDER_MINUTES, values.dispatchTimeoutReminderMinutes);
    pushNumber(SystemSettingKeys.AUTO_CONFIRM_RECEIPT_DAYS, values.autoConfirmReceiptDays);
    for (const slot of SLOTS) {
      const slotValue = values.slots?.[slot.label];
      if (!slotValue) continue;
      input.push({ settingKey: slot.config.enabledKey, settingValue: String(slotValue.enabled) });
      if (slotValue.range && slotValue.range[0] && slotValue.range[1]) {
        input.push({ settingKey: slot.config.startKey, settingValue: slotValue.range[0].format('HH:mm') });
        input.push({ settingKey: slot.config.endKey, settingValue: slotValue.range[1].format('HH:mm') });
      }
    }
    if (input.length === 0) {
      message.warning('没有可保存的参数变更');
      return;
    }
    Modal.confirm({
      title: '确定保存吗?将影响新订单的履约流程,已生成订单不受影响',
      okText: '确定保存',
      cancelText: '取消',
      onOk: async () => {
        await saveConfigs({ variables: { input } });
      },
    });
  };

  return (
    <div>
      <Card title="订单与履约参数" loading={loading}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
          message="配置将影响新订单的履约流程,已生成订单不受影响;每次修改留审计日志。"
        />
        <Form form={form} layout="vertical" style={{ maxWidth: 640 }}>
          <Divider plain>支付参数</Divider>
          <Form.Item
            name="paymentTimeoutMinutes"
            label="支付超时(分钟)"
            extra="支付超时后自动取消订单并恢复库存(R-ORD-04)"
            rules={[{ required: true, message: '参数无效' }, { type: 'number', min: 1, message: '参数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} precision={0} disabled={!canWrite} />
          </Form.Item>

          <Divider plain>配送时段</Divider>
          <Form.Item
            name="immediateMinutes"
            label="立即配送时限(分钟)"
            rules={[{ required: true, message: '参数无效' }, { type: 'number', min: 1, message: '参数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} precision={0} disabled={!canWrite} />
          </Form.Item>

          <Form.List name="slots">
            {() =>
              SLOTS.map((slot) => (
                <Space key={slot.label} align="baseline" style={{ display: 'flex', marginBottom: 16 }} size="middle">
                  <Typography.Text strong style={{ width: 48 }}>{slot.label}</Typography.Text>
                  <Form.Item
                    name={[slot.label, 'enabled']}
                    valuePropName="checked"
                    style={{ marginBottom: 0 }}
                  >
                    <Switch disabled={!canWrite} checkedChildren="启用" unCheckedChildren="停用" />
                  </Form.Item>
                  <Form.Item
                    name={[slot.label, 'range']}
                    style={{ marginBottom: 0 }}
                    dependencies={[['slots', slot.label, 'enabled']]}
                    rules={[
                      () => ({
                        validator: (_, value?: [Dayjs, Dayjs]) => {
                          if (!value) return Promise.resolve();
                          if (!value[0].isBefore(value[1])) return Promise.reject(new Error('时间窗不合法'));
                          return Promise.resolve();
                        },
                      }),
                    ]}
                  >
                    <TimePicker.RangePicker format="HH:mm" disabled={!canWrite} />
                  </Form.Item>
                </Space>
              ))
            }
          </Form.List>

          <Divider plain>派单与确认</Divider>
          <Form.Item
            name="dispatchTimeoutReminderMinutes"
            label="派单超时提醒(分钟)"
            rules={[{ required: true, message: '参数无效' }, { type: 'number', min: 1, message: '参数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} precision={0} disabled={!canWrite} />
          </Form.Item>
          <Form.Item
            name="autoConfirmReceiptDays"
            label="自动确认收货(天)"
            rules={[{ required: true, message: '参数无效' }, { type: 'number', min: 1, message: '参数无效' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} precision={0} disabled={!canWrite} />
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

export default SystemOrderConfig;
