import React, { useEffect } from 'react';
import { Form, Input, InputNumber, Radio, DatePicker, Switch } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import { centsToYuan, yuanToCents } from '@aquarush/common';
import { TicketTypeKind, ValidityType } from '@aquarush/common';
import type { TicketTypeAdminVo } from '@aquarush/common';
import ProductSelect from '../../../../components/ProductSelect';
import FormModal from '@/components/FormModal';

const { RangePicker } = DatePicker;

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
});

// 提交给 mutation 的表单载荷(金额已由元转分,时间为 ISO 字符串)
export interface TicketTypeFormPayload {
  name: string;
  ticketType: TicketTypeKind;
  productId?: string | null;
  specification?: string | null;
  packageCount: number;
  packagePriceCents: number;
  giftCount: number;
  validityType: ValidityType;
  validFrom?: string | null;
  validTo?: string | null;
  validDays?: number | null;
  perUserLimit: number;
  // G97 运营策略(R-MKT-08 修订):引流票 / 复购激活单数(null=关) / 单次核销上限(0=不限)
  isLeadGeneration: boolean;
  activationOrderCount: number | null;
  maxRedeemPerOrder: number;
}

interface FormValues {
  name: string;
  ticketType: TicketTypeKind;
  productId?: string;
  specification?: string;
  packageCount: number;
  packagePriceYuan: number;
  giftCount: number;
  validityType: ValidityType;
  timeWindow?: [Dayjs, Dayjs];
  validDays?: number;
  perUserLimit: number;
  isLeadGeneration: boolean;
  activationEnabled: boolean;
  activationOrderCount?: number;
  maxRedeemPerOrder: number;
}

interface Props {
  open: boolean;
  record: TicketTypeAdminVo | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: TicketTypeFormPayload, record: TicketTypeAdminVo | null) => void;
}

const TICKET_TYPE_OPTIONS = [
  { value: 'SPECIFIC_PRODUCT', label: '指定商品' },
  { value: 'UNIVERSAL', label: '通兑票' },
];

const VALIDITY_TYPE_OPTIONS = [
  { value: 'FIXED_RANGE', label: '固定区间' },
  { value: 'DAYS_AFTER_PURCHASE', label: '购买后N天' },
];

const TicketTypeFormModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<FormValues>();
  const ticketType = Form.useWatch('ticketType', form) ?? 'SPECIFIC_PRODUCT';
  const validityType = Form.useWatch('validityType', form) ?? 'FIXED_RANGE';
  const activationEnabled = Form.useWatch('activationEnabled', form) ?? false;

  useEffect(() => {
    if (!open) return;
    if (!record) {
      form.resetFields();
      form.setFieldsValue({ ticketType: TicketTypeKind.SPECIFIC_PRODUCT, validityType: ValidityType.FIXED_RANGE, perUserLimit: 0, isLeadGeneration: false, activationEnabled: false, activationOrderCount: 1, maxRedeemPerOrder: 0 });
      return;
    }
    form.setFieldsValue({
      name: record.name,
      ticketType: record.ticketType,
      productId: record.productId ?? undefined,
      specification: record.specification ?? undefined,
      packageCount: record.packageCount,
      packagePriceYuan: centsToYuan(record.packagePriceCents),
      giftCount: record.giftCount,
      validityType: record.validityType,
      timeWindow:
        record.validityType === 'FIXED_RANGE' && record.validFrom && record.validTo
          ? [dayjs(record.validFrom), dayjs(record.validTo)]
          : undefined,
      validDays: record.validityType === 'DAYS_AFTER_PURCHASE' ? (record.validDays ?? undefined) : undefined,
      perUserLimit: record.perUserLimit,
      isLeadGeneration: record.isLeadGeneration,
      activationEnabled: (record.activationOrderCount ?? 0) > 0,
      activationOrderCount: record.activationOrderCount ?? 1,
      maxRedeemPerOrder: record.maxRedeemPerOrder ?? 0,
    });
  }, [open, record, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const [validFrom, validTo] = values.timeWindow ?? [undefined, undefined];
    const payload: TicketTypeFormPayload = {
      name: values.name,
      ticketType: values.ticketType,
      productId: values.ticketType === 'SPECIFIC_PRODUCT' ? (values.productId ?? null) : null,
      specification: values.ticketType === 'UNIVERSAL' ? (values.specification ?? null) : null,
      packageCount: values.packageCount,
      packagePriceCents: yuanToCents(values.packagePriceYuan),
      giftCount: values.giftCount,
      validityType: values.validityType,
      validFrom:
        values.validityType === 'FIXED_RANGE' && validFrom
          ? validFrom.startOf('second').format('YYYY-MM-DDTHH:mm:ss')
          : null,
      validTo:
        values.validityType === 'FIXED_RANGE' && validTo
          ? validTo.startOf('second').format('YYYY-MM-DDTHH:mm:ss')
          : null,
      validDays: values.validityType === 'DAYS_AFTER_PURCHASE' ? values.validDays ?? null : null,
      perUserLimit: values.perUserLimit,
      isLeadGeneration: values.isLeadGeneration ?? false,
      activationOrderCount: values.activationEnabled ? values.activationOrderCount ?? 1 : null,
      maxRedeemPerOrder: values.maxRedeemPerOrder ?? 0,
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
      noun="票种"
      width={560}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="name"
          label="票种名称"
          rules={[
            { required: true, message: '请输入票种名称' },
            { max: 100, message: '票种名称不能超过100个字' },
          ]}
        >
          <Input placeholder="如:桶装水通兑票" maxLength={100} showCount />
        </Form.Item>
        <Form.Item name="ticketType" label="票种类型" rules={[{ required: true, message: '请选择票种类型' }]}>
          <Radio.Group options={TICKET_TYPE_OPTIONS} />
        </Form.Item>
        {ticketType === 'SPECIFIC_PRODUCT' ? (
          <Form.Item name="productId" label="指定商品" rules={[{ required: true, message: '请选择商品' }]}>
            <ProductSelect />
          </Form.Item>
        ) : (
          <Form.Item
            name="specification"
            label="适用规格"
            rules={[
              { required: true, message: '请输入适用规格' },
              { max: 100, message: '适用规格不能超过100个字符' },
            ]}
            extra="通兑票限定规格(如 19L),与商品规格匹配"
          >
            <Input placeholder="如 19L" maxLength={100} />
          </Form.Item>
        )}
        <Form.Item
          name="packageCount"
          label="套餐张数"
          rules={[
            { required: true, message: '请输入套餐张数' },
            {
              validator: (_, value: number | undefined) =>
                value !== undefined && value >= 1 && value <= 1000
                  ? Promise.resolve()
                  : Promise.reject(new Error('套餐张数必须大于0且不超过1000')),
            },
          ]}
        >
          <InputNumber min={1} max={1000} precision={0} {...stylex.props(styles.fullWidth)} placeholder="如 10" />
        </Form.Item>
        <Form.Item
          name="packagePriceYuan"
          label="购票价(元)"
          rules={[
            { required: true, message: '请输入购票价' },
            {
              validator: (_, value: number | undefined) =>
                value !== undefined && value >= 0.01 && value <= 99999
                  ? Promise.resolve()
                  : Promise.reject(new Error('购票价无效(0.01-99999元)')),
            },
          ]}
        >
          <InputNumber min={0.01} max={99999} precision={2} {...stylex.props(styles.fullWidth)} placeholder="如 100" />
        </Form.Item>
        <Form.Item
          name="giftCount"
          label="赠送张数"
          rules={[
            { required: true, message: '请输入赠送张数' },
            {
              validator: (_, value: number | undefined) =>
                value !== undefined && value >= 0 && value <= 1000
                  ? Promise.resolve()
                  : Promise.reject(new Error('赠送张数无效(0-1000)')),
            },
          ]}
        >
          <InputNumber min={0} max={1000} precision={0} {...stylex.props(styles.fullWidth)} placeholder="如 2" />
        </Form.Item>
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
                validator: (_, value: number | undefined) =>
                  value !== undefined && value >= 1 && value <= 3650
                    ? Promise.resolve()
                    : Promise.reject(new Error('有效天数无效(1-3650天)')),
              },
            ]}
            extra="用户购买后 N 天内有效"
          >
            <InputNumber min={1} max={3650} precision={0} {...stylex.props(styles.fullWidth)} placeholder="如 365" />
          </Form.Item>
        )}
        <Form.Item
          name="perUserLimit"
          label="每人限购"
          rules={[
            { required: true, message: '请输入每人限购数' },
            {
              validator: (_, value: number | undefined) =>
                value !== undefined && value >= 0 && value <= 1000
                  ? Promise.resolve()
                  : Promise.reject(new Error('限购数无效(0-1000)')),
            },
          ]}
          extra="0 = 不限购"
        >
          <InputNumber min={0} max={1000} precision={0} {...stylex.props(styles.fullWidth)} placeholder="0 = 不限购" />
        </Form.Item>
        <Form.Item label="运营策略" style={{ marginBottom: 0 }}>
          <Form.Item
            name="isLeadGeneration"
            valuePropName="checked"
            style={{ marginBottom: 12 }}
            extra="引流票:小额票拉新,仅无已完成订单的新用户可购买(判定口径同首单立减)"
          >
            <Switch />
          </Form.Item>
          <Form.Item
            name="activationEnabled"
            valuePropName="checked"
            style={{ marginBottom: activationEnabled ? 8 : 12 }}
            extra="复购激活:购票后需再完成 N 单复购方可核销,防囤票不复购"
          >
            <Switch />
          </Form.Item>
          {activationEnabled && (
            <Form.Item
              name="activationOrderCount"
              label="需复购单数"
              style={{ marginBottom: 12 }}
              rules={[
                {
                  validator: (_, value: number | undefined) =>
                    value !== undefined && value >= 1 && value <= 100
                      ? Promise.resolve()
                      : Promise.reject(new Error('复购单数必须为1-100')),
                },
              ]}
              extra="如 1 = 购票后完成 1 单复购即激活,可核销"
            >
              <InputNumber min={1} max={100} precision={0} style={{ width: '100%' }} placeholder="如 1" />
            </Form.Item>
          )}
          <Form.Item
            name="maxRedeemPerOrder"
            label="单次核销上限"
            style={{ marginBottom: 0 }}
            rules={[
              {
                validator: (_, value: number | undefined) =>
                  value !== undefined && value >= 0 && value <= 100
                    ? Promise.resolve()
                    : Promise.reject(new Error('单次核销上限无效(0-100)')),
              },
            ]}
            extra="0 = 不限;单笔订单该票种最多核销 N 张,防一次性清票"
          >
            <InputNumber min={0} max={100} precision={0} style={{ width: '100%' }} placeholder="0 = 不限" />
          </Form.Item>
        </Form.Item>
      </Form>
    </FormModal>
  );
};

export default TicketTypeFormModal;
