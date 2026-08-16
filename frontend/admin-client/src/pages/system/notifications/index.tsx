import React, { useState, useEffect, useCallback } from 'react';
import { Card, Table, Tag, Button, Alert, Modal, Form, Input, InputNumber, Switch, Typography, message } from 'antd';
import { SystemConfigGroup, SystemSettingKeys } from '@aquarush/common';
import type { SystemConfigVo } from '@aquarush/common';
import { useSystemConfigs, useSaveSystemConfigs } from '../../../services/system-graphql';

// 通知与订阅消息配置页(F-NTF-005,承接原 F-SYS-005):微信订阅消息模板(用户端) + 送水 App 实时推送配置(送水工端 F-NTF-003)
// 仅超级管理员可读写(SYSTEM_CONFIG_WRITE,R-SYS-03);密钥类(appid/secret、APNs/FCM 凭证)不在此在线配置(R-SYS-02/R-NTF-05);
// 实际发送依赖通知链路(F-NTF-001/G08、F-NTF-003/G44,目标形态)

interface TemplateScenario {
  key: string;
  label: string;
  description: string;
  idKey: string;
  enabledKey: string;
}

const SCENARIOS: TemplateScenario[] = [
  {
    key: 'order_created',
    label: '下单通知',
    description: '用户下单成功后推送订单确认',
    idKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_ORDER_CREATED_ID,
    enabledKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_ORDER_CREATED_ENABLED,
  },
  {
    key: 'delivery_completed',
    label: '配送完成',
    description: '配送完成后推送送达确认',
    idKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_DELIVERY_COMPLETED_ID,
    enabledKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_DELIVERY_COMPLETED_ENABLED,
  },
  {
    key: 'order_urging',
    label: '催单',
    description: '配送超时用户催单提醒',
    idKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_ORDER_URGING_ID,
    enabledKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_ORDER_URGING_ENABLED,
  },
  {
    key: 'payment_result',
    label: '支付结果',
    description: '支付成功/失败结果通知',
    idKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_PAYMENT_RESULT_ID,
    enabledKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_PAYMENT_RESULT_ENABLED,
  },
];

// 微信模板 ID:字母/数字/下划线/中划线,6-64 位(宽松校验,避免误拦)
const TEMPLATE_ID_PATTERN = /^[A-Za-z0-9_-]{6,64}$/;

interface DeliveryPushScenario {
  key: string;
  label: string;
  description: string;
  enabledKey: string;
}

// 送水 App 实时推送场景(F-NTF-003,PRD 13-notification.md):任务关键节点
const DELIVERY_PUSH_SCENARIOS: DeliveryPushScenario[] = [
  {
    key: 'new_order',
    label: '新订单',
    description: '新订单产生(可抢单),任务列表即时置顶',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_NEW_ORDER_ENABLED,
  },
  {
    key: 'assigned',
    label: '被派单',
    description: '订单派给本人时推送',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_ASSIGNED_ENABLED,
  },
  {
    key: 'cancelled',
    label: '订单取消',
    description: '已接订单被取消时推送',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_CANCELLED_ENABLED,
  },
  {
    key: 'urging',
    label: '用户催单',
    description: '用户催单时推送',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_URGING_ENABLED,
  },
  {
    key: 'timeout',
    label: '配送超时',
    description: '配送超时未送达提醒',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_TIMEOUT_ENABLED,
  },
  {
    key: 'payment_timeout',
    label: '支付超时',
    description: '待支付订单超时未支付提醒',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_PAYMENT_TIMEOUT_ENABLED,
  },
];

interface TemplateRow {
  scenario: TemplateScenario;
  templateId: string | null;
  enabled: boolean;
}

const SystemNotificationsConfig: React.FC = () => {
  const canWrite = JSON.parse(localStorage.getItem('userInfo') || '{}').role === 'SUPER_ADMIN';
  const [loadConfigs, { data, loading }] = useSystemConfigs();
  const [saveConfigs, { loading: saving }] = useSaveSystemConfigs();
  const [rows, setRows] = useState<TemplateRow[]>([]);
  const [editing, setEditing] = useState<TemplateRow | null>(null);
  const [deliveryRows, setDeliveryRows] = useState<{ scenario: DeliveryPushScenario; enabled: boolean }[]>([]);
  const [pollInterval, setPollInterval] = useState<number | undefined>(undefined);
  const [form] = Form.useForm();

  const loadGroup = useCallback(() => {
    loadConfigs({ variables: { group: SystemConfigGroup.NOTIFICATION } });
  }, [loadConfigs]);

  useEffect(() => {
    loadGroup();
  }, [loadGroup]);

  // 回显:键 → 模板行(模板 ID 未配置时展示「未设置」)+ 送水 App 推送开关/轮询间隔(未配置按默认 30s 兜底)
  useEffect(() => {
    const configs = data?.systemConfigs || [];
    if (configs.length === 0) return;
    const byKey = new Map<string, SystemConfigVo>(configs.map((c: SystemConfigVo) => [c.settingKey, c]));
    setRows(
      SCENARIOS.map((scenario) => ({
        scenario,
        templateId: byKey.get(scenario.idKey)?.settingValue ?? null,
        enabled: byKey.get(scenario.enabledKey)?.settingValue !== 'false',
      })),
    );
    setDeliveryRows(
      DELIVERY_PUSH_SCENARIOS.map((scenario) => ({
        scenario,
        enabled: byKey.get(scenario.enabledKey)?.settingValue !== 'false',
      })),
    );
    const rawInterval = byKey.get(SystemSettingKeys.DELIVERY_POLL_INTERVAL_SECONDS)?.settingValue;
    setPollInterval(rawInterval == null ? 30 : Number(rawInterval));
  }, [data]);

  // 编辑弹窗打开时回填当前值
  useEffect(() => {
    if (!editing) return;
    form.setFieldsValue({
      templateId: editing.templateId ?? '',
      enabled: editing.enabled,
    });
  }, [editing, form]);

  const handleSaveEdit = async () => {
    if (!editing) return;
    const values = await form.validateFields();
    // 模板 ID 必填(AC-SYS-005-02),空值由表单校验拦截
    const input = [
      { settingKey: editing.scenario.idKey, settingValue: values.templateId },
      { settingKey: editing.scenario.enabledKey, settingValue: String(values.enabled) },
    ];
    await saveConfigs({ variables: { input } });
    setEditing(null);
  };

  // 送水 App 推送配置保存:6 个场景开关 + 轮询间隔,一次 mutation(R-NTF-03,默认 30s)
  const handleSaveDeliveryPush = async () => {
    if (pollInterval == null || !Number.isFinite(pollInterval)) {
      message.error('轮询间隔无效');
      return;
    }
    const input: { settingKey: string; settingValue: string }[] = deliveryRows.map((row) => ({
      settingKey: row.scenario.enabledKey,
      settingValue: String(row.enabled),
    }));
    input.push({ settingKey: SystemSettingKeys.DELIVERY_POLL_INTERVAL_SECONDS, settingValue: String(pollInterval) });
    Modal.confirm({
      title: '确定保存吗?推送链路接通后送水工端按此配置触达,已推送消息不受影响。',
      okText: '确定保存',
      cancelText: '取消',
      onOk: async () => {
        await saveConfigs({ variables: { input } });
      },
    });
  };

  const deliveryColumns = [
    {
      title: '推送场景',
      dataIndex: ['scenario', 'label'],
      key: 'label',
      width: 140,
      render: (_: unknown, record: { scenario: DeliveryPushScenario; enabled: boolean }) => (
        <Typography.Text strong>{record.scenario.label}</Typography.Text>
      ),
    },
    {
      title: '状态',
      key: 'enabled',
      width: 130,
      render: (_: unknown, record: { scenario: DeliveryPushScenario; enabled: boolean }) => (
        <Switch
          checked={record.enabled}
          disabled={!canWrite}
          checkedChildren="启用"
          unCheckedChildren="停用"
          onChange={(checked) =>
            setDeliveryRows((prev) =>
              prev.map((row) => (row.scenario.key === record.scenario.key ? { ...row, enabled: checked } : row)),
            )
          }
        />
      ),
    },
    {
      title: '说明',
      dataIndex: ['scenario', 'description'],
      key: 'description',
    },
  ];

  const columns = [
    {
      title: '业务场景',
      dataIndex: ['scenario', 'label'],
      key: 'label',
      width: 140,
      render: (_: unknown, record: TemplateRow) => <Typography.Text strong>{record.scenario.label}</Typography.Text>,
    },
    {
      title: '模板 ID',
      key: 'templateId',
      width: 280,
      render: (_: unknown, record: TemplateRow) =>
        record.templateId ? <Typography.Text code>{record.templateId}</Typography.Text> : (
          <Typography.Text type="secondary">未设置</Typography.Text>
        ),
    },
    {
      title: '状态',
      key: 'enabled',
      width: 90,
      render: (_: unknown, record: TemplateRow) =>
        record.enabled ? <Tag color="green">启用</Tag> : <Tag color="default">停用</Tag>,
    },
    {
      title: '说明',
      dataIndex: ['scenario', 'description'],
      key: 'description',
    },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      render: (_: unknown, record: TemplateRow) => (
        <Button type="link" size="small" disabled={!canWrite} onClick={() => setEditing(record)}>
          编辑
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Card title="通知与订阅消息" loading={loading}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
          message="此处仅配置非敏感模板参数;小程序 appid/secret 等密钥类参数不提供在线配置(R-SYS-02)。实际发送依赖订阅消息链路(F-ADR-011),接通后按模板发送。每次修改留审计日志。"
        />
        <Table
          columns={columns}
          dataSource={rows}
          loading={loading}
          rowKey={(record: TemplateRow) => record.scenario.key}
          size="middle"
          pagination={false}
          locale={{ emptyText: '暂无模板配置' }}
        />
        {!canWrite && (
          <Typography.Text type="warning" style={{ display: 'block', marginTop: 16 }}>
            当前账号无系统配置修改权限(SYSTEM_CONFIG_WRITE),仅可查看。
          </Typography.Text>
        )}
      </Card>

      <Card title="送水 App 消息通知" style={{ marginTop: 16 }} loading={loading}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
          message="送水工端实时推送(F-NTF-003)的任务节点开关与离线/弱网轮询间隔;APNs/FCM 等推送凭证为密钥类,不在此在线配置(R-NTF-05)。推送链路接通后按此配置触达,每次修改留审计日志。"
        />
        <Table
          columns={deliveryColumns}
          dataSource={deliveryRows}
          loading={loading}
          rowKey={(record: { scenario: DeliveryPushScenario; enabled: boolean }) => record.scenario.key}
          size="middle"
          pagination={false}
          locale={{ emptyText: '暂无推送场景配置' }}
        />
        <Form layout="inline" style={{ marginTop: 16 }}>
          <Form.Item label="离线/弱网轮询间隔(秒)" required>
            <InputNumber
              min={5}
              max={300}
              precision={0}
              value={pollInterval}
              onChange={(value) => setPollInterval(value ?? undefined)}
              disabled={!canWrite}
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" onClick={handleSaveDeliveryPush} loading={saving} disabled={!canWrite}>
              保存
            </Button>
          </Form.Item>
        </Form>
        <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
          实时通道断开时,送水工端按此间隔轮询补拉任务事件,推送与轮询按幂等键合并去重(R-NTF-02/03,默认 30s)。
        </Typography.Paragraph>
      </Card>

      <Modal
        title={`编辑模板${editing ? `:${editing.scenario.label}` : ''}`}
        open={!!editing}
        onOk={handleSaveEdit}
        onCancel={() => setEditing(null)}
        okText="保存"
        cancelText="取消"
        confirmLoading={saving}
        okButtonProps={{ disabled: !canWrite }}
      >
        {editing && (
          <Form form={form} layout="vertical">
            <Form.Item
              name="templateId"
              label="模板 ID"
              extra="粘贴自微信公众平台订阅消息模板 ID"
              rules={[
                { required: true, message: '模板 ID 不能为空' },
                { pattern: TEMPLATE_ID_PATTERN, message: '模板 ID 格式不正确' },
              ]}
            >
              <Input maxLength={64} placeholder="请输入微信订阅消息模板 ID" disabled={!canWrite} />
            </Form.Item>
            <Form.Item name="enabled" label="启用" valuePropName="checked">
              <Switch disabled={!canWrite} checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
            <Form.Item label="业务场景说明">
              <Typography.Text type="secondary">{editing.scenario.description}</Typography.Text>
            </Form.Item>
          </Form>
        )}
      </Modal>
    </div>
  );
};

export default SystemNotificationsConfig;
