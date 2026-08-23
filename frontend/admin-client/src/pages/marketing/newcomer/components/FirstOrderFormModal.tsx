import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select, Radio, DatePicker, message } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import { centsToYuan, yuanToCents } from '@aquarush/common';
import { FirstOrderActivityType, PromotionScope } from '@aquarush/common';
import type { FirstOrderActivity } from '@aquarush/common';
import ProductSelect from '../../../../components/ProductSelect';

const { RangePicker } = DatePicker;

// 提交给 mutation 的表单载荷(金额已由元转分,时间为 ISO 字符串)
export interface FirstOrderFormPayload {
  name: string;
  activityType: FirstOrderActivityType;
  thresholdCents?: number | null;
  discountAmountCents?: number | null;
  discountRatePermille?: number | null;
  scope: PromotionScope;
  productId?: string | null;
  newcomerPriceCents?: number | null;
  validFrom: string;
  validTo: string;
}

interface FormValues {
  name: string;
  activityType: FirstOrderActivityType;
  discountMode: 'AMOUNT' | 'RATE';
  thresholdYuan?: number;
  discountAmountYuan?: number;
  discountRate?: number;
  scope: PromotionScope;
  productId?: string;
  newcomerPriceYuan?: number;
  timeWindow?: [Dayjs, Dayjs];
}

interface Props {
  open: boolean;
  record: FirstOrderActivity | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: FirstOrderFormPayload, record: FirstOrderActivity | null) => void;
}

const ACTIVITY_TYPE_OPTIONS = [
  { value: 'FIRST_ORDER_REDUCE', label: '首单立减(满X减Y或打N折)' },
  { value: 'NEWCOMER_PRICE', label: '新人专享价(指定商品专享单价)' },
];

const SCOPE_OPTIONS = [
  { value: 'ALL', label: '全场' },
  { value: 'PRODUCT', label: '指定商品' },
];

const FirstOrderFormModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<FormValues>();
  const activityType = Form.useWatch('activityType', form) ?? 'FIRST_ORDER_REDUCE';
  const discountMode = Form.useWatch('discountMode', form) ?? 'AMOUNT';
  const scope = Form.useWatch('scope', form) ?? 'ALL';

  useEffect(() => {
    if (!open) return;
    if (!record) {
      form.resetFields();
      form.setFieldsValue({ activityType: FirstOrderActivityType.FIRST_ORDER_REDUCE, discountMode: 'AMOUNT', scope: PromotionScope.ALL });
      return;
    }
    const discountMode = record.discountAmountCents != null ? 'AMOUNT' : 'RATE';
    form.setFieldsValue({
      name: record.name,
      activityType: record.activityType,
      discountMode,
      thresholdYuan: record.thresholdCents != null ? centsToYuan(record.thresholdCents) : undefined,
      discountAmountYuan: record.discountAmountCents != null ? centsToYuan(record.discountAmountCents) : undefined,
      discountRate: record.discountRatePermille != null ? record.discountRatePermille / 1000 : undefined,
      scope: record.activityType === FirstOrderActivityType.NEWCOMER_PRICE ? PromotionScope.PRODUCT : record.scope,
      productId: record.productId != null ? String(record.productId) : undefined,
      newcomerPriceYuan: record.newcomerPriceCents != null ? centsToYuan(record.newcomerPriceCents) : undefined,
      timeWindow: [dayjs(record.validFrom), dayjs(record.validTo)],
    });
  }, [open, record, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    if (!values.timeWindow) {
      message.error('请选择活动时间');
      return;
    }
    const [validFrom, validTo] = values.timeWindow;
    const base = {
      name: values.name,
      activityType: values.activityType,
      validFrom: validFrom.startOf('second').format('YYYY-MM-DDTHH:mm:ss'),
      validTo: validTo.startOf('second').format('YYYY-MM-DDTHH:mm:ss'),
    };
    let payload: FirstOrderFormPayload;
    if (values.activityType === 'FIRST_ORDER_REDUCE') {
      if (values.thresholdYuan === undefined) {
        message.error('请输入立减门槛');
        return;
      }
      if (values.discountMode === 'AMOUNT') {
        if (values.discountAmountYuan === undefined) {
          message.error('请输入减金额');
          return;
        }
        payload = {
          ...base,
          thresholdCents: yuanToCents(values.thresholdYuan),
          discountAmountCents: yuanToCents(values.discountAmountYuan),
          discountRatePermille: null,
          scope: values.scope,
          productId: values.scope === 'PRODUCT' ? (values.productId ?? null) : null,
          newcomerPriceCents: null,
        };
      } else {
        if (values.discountRate === undefined) {
          message.error('请输入折扣率');
          return;
        }
        payload = {
          ...base,
          thresholdCents: yuanToCents(values.thresholdYuan),
          discountAmountCents: null,
          discountRatePermille: Math.round(values.discountRate * 1000),
          scope: values.scope,
          productId: values.scope === 'PRODUCT' ? (values.productId ?? null) : null,
          newcomerPriceCents: null,
        };
      }
    } else {
      if (!values.productId) {
        message.error('请选择商品');
        return;
      }
      if (values.newcomerPriceYuan === undefined) {
        message.error('请输入新人专享价');
        return;
      }
      payload = {
        ...base,
        thresholdCents: null,
        discountAmountCents: null,
        discountRatePermille: null,
        scope: PromotionScope.PRODUCT,
        productId: values.productId,
        newcomerPriceCents: yuanToCents(values.newcomerPriceYuan),
      };
    }
    onSubmit(payload, record);
  };

  return (
// TODO(Phase4): 弹窗外壳可复用 FormModal + useFormModal(编辑X/新建X/confirmLoading/destroyOnClose),见 ops/banners/BannerFormModal 迁移示例;字段与 payload 转换保留本页。
    <Modal
      title={record ? '编辑优惠' : '新建优惠'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={submitting}
      destroyOnClose
      width={620}
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
          <Input placeholder="如:新人首单立减" maxLength={100} showCount />
        </Form.Item>
        <Form.Item name="activityType" label="优惠类型" rules={[{ required: true, message: '请选择优惠类型' }]}>
          <Radio.Group options={ACTIVITY_TYPE_OPTIONS} />
        </Form.Item>
        {activityType === 'FIRST_ORDER_REDUCE' ? (
          <>
            <Form.Item
              name="thresholdYuan"
              label="立减门槛(元)"
              rules={[
                { required: true, message: '请输入门槛金额' },
                {
                  validator: (_, value: number | undefined) =>
                    value !== undefined && value >= 0.01 && value <= 99999
                      ? Promise.resolve()
                      : Promise.reject(new Error('门槛无效(0.01-99999元)')),
                },
              ]}
            >
              <InputNumber min={0.01} max={99999} precision={2} placeholder="如 50" style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="discountMode" label="立减方式">
              <Radio.Group
                options={[
                  { value: 'AMOUNT', label: '减金额' },
                  { value: 'RATE', label: '打折扣' },
                ]}
              />
            </Form.Item>
            {discountMode === 'AMOUNT' ? (
              <Form.Item
                name="discountAmountYuan"
                label="立减金额(元)"
                rules={[
                  { required: true, message: '请输入减金额' },
                  {
                    validator: (_, value: number | undefined) =>
                      value !== undefined && value >= 0.01 && value <= 99999
                        ? Promise.resolve()
                        : Promise.reject(new Error('减金额无效(0.01-99999元)')),
                  },
                ]}
              >
                <InputNumber min={0.01} max={99999} precision={2} placeholder="如 10" style={{ width: '100%' }} />
              </Form.Item>
            ) : (
              <Form.Item
                name="discountRate"
                label="折扣(折)"
                rules={[
                  { required: true, message: '请输入折扣' },
                  {
                    validator: (_, value: number | undefined) =>
                      value !== undefined && value >= 1 && value <= 9.9
                        ? Promise.resolve()
                        : Promise.reject(new Error('折扣率超出范围(1-9.9折)')),
                  },
                ]}
              >
                <InputNumber min={1} max={9.9} precision={1} step={0.1} placeholder="如 9" style={{ width: '100%' }} />
              </Form.Item>
            )}
            <Form.Item name="scope" label="适用范围" rules={[{ required: true, message: '请选择适用范围' }]}>
              <Select options={SCOPE_OPTIONS} />
            </Form.Item>
            {scope === 'PRODUCT' && (
              <Form.Item name="productId" label="指定商品" rules={[{ required: true, message: '请选择商品' }]}>
                <ProductSelect />
              </Form.Item>
            )}
          </>
        ) : (
          <>
            <Form.Item name="productId" label="指定商品" rules={[{ required: true, message: '请选择商品' }]}>
              <ProductSelect />
            </Form.Item>
            <Form.Item
              name="newcomerPriceYuan"
              label="新人专享价(元)"
              rules={[
                { required: true, message: '请输入新人专享价' },
                {
                  validator: (_, value: number | undefined) =>
                    value !== undefined && value >= 0.01 && value <= 99999
                      ? Promise.resolve()
                      : Promise.reject(new Error('新人专享价无效(0.01-99999元)')),
                },
              ]}
            >
              <InputNumber min={0.01} max={99999} precision={2} placeholder="如 15" style={{ width: '100%' }} />
            </Form.Item>
          </>
        )}
        <Form.Item name="timeWindow" label="活动时间" rules={[{ required: true, message: '请选择活动时间' }]}>
          <RangePicker showTime style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default FirstOrderFormModal;
