import React from 'react';
import { Descriptions, Tag, Typography } from 'antd';
import type { LogBackupRecordVo } from '@aquarush/common';
import { BACKUP_STATUS_CONFIG, LOG_TABLE_LABEL, formatBytes, formatDateTime } from './constants';
import DetailModal from '@/components/DetailModal';

export interface BackupRecordDetailModalProps {
  record: LogBackupRecordVo | null;
  onClose: () => void;
}

/** 备份记录详情弹窗(system/data/index.tsx 拆分;外壳复用 DetailModal,footer 走插槽传 null) */
const BackupRecordDetailModal: React.FC<BackupRecordDetailModalProps> = ({ record, onClose }) => (
  <DetailModal
    title="备份记录详情"
    open={record != null}
    onClose={onClose}
    width={640}
    footer={null}
  >
    {record && (
      <Descriptions column={2} size="small" bordered>
        <Descriptions.Item label="日志表">{LOG_TABLE_LABEL[record.logTable] || record.logTable}</Descriptions.Item>
        <Descriptions.Item label="分区">{record.partitionName}</Descriptions.Item>
        <Descriptions.Item label="冷表" span={2}>{record.coldTableName}</Descriptions.Item>
        <Descriptions.Item label="行数">{record.rowCount.toLocaleString()}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag color={BACKUP_STATUS_CONFIG[record.status]?.color}>{BACKUP_STATUS_CONFIG[record.status]?.label || record.status}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="文件" span={2}>{record.filePath || '-'}</Descriptions.Item>
        <Descriptions.Item label="文件大小">{formatBytes(record.fileSizeBytes)}</Descriptions.Item>
        <Descriptions.Item label="SHA-256">{record.checksum || '-'}</Descriptions.Item>
        <Descriptions.Item label="操作人">{record.operatorName || '-'}</Descriptions.Item>
        <Descriptions.Item label="开始时间">{formatDateTime(record.startedAt)}</Descriptions.Item>
        <Descriptions.Item label="完成时间">{formatDateTime(record.finishedAt)}</Descriptions.Item>
        {record.errorMessage && (
          <Descriptions.Item label="失败原因" span={2}>
            <Typography.Text type="danger">{record.errorMessage}</Typography.Text>
          </Descriptions.Item>
        )}
      </Descriptions>
    )}
  </DetailModal>
);

export default BackupRecordDetailModal;
