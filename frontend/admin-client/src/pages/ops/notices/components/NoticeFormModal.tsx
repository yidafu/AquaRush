import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Switch, DatePicker, Spin } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import type { Notice, NoticeType } from '@aquarush/common';
import { useNotice } from '../../../../services/ops-graphql';
import { NOTICE_TYPE_LABELS } from '../constants';

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
  record: Notice | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: NoticeFormPayload, record: Notice | null) => void;
}

// 公告类型选项
export const NOTICE_TYPE_OPTIONS = Object.entries(NOTICE_TYPE_LABELS).map(([value, label]) => ({ value, label }));

const NoticeFormModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<FormValues>();
  const [type, setType] = useState<NoticeType>('PLATFORM_NOTICE');
  const [loadNotice, { data, error }] = useNotice();

  // 编辑模式:打开弹窗后按 ID 拉取公告详情(回填数据源)
  useEffect(() => {
    if (!open || !record) return;
    loadNotice({ variables: { id: record.id } });
  }, [open, record, loadNotice]);

  // 详情返回后再回填;查询失败退化为用列表记录回填;新建模式给默认值
  useEffect(() => {
    if (!open) return;
    if (!record) {
      form.resetFields();
      form.setFieldsValue({ type: 'PLATFORM_NOTICE', pinned: false });
      setType('PLATFORM_NOTICE');
      return;
    }
    const detail = data?.notice;
    const source = detail && detail.id === record.id ? detail : error ? record : null;
    if (!source) return; // 详情未就绪
    form.setFieldsValue({
      title: source.title,
      content: source.content,
      type: source.type,
      pinned: source.pinned,
      timeWindow:
        source.startAt && source.endAt ? [dayjs(source.startAt), dayjs(source.endAt)] : undefined,
    });
    setType(source.type);
  }, [open, record, data, error, form]);

  // 编辑模式且详情尚未返回(且未失败)时不可提交
  const detailReady = !record || !!error || (!!data?.notice && data.notice.id === record.id);

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
    <Modal
      title={record ? '编辑公告' : '新建公告'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={submitting}
      destroyOnClose
      width={560}
      okButtonProps={{ disabled: !detailReady }}
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
          <RangePicker showTime style={{ width: '100%' }} />
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
    </Modal>
  );
};

export default NoticeFormModal;
