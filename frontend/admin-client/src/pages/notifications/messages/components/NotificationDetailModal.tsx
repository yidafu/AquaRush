import React from 'react';
import { Modal, Descriptions, Tag, Button, Popconfirm, Space } from 'antd';
import dayjs from 'dayjs';
import type { NotificationRecord } from './constants';
import { TYPE_LABELS, RECEIVER_LABELS, CHANNEL_STATUS_LABELS } from './constants';

export interface NotificationDetailModalProps {
  record: NotificationRecord | null;
  loading: boolean;
  open: boolean;
  resending?: boolean;
  onClose: () => void;
  onResend?: (record: NotificationRecord) => void;
}

/** 消息详情弹窗(notifications/messages/index.tsx 拆分,含通道状态与失败消息手动重发 F-NTF-006) */
const NotificationDetailModal: React.FC<NotificationDetailModalProps> = ({
  record,
  loading,
  open,
  resending,
  onClose,
  onResend,
}) => {
  const canResend = !!record && (record.channelStatus === 'FAILED' || record.channelStatus === 'PENDING');

  return (
    <Modal
      title='消息详情'
      open={open}
      onCancel={onClose}
      footer={
        canResend ? (
          <Space>
            <Button onClick={onClose}>关闭</Button>
            <Popconfirm
              title='确认重发?'
              description='将重新尝试向该用户发送微信订阅消息(仅超管)'
              onConfirm={() => onResend?.(record!)}
              okText='重发'
              cancelText='取消'
            >
              <Button type='primary' loading={resending}>
                重发
              </Button>
            </Popconfirm>
          </Space>
        ) : null
      }
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
          <Descriptions.Item label='通道状态'>
            {record.channelStatus ? (
              <Tag color={CHANNEL_STATUS_LABELS[record.channelStatus]?.color}>
                {CHANNEL_STATUS_LABELS[record.channelStatus]?.label ?? record.channelStatus}
              </Tag>
            ) : (
              '-'
            )}
          </Descriptions.Item>
          {record.wxMessageId && (
            <Descriptions.Item label='微信消息ID'>{record.wxMessageId}</Descriptions.Item>
          )}
          {record.errorMessage && (
            <Descriptions.Item label='失败原因'>{record.errorMessage}</Descriptions.Item>
          )}
          <Descriptions.Item label='发送时间'>
            {dayjs(record.createdAt).format('YYYY-MM-DD HH:mm:ss')}
          </Descriptions.Item>
          {record.sentAt && (
            <Descriptions.Item label='通道处理时间'>
              {dayjs(record.sentAt).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
          )}
          <Descriptions.Item label='已读时间'>
            {record.readAt ? dayjs(record.readAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </Descriptions.Item>
        </Descriptions>
      ) : (
        <div style={{ textAlign: 'center', padding: 24 }}>未找到该消息</div>
      )}
    </Modal>
  );
};

export default NotificationDetailModal;
