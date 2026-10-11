import React from 'react';
import {
  Card,
  Alert,
  Table,
  Switch,
  Form,
  InputNumber,
  Button,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import * as stylex from '@stylexjs/stylex';
import type { DeliveryPushScenario } from './constants';

const styles = stylex.create({
  card: {
    marginTop: '16px',
  },
  alert: {
    marginBottom: '24px',
  },
  form: {
    marginTop: '16px',
  },
  hint: {
    marginTop: '12px',
    marginBottom: '0px',
  },
});

export interface DeliveryPushRow {
  scenario: DeliveryPushScenario;
  enabled: boolean;
}

export interface DeliveryPushConfigCardProps {
  rows: DeliveryPushRow[];
  loading: boolean;
  pollInterval: number | undefined;
  saving: boolean;
  canWrite: boolean;
  onToggle: (key: string, enabled: boolean) => void;
  onPollIntervalChange: (value: number | null) => void;
  onSave: () => void;
}

/** 送水 App 消息通知配置卡(实时推送场景开关 + 轮询间隔,system/notifications/index.tsx 拆分,F-NTF-003) */
const DeliveryPushConfigCard: React.FC<DeliveryPushConfigCardProps> = ({
  rows,
  loading,
  pollInterval,
  saving,
  canWrite,
  onToggle,
  onPollIntervalChange,
  onSave,
}) => {
  const columns: ColumnsType<DeliveryPushRow> = [
    {
      title: '推送场景',
      dataIndex: ['scenario', 'label'],
      key: 'label',
      width: 140,
      render: (_: unknown, record: DeliveryPushRow) => (
        <Typography.Text strong>{record.scenario.label}</Typography.Text>
      ),
    },
    {
      title: '状态',
      key: 'enabled',
      width: 130,
      render: (_: unknown, record: DeliveryPushRow) => (
        <Switch
          checked={record.enabled}
          disabled={!canWrite}
          checkedChildren="启用"
          unCheckedChildren="停用"
          onChange={(checked) => onToggle(record.scenario.key, checked)}
        />
      ),
    },
    {
      title: '说明',
      dataIndex: ['scenario', 'description'],
      key: 'description',
    },
  ];

  return (
    <Card title="送水 App 消息通知" {...stylex.props(styles.card)} loading={loading}>
      <Alert
        type="info"
        showIcon
        {...stylex.props(styles.alert)}
        message="送水工端实时推送(F-NTF-003)的任务节点开关与离线/弱网轮询间隔;APNs/FCM 等推送凭证为密钥类,不在此在线配置(R-NTF-05)。推送链路接通后按此配置触达,每次修改留审计日志。"
      />
      <Table
        columns={columns}
        dataSource={rows}
        loading={loading}
        rowKey={(record: DeliveryPushRow) => record.scenario.key}
        size="middle"
        pagination={false}
        locale={{ emptyText: '暂无推送场景配置' }}
      />
      <Form layout="inline" {...stylex.props(styles.form)}>
        <Form.Item label="离线/弱网轮询间隔(秒)" required>
          <InputNumber
            min={5}
            max={300}
            precision={0}
            value={pollInterval}
            onChange={onPollIntervalChange}
            disabled={!canWrite}
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" onClick={onSave} loading={saving} disabled={!canWrite}>
            保存
          </Button>
        </Form.Item>
      </Form>
      <Typography.Paragraph type="secondary" {...stylex.props(styles.hint)}>
        实时通道断开时,送水工端按此间隔轮询补拉任务事件,推送与轮询按幂等键合并去重(R-NTF-02/03,默认 30s)。
      </Typography.Paragraph>
    </Card>
  );
};

export default DeliveryPushConfigCard;
