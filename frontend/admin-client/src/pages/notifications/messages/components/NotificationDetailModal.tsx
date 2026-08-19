import React from 'react';
import { Modal, Descriptions, Tag } from 'antd';
import dayjs from 'dayjs';
import type { NotificationRecord } from './constants';
import { TYPE_LABELS, RECEIVER_LABELS } from './constants';

export interface NotificationDetailModalProps {
  record: NotificationRecord | null;
  loading: boolean;
  open: boolean;
  onClose: () => void;
}

/** 消息详情弹窗(notifications/messages/index.tsx 拆分) */
const NotificationDetailModal: React.FC<NotificationDetailModalProps> = ({ record, loading, open, onClose }) => (
  <Modal
    title='消息详情'
    open={open}
    onCancel={onClose}
    footer={null}
    width={560}
  >
    {loading && !record ? (
      <div style={{ textAlign: 'center', padding: 24 }}>加载中...</div>
    ) : record ? (
      <Descriptions column={1} size='middle'>
        <Descriptions.Item label='标题'>{record.title}</Descriptions.Item>
        <Descriptions.Item label='摘要'>{record.summary || '-'}</Descriptions.Item>
        <Descriptions.Item label='类型'>
          <Tag color={TYPE_LABELS[record.type]?.color}>{TYPE_LABELS[record.type]?.label ?? record.type}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label='场景'>{record.scene}</Descriptions.Item>
        <Descriptions.Item label='关联单号'>{record.relatedNo || '-'}</Descriptions.Item>
        <Descriptions.Item label='接收人'>
          {RECEIVER_LABELS[record.receiverType] ?? record.receiverType}
        </Descriptions.Item>
        <Descriptions.Item label='状态'>
          {record.read ? <Tag color='default'>已读</Tag> : <Tag color='red'>未读</Tag>}
        </Descriptions.Item>
        <Descriptions.Item label='发送时间'>
          {dayjs(record.createdAt).format('YYYY-MM-DD HH:mm:ss')}
        </Descriptions.Item>
        <Descriptions.Item label='已读时间'>
          {record.readAt ? dayjs(record.readAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
        </Descriptions.Item>
      </Descriptions>
    ) : (
      <div style={{ textAlign: 'center', padding: 24 }}>未找到该消息</div>
    )}
  </Modal>
);

export default NotificationDetailModal;
