import React, { useEffect } from 'react';
import { Form, Input, InputNumber, Select, Radio, DatePicker } from '@/ui';
import dayjs, { Dayjs } from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import { centsToYuan, yuanToCents, COUPON_TYPE_LABELS } from '@aquarush/common';
import { CouponType, CouponScope, ValidityType, CouponIssuanceType } from '@aquarush/common';
import type { CouponTemplateAdminVo } from '@aquarush/common';
import ProductSelect from '../../../../components/ProductSelect';
import FormModal from '@/components/FormModal';

const { RangePicker } = DatePicker;

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
});

// 提交给 mutation 的表单载荷(金额已由元转分,时间为 ISO 字符串)
export interface CouponFormPayload {
  name: string;
  couponType: CouponType;
  thresholdCents: number;
  discountAmountCents?: number | null;
  discountRatePermille?: number | null;
  scope: CouponScope;
  productId?: string | null;
  validityType: ValidityType;
  validFrom?: string | null;
  validTo?: string | null;
  validDays?: number | null;
  issuanceType: CouponIssuanceType;
  perUserLimit: number;
}

interface FormValues {
  name: string;
  couponType: CouponType;
  discountAmountYuan?: number;
  discountRate?: number;
  thresholdYuan?: number;
  scope: CouponScope;
  productId?: string;
  validityType: ValidityType;
  timeWindow?: [Dayjs, Dayjs];
  validDays?: number;
  issuanceType: CouponIssuanceType;
  perUserLimit: number;
}

interface Props {
  open: boolean;
  record: CouponTemplateAdminVo | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: CouponFormPayload, record: CouponTemplateAdminVo | null) => void;
}

const COUPON_TYPE_OPTIONS = (Object.keys(COUPON_TYPE_LABELS) as CouponType[]).map((value) => ({
  value,
  label: COUPON_TYPE_LABELS[value],
}));

const SCOPE_OPTIONS = [
  { value: 'ALL', label: '全场' },
  { value: 'PRODUCT', label: '指定商品' },
];

const VALIDITY_TYPE_OPTIONS = [
  { value: 'FIXED_RANGE', label: '固定区间' },
  { value: 'DAYS_AFTER_CLAIM', label: '领取后N天' },
];

const ISSUANCE_TYPE_OPTIONS = [
  { value: 'AUTO_CLAIM', label: '自动领取' },
  { value: 'TARGETED', label: '定向发放' },
];

const CouponFormModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<FormValues>();
  const couponType = Form.useWatch('couponType', form) ?? 'CASH';
  const scope = Form.useWatch('scope', form) ?? 'ALL';
  const validityType = Form.useWatch('validityType', form) ?? 'FIXED_RANGE';

  useEffect(() => {
    if (!open) return;
    if (!record) {
      form.resetFields();
      form.setFieldsValue({ couponType: CouponType.CASH, scope: CouponScope.ALL, validityType: ValidityType.FIXED_RANGE, issuanceType: CouponIssuanceType.AUTO_CLAIM });
      return;
    }
    form.setFieldsValue({
      name: record.name,
      couponType: record.couponType,
      discountAmountYuan:
        record.couponType === 'CASH' || record.couponType === 'NO_THRESHOLD_CASH'
          ? record.discountAmountCents != null
            ? centsToYuan(record.discountAmountCents)
            : undefined
          : undefined,
      discountRate:
        record.couponType === 'DISCOUNT' && record.discountRatePermille != null
          ? record.discountRatePermille / 1000
          : undefined,
      thresholdYuan: record.thresholdCents != null ? centsToYuan(record.thresholdCents) : undefined,
      scope: record.scope,
      productId: record.productId ?? undefined,
      validityType: record.validityType,
      timeWindow:
        record.validityType === 'FIXED_RANGE' && record.validFrom && record.validTo
          ? [dayjs(record.validFrom), dayjs(record.validTo)]
          : undefined,
      validDays: record.validityType === 'DAYS_AFTER_CLAIM' ? (record.validDays ?? undefined) : undefined,
      issuanceType: record.issuanceType,
      perUserLimit: record.perUserLimit,
    });
  }, [open, record, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const isNoThreshold = values.couponType === 'NO_THRESHOLD_CASH';
    const isCash = values.couponType === 'CASH' || isNoThreshold;
    const [validFrom, validTo] = values.timeWindow ?? [undefined, undefined];
    const payload: CouponFormPayload = {
      name: values.name,
      couponType: values.couponType,
      thresholdCents: isNoThreshold ? 0 : yuanToCents(values.thresholdYuan ?? 0),
      scope: values.scope,
      productId: values.scope === 'PRODUCT' ? (values.productId ?? null) : null,
      validityType: values.validityType,
      validFrom:
        values.validityType === 'FIXED_RANGE' && validFrom
          ? validFrom.startOf('second').format('YYYY-MM-DDTHH:mm:ss')
          : null,
      validTo:
        values.validityType === 'FIXED_RANGE' && validTo
          ? validTo.startOf('second').format('YYYY-MM-DDTHH:mm:ss')
          : null,
      validDays: values.validityType === 'DAYS_AFTER_CLAIM' ? values.validDays ?? null : null,
      issuanceType: values.issuanceType,
      perUserLimit: values.perUserLimit,
      ...(isCash
        ? { discountAmountCents: yuanToCents(values.discountAmountYuan as number), discountRatePermille: null }
        : { discountRatePermille: Math.round((values.discountRate as number) * 1000), discountAmountCents: null }),
    };
    onSubmit(payload, record);
  };

  return (
    <FormModal
      open={open}
      record={record}
      submitting={submitting}
      onCancel={onCancel}
      onOk={handleOk}
      noun="优惠券"
      width={560}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="name"
          label="券名称"
          rules={[
            { required: true, message: '请输入券名称' },
            { max: 100, message: '券名称不能超过100个字' },
          ]}
        >
          <Input placeholder="如:新客立减5元" maxLength={100} showCount />
        </Form.Item>
        <Form.Item name="couponType" label="券类型" rules={[{ required: true, message: '请选择券类型' }]}>
          <Radio.Group options={COUPON_TYPE_OPTIONS} />
        </Form.Item>
        {couponType === 'CASH' || couponType === 'NO_THRESHOLD_CASH' ? (
          <Form.Item
            name="discountAmountYuan"
            label="面额(元)"
            rules={[
              { required: true, message: '请输入面额' },
              {
                validator: (_, value: unknown) =>
                  typeof value === 'number' && value >= 0.01 && value <= 99999
                    ? Promise.resolve()
                    : Promise.reject(new Error('面额无效(0.01-99999元)')),
              },
            ]}
          >
            <InputNumber min={0.01} max={99999} precision={2} {...stylex.props(styles.fullWidth)} placeholder="如 5" />
          </Form.Item>
        ) : (
          <Form.Item
            name="discountRate"
            label="折扣率(折)"
            rules={[
              { required: true, message: '请输入折扣率' },
              {
                validator: (_, value: unknown) =>
                  typeof value === 'number' && value >= 0.1 && value <= 9.9
                    ? Promise.resolve()
                    : Promise.reject(new Error('折扣率超出范围(0.1-9.9折)')),
              },
            ]}
            extra="输入如 8.5 代表 8.5 折(按原价的 85% 支付)"
          >
            <InputNumber min={0.1} max={9.9} precision={1} step={0.1} {...stylex.props(styles.fullWidth)} placeholder="如 8.5" />
          </Form.Item>
        )}
        {couponType !== 'NO_THRESHOLD_CASH' && (
          <Form.Item
            name="thresholdYuan"
            label="门槛(元)"
            rules={[
              {
                validator: (_, value: unknown) =>
                  value === undefined || value === null || (typeof value === 'number' && value >= 0 && value <= 99999)
                    ? Promise.resolve()
                    : Promise.reject(new Error('门槛金额无效')),
              },
            ]}
            extra="订单实付金额需达到该值才可用;0 或留空 = 无门槛"
          >
            <InputNumber min={0} max={99999} precision={2} {...stylex.props(styles.fullWidth)} placeholder="如 30" />
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
        <Form.Item name="validityType" label="有效期类型" rules={[{ required: true, message: '请选择有效期类型' }]}>
          <Radio.Group options={VALIDITY_TYPE_OPTIONS} />
        </Form.Item>
        {validityType === 'FIXED_RANGE' ? (
          <Form.Item name="timeWindow" label="有效区间" rules={[{ required: true, message: '请选择有效区间' }]}>
            <RangePicker showTime {...stylex.props(styles.fullWidth)} />
          </Form.Item>
        ) : (
          <Form.Item
            name="validDays"
            label="有效天数"
            rules={[
              { required: true, message: '请输入有效天数' },
              {
                validator: (_, value: unknown) =>
                  typeof value === 'number' && value >= 1 && value <= 3650
                    ? Promise.resolve()
                    : Promise.reject(new Error('有效天数无效(1-3650天)')),
              },
            ]}
            extra="用户领取后 N 天内有效"
          >
            <InputNumber min={1} max={3650} precision={0} {...stylex.props(styles.fullWidth)} placeholder="如 7" />
          </Form.Item>
        )}
        <Form.Item name="issuanceType" label="发放方式" rules={[{ required: true, message: '请选择发放方式' }]}>
          <Radio.Group options={ISSUANCE_TYPE_OPTIONS} />
        </Form.Item>
        <Form.Item
          name="perUserLimit"
          label="每人限领(张)"
          rules={[
            { required: true, message: '请输入每人限领张数' },
            {
              validator: (_, value: unknown) =>
                typeof value === 'number' && value >= 1 && value <= 100
                  ? Promise.resolve()
                  : Promise.reject(new Error('限领数无效(1-100)')),
            },
          ]}
        >
          <InputNumber min={1} max={100} precision={0} {...stylex.props(styles.fullWidth)} placeholder="如 1" />
        </Form.Item>
      </Form>
    </FormModal>
  );
};

export default CouponFormModal;
