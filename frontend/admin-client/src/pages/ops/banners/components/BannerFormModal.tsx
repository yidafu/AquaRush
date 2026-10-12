import React, { useState } from 'react';
import { Form, Input, InputNumber, Select, Switch, DatePicker, Spin } from '@aquarush/admin-ui';
import dayjs, { Dayjs } from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import { SingleImageUpload } from '../../../../components/ImageUpload';
import { BannerLinkType } from '@aquarush/common';
import type { BannerVo } from '@aquarush/common';
import { useBanner } from '../../../../services/ops-graphql';
import FormModal from '@/components/FormModal';
import { useDetailBackfill } from '@/hooks/useDetailBackfill';

const { RangePicker } = DatePicker;

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
});

// 提交给 mutation 的表单载荷(LocalDateTime 以 ISO 字符串提交,空时间窗 = null = 长期)
export interface BannerFormPayload {
  imageUrl: string;
  title: string;
  subtitle?: string | null;
  linkType: BannerLinkType;
  linkTarget: string;
  sortOrder: number;
  enabled: boolean;
  startAt: string | null;
  endAt: string | null;
}

interface FormValues {
  imageUrl: string;
  title: string;
  subtitle?: string;
  linkType: BannerLinkType;
  linkTarget: string;
  sortOrder: number;
  enabled: boolean;
  timeWindow?: [Dayjs, Dayjs];
}

interface Props {
  open: boolean;
  record: BannerVo | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: BannerFormPayload, record: BannerVo | null) => void;
}

// 跳转类型文案
export const LINK_TYPE_OPTIONS = [
  { value: 'PRODUCT', label: '商品详情' },
  { value: 'CUSTOM_PAGE', label: '自定义页面' },
];

const BannerFormModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<FormValues>();
  const [linkType, setLinkType] = useState<BannerLinkType>(BannerLinkType.CUSTOM_PAGE);
  const [loadBanner, { data, error }] = useBanner();

  // 编辑模式:按 ID 拉详情回填 + detailReady 禁提交(useDetailBackfill);字段映射保留在本页
  const { ready: detailReady } = useDetailBackfill({
    open,
    record,
    load: (id: string) => loadBanner({ variables: { id } }),
    detail: data?.banner,
    detailError: error,
    applyDefaults: () => {
      form.resetFields();
      form.setFieldsValue({ linkType: BannerLinkType.CUSTOM_PAGE, enabled: false });
      setLinkType(BannerLinkType.CUSTOM_PAGE);
    },
    backfill: (source) => {
      form.setFieldsValue({
        imageUrl: source.imageUrl,
        title: source.title,
        subtitle: source.subtitle || undefined,
        linkType: source.linkType,
        linkTarget: source.linkTarget,
        sortOrder: source.sortOrder,
        enabled: source.enabled,
        timeWindow:
          source.startAt && source.endAt
            ? [dayjs(source.startAt), dayjs(source.endAt)]
            : undefined,
      });
      setLinkType(source.linkType);
    },
  });

  const handleOk = async () => {
    const values = await form.validateFields();
    const [startAt, endAt] = values.timeWindow ?? [undefined, undefined];
    onSubmit(
      {
        imageUrl: values.imageUrl,
        title: values.title,
        subtitle: values.subtitle ?? null,
        linkType: values.linkType,
        linkTarget: values.linkTarget,
        sortOrder: values.sortOrder,
        enabled: values.enabled,
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
      noun="轮播图"
      width={560}
      okDisabled={!detailReady}
    >
      <Spin spinning={!detailReady} tip="加载轮播图详情...">
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="imageUrl"
          label="轮播图片"
          rules={[{ required: true, message: '请上传轮播图片' }]}
          extra="建议尺寸 750×360,支持 jpg/png/webp,≤5MB"
        >
          <SingleImageUpload />
        </Form.Item>
        <Form.Item
          name="title"
          label="标题"
          rules={[
            { required: true, message: '请输入标题' },
            { max: 20, message: '标题不能超过20个字' },
          ]}
        >
          <Input placeholder="请输入标题" maxLength={20} showCount />
        </Form.Item>
        <Form.Item name="subtitle" label="副标题" rules={[{ max: 100, message: '副标题不能超过100个字' }]}>
          <Input placeholder="请输入副标题" maxLength={100} />
        </Form.Item>
        <Form.Item name="linkType" label="跳转类型" rules={[{ required: true, message: '请选择跳转类型' }]}>
          <Select
            options={LINK_TYPE_OPTIONS}
            onChange={(value: unknown) => setLinkType(value as BannerLinkType)}
          />
        </Form.Item>
        <Form.Item
          name="linkTarget"
          label={linkType === 'PRODUCT' ? '跳转商品 ID' : '跳转页面路径'}
          rules={[{ required: true, message: '请输入跳转目标' }]}
          extra={linkType === 'PRODUCT' ? '填写商品 ID,保存时校验商品存在' : '如 /pages/home/index'}
        >
          <Input placeholder={linkType === 'PRODUCT' ? '商品 ID' : '页面路径'} />
        </Form.Item>
        <Form.Item name="sortOrder" label="排序号" rules={[{ required: true, message: '请输入排序号' }]}>
          <InputNumber min={1} {...stylex.props(styles.fullWidth)} placeholder="数值越小越靠前" />
        </Form.Item>
        <Form.Item name="timeWindow" label="时间窗(空 = 长期)">
          <RangePicker showTime {...stylex.props(styles.fullWidth)} />
        </Form.Item>
        <Form.Item name="enabled" label="启用" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
      </Spin>
    </FormModal>
  );
};

export default BannerFormModal;
