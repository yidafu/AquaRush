import React, { useState } from 'react';
import { Card, Image, Space, Tag, Button, Modal, Input, message, Descriptions } from 'antd';
import { CarOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons';
import { useMutation } from '@apollo/client';
import { REVIEW_DELIVERY_PHOTO_MUTATION } from '../../../../graphql/mutations/order.graphql';
import { useOrderDetail, formatDateTime } from '../OrderDetailContext';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  card: {
    marginBottom: '16px',
  },
  photo: {
    objectFit: 'cover',
    borderRadius: '8px',
  },
  reviewDesc: {
    marginTop: '12px',
  },
  actions: {
    marginTop: '12px',
  },
});

// 核验状态展示配置(G23/F-ORD-021)
const REVIEW_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待核验', color: 'processing' },
  APPROVED: { text: '已通过', color: 'success' },
  REJECTED: { text: '已驳回', color: 'error' },
};

const DeliveryPhotosCard: React.FC = () => {
  const { order, refreshOrder } = useOrderDetail();
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectRemark, setRejectRemark] = useState('');
  const [reviewing, setReviewing] = useState(false);

  const [reviewDeliveryPhoto] = useMutation(REVIEW_DELIVERY_PHOTO_MUTATION, {
    onError: (err) => {
      console.error('照片核验失败:', err);
      message.error('照片核验失败: ' + err.message);
    },
  });

  if (!order || !order.deliveryPhotos || order.deliveryPhotos.length === 0) {
    return null;
  }

  const reviewStatus = order.deliveryPhotoReviewStatus || 'PENDING';
  const statusInfo = REVIEW_STATUS_MAP[reviewStatus] || { text: reviewStatus, color: 'default' };

  const doReview = async (action: 'APPROVE' | 'REJECT', remark?: string) => {
    setReviewing(true);
    try {
      await reviewDeliveryPhoto({
        variables: {
          orderId: order.id,
          action,
          remark: remark || null,
        },
      });
      message.success(action === 'APPROVE' ? '照片核验通过' : '照片已驳回');
      setRejectModalOpen(false);
      setRejectRemark('');
      refreshOrder();
    } catch {
      // onError 已提示
    } finally {
      setReviewing(false);
    }
  };

  const handleReject = () => {
    if (!rejectRemark.trim()) {
      message.warning('请填写驳回原因');
      return;
    }
    doReview('REJECT', rejectRemark.trim());
  };

  return (
    <Card
      title={
        <Space>
          <CarOutlined />
          交付照片
          <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
        </Space>
      }
      {...stylex.props(styles.card)}
    >
      <Space size="middle" wrap>
        {order.deliveryPhotos.map((photo, index) => (
          <Image
            key={index}
            src={photo}
            alt={`交付照片 ${index + 1}`}
            width={120}
            height={120}
            {...stylex.props(styles.photo)}
          />
        ))}
      </Space>

      {/* 核验留痕信息 */}
      {order.deliveryPhotoReviewedAt && (
        <Descriptions size="small" column={{ xs: 1, sm: 3 }} {...stylex.props(styles.reviewDesc)}>
          <Descriptions.Item label="核验时间">{formatDateTime(order.deliveryPhotoReviewedAt)}</Descriptions.Item>
          <Descriptions.Item label="核验人ID">{order.deliveryPhotoReviewedBy || '-'}</Descriptions.Item>
          <Descriptions.Item label="核验备注">{order.deliveryPhotoReviewRemark || '-'}</Descriptions.Item>
        </Descriptions>
      )}

      {/* 核验操作(G23:通过/驳回,可重复核验纠错) */}
      <Space {...stylex.props(styles.actions)}>
        <Button
          type="primary"
          icon={<CheckOutlined />}
          loading={reviewing}
          onClick={() => doReview('APPROVE')}
        >
          通过
        </Button>
        <Button danger icon={<CloseOutlined />} loading={reviewing} onClick={() => setRejectModalOpen(true)}>
          驳回
        </Button>
      </Space>

      <Modal
        title="驳回配送照片"
        open={rejectModalOpen}
        onCancel={() => setRejectModalOpen(false)}
        onOk={handleReject}
        okText="确认驳回"
        okButtonProps={{ danger: true }}
        confirmLoading={reviewing}
      >
        <Input.TextArea
          rows={3}
          maxLength={500}
          value={rejectRemark}
          onChange={(e) => setRejectRemark(e.target.value)}
          placeholder="请填写驳回原因(核验留痕,必填)"
        />
      </Modal>
    </Card>
  );
};

export default DeliveryPhotosCard;
