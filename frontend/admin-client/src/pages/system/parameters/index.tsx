import React, { useState, useEffect, useCallback } from 'react';
import { Card, Tabs, Table, Tag, Button, Space, Popconfirm, Modal, Form, Input, InputNumber, Switch, Select, Typography } from 'antd';
import type { SystemConfigVo } from '@aquarush/common';
import { SystemConfigGroup, SystemSettingType } from '@aquarush/common';
import { useSystemConfigs, useSaveSystemConfigs, useRestoreSystemConfig } from '../../../services/system-graphql';
import SystemConfigAuditLog from '../components/SystemConfigAuditLog';

// 系统参数管理页(F-SYS-001):按参数组分 Tab 浏览,类型化编辑,恢复默认,修改记录审计(R-SYS-01)
// 仅超级管理员可读写(SYSTEM_CONFIG_READ/WRITE,R-SYS-03),前端隐藏/禁用为 UX 层,后端 @PreAuthorize 强制

const GROUP_TABS = [
  { key: SystemConfigGroup.ORDER_AND_FULFILLMENT, label: '订单与履约' },
  { key: SystemConfigGroup.INVENTORY, label: '库存' },
  { key: SystemConfigGroup.SECURITY, label: '安全' },
  { key: SystemConfigGroup.NOTIFICATION, label: '通知' },
  { key: SystemConfigGroup.DATA_MAINTENANCE, label: '数据维护' },
];

const TYPE_LABELS: Record<SystemSettingType, { label: string; color: string }> = {
  [SystemSettingType.NUMBER]: { label: '数值', color: 'blue' },
  [SystemSettingType.BOOLEAN]: { label: '布尔', color: 'purple' },
  [SystemSettingType.STRING]: { label: '文本', color: 'green' },
  [SystemSettingType.ENUM]: { label: '枚举', color: 'orange' },
};

const SystemParameters: React.FC = () => {
  const canWrite = JSON.parse(localStorage.getItem('userInfo') || '{}').role === 'SUPER_ADMIN';
  const [loadConfigs, { data, loading }] = useSystemConfigs();
  const [saveConfigs, { loading: saving }] = useSaveSystemConfigs();
  const [restoreConfig] = useRestoreSystemConfig();
  const [activeGroup, setActiveGroup] = useState<SystemConfigGroup>(SystemConfigGroup.ORDER_AND_FULFILLMENT);
  const [editing, setEditing] = useState<SystemConfigVo | null>(null);
  const [form] = Form.useForm();

  const loadGroup = useCallback(
    (group: SystemConfigGroup) => {
      loadConfigs({ variables: { group } });
    },
    [loadConfigs],
  );

  useEffect(() => {
    // 「修改记录」Tab 非参数组,不触发加载
    if (GROUP_TABS.some((tab) => tab.key === activeGroup)) {
      loadGroup(activeGroup);
    }
  }, [activeGroup, loadGroup]);

  // 编辑弹窗打开时回填当前值(按类型)
  useEffect(() => {
    if (!editing) return;
    form.setFieldsValue({
      settingValue: editing.settingType === SystemSettingType.BOOLEAN
        ? editing.settingValue === 'true'
        : editing.settingType === SystemSettingType.NUMBER
          ? Number(editing.settingValue)
          : editing.settingValue,
    });
  }, [editing, form]);

  const handleEdit = (record: SystemConfigVo) => {
    setEditing(record);
  };

  const handleSaveEdit = async () => {
    if (!editing) return;
    const values = await form.validateFields();
    const rawValue = editing.settingType === SystemSettingType.BOOLEAN
      ? String(values.settingValue)
      : editing.settingType === SystemSettingType.NUMBER
        ? String(values.settingValue)
        : values.settingValue;
    await saveConfigs({
      variables: { input: [{ settingKey: editing.settingKey, settingValue: rawValue }] },
    });
    setEditing(null);
  };

  const handleRestore = async (record: SystemConfigVo) => {
    await restoreConfig({ variables: { settingKey: record.settingKey } });
  };

  const renderValue = (record: SystemConfigVo) => {
    if (record.settingValue == null) {
      return <Typography.Text type="secondary">未设置</Typography.Text>;
    }
    if (record.settingType === SystemSettingType.BOOLEAN) {
      return record.settingValue === 'true' ? <Tag color="green">开</Tag> : <Tag color="default">关</Tag>;
    }
    return <Typography.Text>{record.settingValue}</Typography.Text>;
  };

  const columns = [
    {
      title: '参数名',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      render: (desc: string | null | undefined) => desc || '-',
    },
    {
      title: '参数键',
      dataIndex: 'settingKey',
      key: 'settingKey',
      width: 240,
      render: (key: string) => <Typography.Text code>{key}</Typography.Text>,
    },
    {
      title: '类型',
      dataIndex: 'settingType',
      key: 'settingType',
      width: 90,
      render: (type: SystemSettingType) => {
        const info = TYPE_LABELS[type] || { label: type, color: 'default' };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: '当前值',
      dataIndex: 'settingValue',
      key: 'settingValue',
      width: 140,
      render: (_: string, record: SystemConfigVo) => renderValue(record),
    },
    {
      title: '默认值',
      dataIndex: 'defaultValue',
      key: 'defaultValue',
      width: 120,
      render: (value: string | null | undefined) => value ?? '-',
    },
    {
      title: '说明',
      dataIndex: 'description',
      key: 'descriptionInfo',
      ellipsis: true,
    },
    {
      title: '修改人',
      dataIndex: 'updatedBy',
      key: 'updatedBy',
      width: 110,
      render: (name: string | null | undefined) => name || '-',
    },
    {
      title: '修改时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 170,
      render: (date: string) => (date ? new Date(date).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      key: 'actions',
      width: 140,
      render: (_: unknown, record: SystemConfigVo) => (
        <Space size="small">
          <Button type="link" size="small" disabled={!canWrite} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定恢复默认值吗?"
            okText="确定"
            cancelText="取消"
            disabled={!canWrite}
            onConfirm={() => handleRestore(record)}
          >
            <Button type="link" size="small" disabled={!canWrite}>
              恢复默认
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const renderEditControl = (record: SystemConfigVo) => {
    const commonProps = { disabled: !canWrite };
    switch (record.settingType) {
      case SystemSettingType.NUMBER:
        return (
          <Form.Item
            name="settingValue"
            label="参数值"
            rules={[
              { required: true, message: '请输入参数值' },
              {
                validator: (_, value) => {
                  if (value == null || value === '') return Promise.reject(new Error('参数值格式不正确'));
                  const min = record.minValue == null ? Number.NEGATIVE_INFINITY : Number(record.minValue);
                  const max = record.maxValue == null ? Number.POSITIVE_INFINITY : Number(record.maxValue);
                  if (value < min || value > max) return Promise.reject(new Error('参数值超出范围'));
                  return Promise.resolve();
                },
              },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              precision={0}
              min={record.minValue == null ? undefined : Number(record.minValue)}
              max={record.maxValue == null ? undefined : Number(record.maxValue)}
              {...commonProps}
            />
          </Form.Item>
        );
      case SystemSettingType.BOOLEAN:
        return (
          <Form.Item name="settingValue" label="参数值" valuePropName="checked">
            <Switch {...commonProps} />
          </Form.Item>
        );
      case SystemSettingType.ENUM:
        return (
          <Form.Item
            name="settingValue"
            label="参数值"
            rules={[{ required: true, message: '参数值格式不正确' }]}
          >
            <Select
              {...commonProps}
              options={(record.options || '').split(',').map((opt) => ({ value: opt, label: opt }))}
            />
          </Form.Item>
        );
      default:
        return (
          <Form.Item
            name="settingValue"
            label="参数值"
            rules={[
              { required: true, message: '参数值格式不正确' },
              { max: 255, message: '参数值不能超过255个字符' },
            ]}
          >
            <Input maxLength={255} {...commonProps} />
          </Form.Item>
        );
    }
  };

  return (
    <Card title="系统参数" loading={loading}>
      <Tabs
        activeKey={activeGroup}
        onChange={(key) => setActiveGroup(key as SystemConfigGroup)}
        items={[
          ...GROUP_TABS.map((tab) => ({
            key: tab.key,
            label: tab.label,
            children: (
              <Table
                columns={columns}
                dataSource={data?.systemConfigs || []}
                loading={loading}
                rowKey="settingKey"
                size="middle"
                pagination={false}
                locale={{ emptyText: '该分组暂无可配置参数' }}
              />
            ),
          })),
          {
            key: 'audit',
            label: '修改记录',
            children: <SystemConfigAuditLog />,
          },
        ]}
      />

      <Modal
        title={`编辑参数${editing ? `：${editing.description || editing.settingKey}` : ''}`}
        open={!!editing}
        onOk={handleSaveEdit}
        onCancel={() => setEditing(null)}
        okText="保存"
        cancelText="取消"
        confirmLoading={saving}
        okButtonProps={{ disabled: !canWrite }}
      >
        {editing && (
          <Form form={form} layout="vertical" preserve={false}>
            {renderEditControl(editing)}
            <Form.Item label="参数键">
              <Typography.Text code>{editing.settingKey}</Typography.Text>
            </Form.Item>
            <Form.Item label="说明">
              <Typography.Text type="secondary">{editing.description || '暂无说明'}</Typography.Text>
            </Form.Item>
            {!canWrite && (
              <Typography.Text type="warning">当前账号无系统配置修改权限(SYSTEM_CONFIG_WRITE),仅可查看。</Typography.Text>
            )}
          </Form>
        )}
      </Modal>
    </Card>
  );
};

export default SystemParameters;
