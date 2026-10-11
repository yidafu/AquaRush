import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, DatePicker, message } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import { centsToYuan, yuanToCents } from '@aquarush/common';
import type { FlashSaleActivityVo } from '@aquarush/common';
import ProductSelect from '../../../../components/ProductSelect';

const { RangePicker } = DatePicker;

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
});

// 提交给 mutation 的表单载荷(金额已由元转分,时间为 ISO 字符串)
export interface FlashSaleFormPayload {
  name: string;
  productId: string;
  salePriceCents: number;
  validFrom: string;
  validTo: string;
}

interface FormValues {
  name: string;
  productId?: string;
  salePriceYuan?: number;
  timeWindow?: [Dayjs, Dayjs];
}

interface Props {
  open: boolean;
  record: FlashSaleActivityVo | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: FlashSaleFormPayload, record: FlashSaleActivityVo | null) => void;
}

const FlashSaleFormModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<FormValues>();

  useEffect(() => {
    if (!open) return;
    if (!record) {
      form.resetFields();
      return;
    }
    form.setFieldsValue({
      name: record.name,
      productId: String(record.productId),
      salePriceYuan: centsToYuan(record.salePriceCents),
      timeWindow: [dayjs(record.validFrom), dayjs(record.validTo)],
    });
  }, [open, record, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    if (!values.productId) {
      message.error('请选择商品');
      return;
    }
    if (values.salePriceYuan === undefined) {
      message.error('请输入特价');
      return;
    }
    if (!values.timeWindow) {
      message.error('请选择活动时间');
      return;
    }
    const [validFrom, validTo] = values.timeWindow;
    const payload: FlashSaleFormPayload = {
      name: values.name,
      productId: values.productId,
      salePriceCents: yuanToCents(values.salePriceYuan),
      validFrom: validFrom.startOf('second').format('YYYY-MM-DDTHH:mm:ss'),
      validTo: validTo.startOf('second').format('YYYY-MM-DDTHH:mm:ss'),
    };
    onSubmit(payload, record);
  };

  return (
// TODO(Phase4): 弹窗外壳可复用 FormModal + useFormModal(编辑X/新建X/confirmLoading/destroyOnClose),见 ops/banners/BannerFormModal 迁移示例;字段与 payload 转换保留本页。
    <Modal
      title={record ? '编辑特价活动' : '新建特价活动'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={submitting}
      destroyOnClose
      width={560}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="name"
          label="活动名称"
          rules={[
            { required: true, message: '请输入活动名称' },
            { max: 100, message: '活动名称不能超过100个字' },
          ]}
        >
          <Input placeholder="如:夏季特惠" maxLength={100} showCount />
        </Form.Item>
        <Form.Item name="productId" label="商品" rules={[{ required: true, message: '请选择商品' }]}>
          <ProductSelect />
        </Form.Item>
        <Form.Item
          name="salePriceYuan"
          label="特价(元)"
          rules={[
            { required: true, message: '请输入特价' },
            {
              validator: (_, value: number | undefined) =>
                value !== undefined && value >= 0.01 && value <= 99999
                  ? Promise.resolve()
                  : Promise.reject(new Error('特价无效(0.01-99999元)')),
            },
          ]}
        >
          <InputNumber min={0.01} max={99999} precision={2} placeholder="如 15" {...stylex.props(styles.fullWidth)} />
        </Form.Item>
        <Form.Item name="timeWindow" label="活动时间" rules={[{ required: true, message: '请选择活动时间' }]}>
          <RangePicker showTime {...stylex.props(styles.fullWidth)} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default FlashSaleFormModal;
