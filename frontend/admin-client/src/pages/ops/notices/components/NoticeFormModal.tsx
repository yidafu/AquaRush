import React, { useState } from 'react';
import { Form, Input, Select, Switch, DatePicker, Spin } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import { NoticeType } from '@aquarush/common';
import type { NoticeVo } from '@aquarush/common';
import { useNotice } from '../../../../services/ops-graphql';
import { NOTICE_TYPE_LABELS } from '../constants';
import FormModal from '@/components/FormModal';
import { useDetailBackfill } from '@/hooks/useDetailBackfill';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
});

const { RangePicker } = DatePicker;

// 提交给 mutation 的表单载荷(LocalDateTime 以 ISO 字符串提交,空时间窗 = null = 长期)
export interface NoticeFormPayload {
  title: string;
  content: string;
  type: NoticeType;
  pinned: boolean;
  startAt: string | null;
  endAt: string | null;
}

interface FormValues {
  title: string;
  content: string;
  type: NoticeType;
  pinned: boolean;
  timeWindow?: [Dayjs, Dayjs];
}

interface Props {
  open: boolean;
  record: NoticeVo | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: NoticeFormPayload, record: NoticeVo | null) => void;
}

// 公告类型选项
export const NOTICE_TYPE_OPTIONS = Object.entries(NOTICE_TYPE_LABELS).map(([value, label]) => ({ value, label }));

const NoticeFormModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<FormValues>();
  const [, setType] = useState<NoticeType>(NoticeType.PLATFORM_NOTICE);
  const [loadNotice, { data, error }] = useNotice();

  // 编辑模式:按 ID 拉详情回填 + detailReady 禁提交(useDetailBackfill);字段映射保留在本页
  const { ready: detailReady } = useDetailBackfill({
    open,
    record,
    load: (id: string) => loadNotice({ variables: { id } }),
    detail: data?.notice,
    detailError: error,
    applyDefaults: () => {
      form.resetFields();
      form.setFieldsValue({ type: NoticeType.PLATFORM_NOTICE, pinned: false });
      setType(NoticeType.PLATFORM_NOTICE);
    },
    backfill: (source) => {
      form.setFieldsValue({
        title: source.title,
        content: source.content,
        type: source.type,
        pinned: source.pinned,
        timeWindow:
          source.startAt && source.endAt ? [dayjs(source.startAt), dayjs(source.endAt)] : undefined,
      });
      setType(source.type);
    },
  });

  const handleOk = async () => {
    const values = await form.validateFields();
    const [startAt, endAt] = values.timeWindow ?? [undefined, undefined];
    onSubmit(
      {
        title: values.title,
        content: values.content,
        type: values.type,
        pinned: values.pinned,
        // RangePicker 首尾时间归一到秒,保证「开始 < 结束」语义
        startAt: startAt ? startAt.startOf('second').format('YYYY-MM-DDTHH:mm:ss') : null,
        endAt: endAt ? endAt.startOf('second').format('YYYY-MM-DDTHH:mm:ss') : null,
      },
      record,
    );
  };

  return (
    <FormModal
      open={open}
      record={record}
      submitting={submitting}
      onCancel={onCancel}
      onOk={handleOk}
      noun="公告"
      width={560}
      okDisabled={!detailReady}
    >
      <Spin spinning={!detailReady} tip="加载公告详情...">
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="title"
          label="标题"
          rules={[
            { required: true, message: '公告标题不能为空' },
            { max: 50, message: '公告标题不能超过50个字' },
          ]}
        >
          <Input placeholder="请输入公告标题" maxLength={50} showCount />
        </Form.Item>
        <Form.Item
          name="content"
          label="内容"
          rules={[
            { required: true, message: '公告内容不能为空' },
            { max: 2000, message: '公告内容不能超过2000个字' },
          ]}
        >
          <Input.TextArea placeholder="请输入公告内容" maxLength={2000} showCount rows={6} />
        </Form.Item>
        <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择公告类型' }]}>
          <Select options={NOTICE_TYPE_OPTIONS} onChange={(value: NoticeType) => setType(value)} />
        </Form.Item>
        <Form.Item name="timeWindow" label="时间窗(空 = 长期)">
          <RangePicker showTime {...stylex.props(styles.fullWidth)} />
        </Form.Item>
        <Form.Item
          name="pinned"
          label="置顶"
          valuePropName="checked"
          extra="置顶公告在公告栏最前展示,仅已发布公告可置顶"
        >
          <Switch />
        </Form.Item>
      </Form>
      </Spin>
    </FormModal>
  );
};

export default NoticeFormModal;
