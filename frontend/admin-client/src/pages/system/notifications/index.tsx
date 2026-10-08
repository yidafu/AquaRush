import React, { useState, useEffect, useCallback } from 'react';
import { Alert, Card, Typography, Modal, message } from 'antd';
import { SystemConfigGroup, SystemSettingKeys } from '@aquarush/common';
import type { SystemConfigVo } from '@aquarush/common';
import { useSystemConfigs, useSaveSystemConfigs } from '../../../services/system-graphql';
import {
  WechatTemplateTable,
  TemplateEditModal,
  DeliveryPushConfigCard,
} from './components';
import {
  SCENARIOS,
  DELIVERY_PUSH_SCENARIOS,
  type TemplateRow,
} from './components/constants';

// 通知与订阅消息配置页(F-NTF-005,承接原 F-SYS-005):微信订阅消息模板(用户端) + 送水 App 实时推送配置(送水工端 F-NTF-003)
// 仅超级管理员可读写(SYSTEM_CONFIG_WRITE,R-SYS-03);密钥类(appid/secret、APNs/FCM 凭证)不在此在线配置(R-SYS-02/R-NTF-05);
// 实际发送依赖通知链路(F-NTF-001/G08、F-NTF-003/G44,目标形态)

const SystemNotificationsConfig: React.FC = () => {
  const canWrite = JSON.parse(localStorage.getItem('userInfo') || '{}').role === 'SUPER_ADMIN';
  const [loadConfigs, { data, loading }] = useSystemConfigs();
  const [saveConfigs, { loading: saving }] = useSaveSystemConfigs();
  const [rows, setRows] = useState<TemplateRow[]>([]);
  const [editing, setEditing] = useState<TemplateRow | null>(null);
  const [deliveryRows, setDeliveryRows] = useState<{ scenario: (typeof DELIVERY_PUSH_SCENARIOS)[number]; enabled: boolean }[]>([]);
  const [pollInterval, setPollInterval] = useState<number | undefined>(undefined);

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

  const handleSaveEdit = async (values: { templateId: string; enabled: boolean }) => {
    if (!editing) return;
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

  return (
    <div>
      <Card title="通知与订阅消息" loading={loading}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
          message="此处仅配置非敏感模板参数;小程序 appid/secret 等密钥类参数不提供在线配置(R-SYS-02)。实际发送依赖订阅消息链路(F-ADR-011),接通后按模板发送。每次修改留审计日志。"
        />
        <WechatTemplateTable
          rows={rows}
          loading={loading}
          canWrite={canWrite}
          onEdit={setEditing}
        />
        {!canWrite && (
          <Typography.Text type="warning" style={{ display: 'block', marginTop: 16 }}>
            当前账号无系统配置修改权限(SYSTEM_CONFIG_WRITE),仅可查看。
          </Typography.Text>
        )}
      </Card>

      <DeliveryPushConfigCard
        rows={deliveryRows}
        loading={loading}
        pollInterval={pollInterval}
        saving={saving}
        canWrite={canWrite}
        onToggle={(key, enabled) =>
          setDeliveryRows((prev) =>
            prev.map((row) => (row.scenario.key === key ? { ...row, enabled } : row)),
          )
        }
        onPollIntervalChange={(value) => setPollInterval(value ?? undefined)}
        onSave={handleSaveDeliveryPush}
      />

      <TemplateEditModal
        editing={editing}
        saving={saving}
        canWrite={canWrite}
        onClose={() => setEditing(null)}
        onSubmit={handleSaveEdit}
      />
    </div>
  );
};

export default SystemNotificationsConfig;
