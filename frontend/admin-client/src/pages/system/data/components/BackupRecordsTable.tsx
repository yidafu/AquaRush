import React from 'react';
import { Table, Tag, Button } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { LogBackupRecord, LogTableType } from '@aquarush/common';
import { BACKUP_STATUS_CONFIG, LOG_TABLE_LABEL, formatBytes, formatDateTime } from './constants';
import { pagination } from '@/utils/table';

export interface BackupRecordsTableProps {
  records: readonly LogBackupRecord[];
  pageNum: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number, size: number) => void;
  onView: (record: LogBackupRecord) => void;
}

/** 离线备份记录表(system/data/index.tsx 拆分) */
const BackupRecordsTable: React.FC<BackupRecordsTableProps> = ({
  records,
  pageNum,
  pageSize,
  total,
  onPageChange,
  onView,
}) => {
  const columns: ColumnsType<LogBackupRecord> = [
    { title: '时间', dataIndex: 'startedAt', width: 170, render: (v: string) => formatDateTime(v) },
    {
      title: '日志表',
      dataIndex: 'logTable',
      width: 120,
      render: (v: LogTableType) => LOG_TABLE_LABEL[v] || v,
    },
    { title: '分区', dataIndex: 'partitionName', width: 180 },
    { title: '冷表', dataIndex: 'coldTableName', width: 220, ellipsis: true },
    { title: '行数', dataIndex: 'rowCount', width: 100, render: (v: number) => v.toLocaleString() },
    { title: '文件', dataIndex: 'filePath', ellipsis: true, render: (v?: string | null) => v || '-' },
    { title: '大小', dataIndex: 'fileSizeBytes', width: 100, render: (v?: number | null) => formatBytes(v) },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v: LogBackupRecord['status']) => {
        const c = BACKUP_STATUS_CONFIG[v];
        return <Tag color={c.color}>{c.label}</Tag>;
      },
    },
    { title: '操作人', dataIndex: 'operatorName', width: 110, render: (v?: string | null) => v || '-' },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => onView(record)}>
          详情
        </Button>
      ),
    },
  ];

  return (
    <Table<LogBackupRecord>
      rowKey="id"
      columns={columns}
      dataSource={records}
      size="middle"
      pagination={pagination(total, {
        current: pageNum + 1,
        pageSize,
        showSizeChanger: true,
        showQuickJumper: true,
        onChange: (nextPage, nextSize) => {
          onPageChange(nextPage - 1, nextSize);
        },
      })}
    />
  );
};

export default BackupRecordsTable;
