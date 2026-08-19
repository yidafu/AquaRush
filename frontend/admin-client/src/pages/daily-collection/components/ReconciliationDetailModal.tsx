import React, { useEffect } from 'react';
import { Modal, Row, Col, Statistic, Tag, Form, Input, Button } from 'antd';
import { centsToYuan } from '@aquarush/common';
import type { DailyReconciliation } from '../../../types/daily-collection';
import { reconciliationStatusColorMap, reconciliationStatusTextMap } from './constants';

export interface ReconciliationDetailModalProps {
  open: boolean;
  record: DailyReconciliation | null;
  onClose: () => void;
  /** 确认复核(提交备注) */
  onReview: (notes: string) => void;
}

/** 对账详情弹窗(daily-collection/index.tsx 拆分) */
const ReconciliationDetailModal: React.FC<ReconciliationDetailModalProps> = ({
  open,
  record,
  onClose,
  onReview,
}) => {
  const [reviewForm] = Form.useForm();

  // 切换记录时重置复核表单
  useEffect(() => {
    if (record) {
      reviewForm.resetFields();
    }
  }, [record, reviewForm]);

  const handleReviewSubmit = async () => {
    const values = await reviewForm.validateFields();
    onReview(values.reviewNotes);
  };

  return (
    <Modal
      title="对账详情"
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="close" onClick={onClose}>
          关闭
        </Button>,
        record?.status !== 'REVIEWED' && (
          <Button key="review" type="primary" onClick={handleReviewSubmit}>
            确认复核
          </Button>
        ),
      ]}
    >
      {record && (
        <div>
          <Row gutter={16}>
            <Col span={12}>
              <Statistic title="订单总数" value={record.totalOrderCount} />
            </Col>
            <Col span={12}>
              <Statistic
                title="订单金额"
                value={centsToYuan(record.totalOrderAmountCents)}
                precision={2}
                prefix="¥"
              />
            </Col>
          </Row>
          <Row gutter={16} style={{ marginTop: 16 }}>
            <Col span={12}>
              <Statistic
                title="现金收款"
                value={centsToYuan(record.totalCashAmountCents)}
                precision={2}
                prefix="¥"
              />
            </Col>
            <Col span={12}>
              <Statistic
                title="扫码收款"
                value={centsToYuan(record.totalQrCodeAmountCents)}
                precision={2}
                prefix="¥"
              />
            </Col>
          </Row>
          <Row gutter={16} style={{ marginTop: 16 }}>
            <Col span={12}>
              <Statistic
                title="差异金额"
                value={centsToYuan(record.discrepancyAmountCents)}
                precision={2}
                prefix="¥"
                valueStyle={{ color: Math.abs(record.discrepancyAmountCents) <= 100 ? '#52c41a' : '#ff4d4f' }}
              />
            </Col>
            <Col span={12}>
              <Tag color={reconciliationStatusColorMap[record.status]}>
                {reconciliationStatusTextMap[record.status]}
              </Tag>
            </Col>
          </Row>
          {record.status !== 'REVIEWED' && (
            <div style={{ marginTop: 16 }}>
              <Form form={reviewForm}>
                <Form.Item name="reviewNotes" label="复核备注">
                  <Input.TextArea rows={3} placeholder="请输入复核备注" />
                </Form.Item>
              </Form>
            </div>
          )}
          {record.reviewedAt && (
            <div style={{ marginTop: 16, color: '#666' }}>
              复核时间: {record.reviewedAt}
              <br />
              复核备注: {record.reviewNotes}
            </div>
          )}
        </div>
      )}
    </Modal>
  );
};

export default ReconciliationDetailModal;
