import React from 'react';
import { Table, Tag, Button, Typography } from '@aquarush/admin-ui';
import type { ColumnsType } from '@aquarush/admin-ui';
import type { TemplateRow } from './constants';

export interface WechatTemplateTableProps {
  rows: TemplateRow[];
  loading: boolean;
  canWrite: boolean;
  onEdit: (row: TemplateRow) => void;
}

/** 微信订阅消息模板配置表(system/notifications/index.tsx 拆分) */
const WechatTemplateTable: React.FC<WechatTemplateTableProps> = ({
  rows,
  loading,
  canWrite,
  onEdit,
}) => {
  const columns: ColumnsType<TemplateRow> = [
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
        <Button type="link" size="small" disabled={!canWrite} onClick={() => onEdit(record)}>
          编辑
        </Button>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={rows}
      loading={loading}
      rowKey={(record: TemplateRow) => record.scenario.key}
      size="middle"
      pagination={false}
      locale={{ emptyText: '暂无模板配置' }}
    />
  );
};

export default WechatTemplateTable;
