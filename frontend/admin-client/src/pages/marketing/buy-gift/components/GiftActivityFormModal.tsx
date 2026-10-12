import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select, Radio, DatePicker, message } from '@/ui';
import dayjs, { Dayjs } from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import { centsToYuan, yuanToCents } from '@aquarush/common';
import { GiftActivityType, PromotionScope } from '@aquarush/common';
import type { CustomerGroupVo, GiftActivityVo } from '@aquarush/common';
import ProductSelect from '../../../../components/ProductSelect';
import { useCustomerGroups } from '../../../../services/marketing-graphql';

const { RangePicker } = DatePicker;

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
});

// 提交给 mutation 的表单载荷(金额已由元转分,时间为 ISO 字符串)
export interface GiftFormPayload {
  name: string;
  giftType: GiftActivityType;
  thresholdCents?: number | null;
  thresholdQuantity?: number | null;
  scope: PromotionScope;
  productId?: string | null;
  customerGroupId?: string | null;
  giftProductId: string;
  giftQuantity: number;
  validFrom: string;
  validTo: string;
}

interface FormValues {
  name: string;
  giftType: GiftActivityType;
  thresholdYuan?: number;
  thresholdQuantity?: number;
  scope: PromotionScope;
  productId?: string;
  customerGroupId?: string;
  giftProductId?: string;
  giftQuantity?: number;
  timeWindow?: [Dayjs, Dayjs];
}

interface Props {
  open: boolean;
  record: GiftActivityVo | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: GiftFormPayload, record: GiftActivityVo | null) => void;
}

const GIFT_TYPE_OPTIONS = [
  { value: 'FULL_GIFT', label: '满赠(订单满X元赠)' },
  { value: 'BUY_GIFT', label: '买赠(指定商品满X桶送)' },
];

const SCOPE_OPTIONS = [
  { value: 'ALL', label: '全场' },
  { value: 'PRODUCT', label: '指定商品' },
  { value: 'GROUP', label: '指定客户分组' },
];

const GiftActivityFormModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<FormValues>();
  const giftType = Form.useWatch('giftType', form) ?? 'FULL_GIFT';
  const scope = Form.useWatch('scope', form) ?? 'ALL';
  const [loadGroups, { data: groupsData }] = useCustomerGroups();

  useEffect(() => {
    if (!open) return;
    if (!record) {
      form.resetFields();
      form.setFieldsValue({ giftType: GiftActivityType.FULL_GIFT, scope: PromotionScope.ALL });
      return;
    }
    form.setFieldsValue({
      name: record.name,
      giftType: record.giftType,
      thresholdYuan: record.thresholdCents != null ? centsToYuan(record.thresholdCents) : undefined,
      thresholdQuantity: record.thresholdQuantity ?? undefined,
      scope: record.scope,
      productId: record.productId ?? undefined,
      customerGroupId: record.customerGroupId ?? undefined,
      giftProductId: String(record.giftProductId),
      giftQuantity: record.giftQuantity,
      timeWindow: [dayjs(record.validFrom), dayjs(record.validTo)],
    });
  }, [open, record, form]);

  useEffect(() => {
    if (open) {
      loadGroups({ variables: { input: { page: 0, size: 200 } } });
    }
  }, [open, loadGroups]);

  const handleOk = async () => {
    const values = await form.validateFields();
    if (!values.giftProductId) {
      message.error('请选择赠品商品');
      return;
    }
    if (!values.giftQuantity || values.giftQuantity < 1) {
      message.error('请输入赠品数量');
      return;
    }
    if (!values.timeWindow) {
      message.error('请选择活动时间');
      return;
    }
    const [validFrom, validTo] = values.timeWindow;
    const payload: GiftFormPayload = {
      name: values.name,
      giftType: values.giftType,
      thresholdCents: values.giftType === 'FULL_GIFT' ? yuanToCents(values.thresholdYuan as number) : null,
      thresholdQuantity: values.giftType === 'BUY_GIFT' ? (values.thresholdQuantity as number) : null,
      scope: values.scope,
      productId: values.scope === 'PRODUCT' ? (values.productId ?? null) : null,
      customerGroupId: values.scope === 'GROUP' ? (values.customerGroupId ?? null) : null,
      giftProductId: values.giftProductId,
      giftQuantity: values.giftQuantity,
      validFrom: validFrom.startOf('second').format('YYYY-MM-DDTHH:mm:ss'),
      validTo: validTo.startOf('second').format('YYYY-MM-DDTHH:mm:ss'),
    };
    onSubmit(payload, record);
  };

  const groups = groupsData?.customerGroups?.content ?? [];

  return (
// TODO(Phase4): 弹窗外壳可复用 FormModal + useFormModal(编辑X/新建X/confirmLoading/destroyOnClose),见 ops/banners/BannerFormModal 迁移示例;字段与 payload 转换保留本页。
    <Modal
      title={record ? '编辑活动' : '新建活动'}
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
          <Input placeholder="如:满100元赠小桶" maxLength={100} showCount />
        </Form.Item>
        <Form.Item name="giftType" label="活动类型" rules={[{ required: true, message: '请选择活动类型' }]}>
          <Radio.Group options={GIFT_TYPE_OPTIONS} />
        </Form.Item>
        {giftType === 'FULL_GIFT' ? (
          <Form.Item
            name="thresholdYuan"
            label="达标门槛(订单现金部分商品小计,元)"
            rules={[
              { required: true, message: '请输入门槛金额' },
              {
                validator: (_, value: unknown) =>
                  typeof value === 'number' && value >= 0.01 && value <= 99999
                    ? Promise.resolve()
                    : Promise.reject(new Error('门槛无效(0.01-99999元)')),
              },
            ]}
          >
            <InputNumber min={0.01} max={99999} precision={2} placeholder="如 100" {...stylex.props(styles.fullWidth)} />
          </Form.Item>
        ) : (
          <Form.Item
            name="thresholdQuantity"
            label="达标门槛(指定商品购买桶数)"
            rules={[
              { required: true, message: '请输入门槛数量' },
              {
                validator: (_, value: unknown) =>
                  typeof value === 'number' && value >= 1 && value <= 999
                    ? Promise.resolve()
                    : Promise.reject(new Error('门槛数量无效(1-999桶)')),
              },
            ]}
          >
            <InputNumber min={1} max={999} precision={0} placeholder="如 5" {...stylex.props(styles.fullWidth)} />
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
        {scope === 'GROUP' && (
          <Form.Item name="customerGroupId" label="指定客户分组" rules={[{ required: true, message: '请选择客户分组' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={groups.map((g: CustomerGroupVo) => ({ value: String(g.id), label: `${g.name}(${g.memberCount}人)` }))}
              placeholder="选择客户分组"
            />
          </Form.Item>
        )}
        <Form.Item name="giftProductId" label="赠品商品" rules={[{ required: true, message: '请选择赠品商品' }]}>
          <ProductSelect />
        </Form.Item>
        <Form.Item
          name="giftQuantity"
          label="赠品数量"
          rules={[
            { required: true, message: '请输入赠品数量' },
            {
              validator: (_, value: unknown) =>
                typeof value === 'number' && value >= 1 && value <= 999
                  ? Promise.resolve()
                  : Promise.reject(new Error('赠品数量无效(1-999)')),
            },
          ]}
        >
          <InputNumber min={1} max={999} precision={0} placeholder="如 1" {...stylex.props(styles.fullWidth)} />
        </Form.Item>
        <Form.Item name="timeWindow" label="活动时间" rules={[{ required: true, message: '请选择活动时间' }]}>
          <RangePicker showTime {...stylex.props(styles.fullWidth)} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default GiftActivityFormModal;
