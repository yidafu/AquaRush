import React from 'react';
import { Table, Tag, Button, Tooltip } from '@/ui';
import type { ColumnsType } from '@/ui';
import type { LogPartitionVo } from '@aquarush/common';
import { formatBytes } from './constants';

export interface PartitionTableProps {
  partitions: LogPartitionVo[];
  canWrite: boolean;
  backingUp: boolean;
  onBackup: (partition: LogPartitionVo) => void;
}

/** 日志分区表(按月分区,system/data/index.tsx 拆分):状态 + 离线备份操作 */
const PartitionTable: React.FC<PartitionTableProps> = ({
  partitions,
  canWrite,
  backingUp,
  onBackup,
}) => {
  const partitionStatus = (partition: LogPartitionVo): { label: string; color: string } => {
    if (partition.isDefault) return { label: '默认分区', color: 'default' };
    if (partition.isCurrentMonth) return { label: '当前月', color: 'blue' };
    if (partition.expired) return { label: '已超期', color: 'orange' };
    return { label: '未超期', color: 'green' };
  };

  const backupDisabledReason = (partition: LogPartitionVo): string => {
    if (!canWrite) return '无系统配置修改权限(SYSTEM_CONFIG_WRITE)';
    if (partition.isDefault) return 'DEFAULT 兜底分区不可备份';
    if (partition.isCurrentMonth) return '当前月分区仍在写入,不可备份';
    if (!partition.expired) return '未达到保留期,暂不可备份';
    return '该分区已备份';
  };

  const columns: ColumnsType<LogPartitionVo> = [
    { title: '分区名', dataIndex: 'partitionName', width: 220 },
    { title: '月份', dataIndex: 'partitionMonth', width: 120, render: (v: string | null) => v || '-' },
    { title: '行数', dataIndex: 'rowCount', width: 120, render: (v: number) => v.toLocaleString() },
    { title: '大小', dataIndex: 'sizeBytes', width: 120, render: (v: number) => formatBytes(v) },
    {
      title: '状态',
      key: 'status',
      width: 120,
      render: (_, record) => {
        const s = partitionStatus(record);
        return <Tag color={s.color}>{s.label}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_, record) => (
        <Tooltip title={record.backupable ? '' : backupDisabledReason(record)}>
          <Button
            type="link"
            size="small"
            danger
            disabled={!record.backupable || !canWrite || backingUp}
            onClick={() => onBackup(record)}
          >
            离线备份
          </Button>
        </Tooltip>
      ),
    },
  ];

  return (
    <Table<LogPartitionVo>
      rowKey="partitionName"
      columns={columns}
      dataSource={partitions}
      pagination={false}
      size="middle"
      locale={{ emptyText: '暂无分区' }}
    />
  );
};

export default PartitionTable;
