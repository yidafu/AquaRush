import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Tag, Image, Space, Typography, Descriptions } from '@aquarush/admin-ui';
import {
  FEEDBACK_STATUS_LABELS,
  FEEDBACK_STATUS_OPTIONS,
  FEEDBACK_STATUS_TONE,
  FEEDBACK_TYPE_LABELS,
  FeedbackStatus,
  formatDateTime,
} from '@aquarush/common';
import type { FeedbackVo } from '@aquarush/common';
import { getToneColor } from '../../../../constants/status';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  desc: {
    marginBottom: '16px',
  },
  blockTitle: {
    marginBottom: '8px',
  },
  contentBlock: {
    whiteSpace: 'pre-wrap',
    // 反馈内容底色 #fafafa 为本弹窗局部色值,共享 token 无对应项
    backgroundColor: '#fafafa',
    paddingTop: '12px',
    paddingRight: '12px',
    paddingBottom: '12px',
    paddingLeft: '12px',
    borderRadius: '6px',
  },
  gallery: {
    marginBottom: '16px',
  },
  thumb: {
    objectFit: 'cover',
    borderRadius: '6px',
  },
  handledNote: {
    display: 'block',
    marginBottom: '12px',
  },
});

const { TextArea } = Input;

export interface ReviewPayload {
  handleResult?: string;
  status: FeedbackStatus;
}

interface Props {
  open: boolean;
  record: FeedbackVo | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: ReviewPayload, record: FeedbackVo) => void;
}

const FeedbackDetailModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<ReviewPayload>();
  const [currentStatus, setCurrentStatus] = useState<FeedbackStatus>(FeedbackStatus.PENDING);

  useEffect(() => {
    if (open && record) {
      form.setFieldsValue({
        handleResult: record.handleResult || undefined,
        status: record.status,
      });
      setCurrentStatus(record.status);
    }
  }, [open, record, form]);

  const handleOk = async () => {
    if (!record) return;
    const values = await form.validateFields();
    onSubmit(
      {
        handleResult: values.handleResult || undefined,
        status: values.status,
      },
      record,
    );
  };

  return (
    <Modal
      title="反馈详情"
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={submitting}
      destroyOnClose
      width={640}
      okText="保存处理"
    >
      {record && (
        <>
          <Descriptions size="small" column={2} {...stylex.props(styles.desc)}>
            <Descriptions.Item label="类型">
              <Tag color="blue">{FEEDBACK_TYPE_LABELS[record.feedbackType] ?? record.feedbackType}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={getToneColor(FEEDBACK_STATUS_TONE[record.status])}>
                {FEEDBACK_STATUS_LABELS[record.status] ?? record.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="联系方式">{record.contactInfo || '—'}</Descriptions.Item>
            <Descriptions.Item label="提交时间">{formatDateTime(record.createdAt)}</Descriptions.Item>
          </Descriptions>

          <Typography.Paragraph strong {...stylex.props(styles.blockTitle)}>
            反馈内容
          </Typography.Paragraph>
          <Typography.Paragraph {...stylex.props(styles.contentBlock)}>
            {record.content}
          </Typography.Paragraph>

          {record.images && record.images.length > 0 && (
            <>
              <Typography.Paragraph strong {...stylex.props(styles.blockTitle)}>
                反馈图片({record.images.length})
              </Typography.Paragraph>
              <Image.PreviewGroup>
                <Space wrap {...stylex.props(styles.gallery)}>
                  {record.images.map((url, index) => (
                    <Image key={index} src={url} width={100} height={100} {...stylex.props(styles.thumb)} />
                  ))}
                </Space>
              </Image.PreviewGroup>
            </>
          )}

          {record.handledByName && (
            <Typography.Text type="secondary" {...stylex.props(styles.handledNote)}>
              已于 {record.handledAt ? formatDateTime(record.handledAt) : '—'} 由 {record.handledByName} 处理
            </Typography.Text>
          )}

          <Form form={form} layout="vertical" preserve={false}>
            <Form.Item name="status" label="处理状态" rules={[{ required: true, message: '请选择处理状态' }]}>
              <Select
                options={FEEDBACK_STATUS_OPTIONS}
                onChange={(value: unknown) => setCurrentStatus(value as FeedbackStatus)}
                disabled={currentStatus === 'RESOLVED'}
              />
            </Form.Item>
            <Form.Item name="handleResult" label="处理结果(选填,≤500字)" rules={[{ max: 500, message: '处理结果不能超过500个字' }]}>
              <TextArea rows={3} maxLength={500} showCount placeholder="填写处理结果" />
            </Form.Item>
          </Form>
        </>
      )}
    </Modal>
  );
};

export default FeedbackDetailModal;
