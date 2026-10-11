import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select, Radio, DatePicker, Button, Space, message } from 'antd';
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import dayjs, { Dayjs } from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import { centsToYuan, yuanToCents } from '@aquarush/common';
import { PromotionActivityType, PromotionScope } from '@aquarush/common';
import type {
  CustomerGroupVo,
  PromotionActivityVo,
  PromotionTierInput,
} from '@aquarush/common';
import ProductSelect from '../../../../components/ProductSelect';
import { useCustomerGroups } from '../../../../services/marketing-graphql';

const { RangePicker } = DatePicker;

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
  tierRow: {
    display: 'flex',
  },
  itemNoMargin: {
    marginBottom: '0px',
  },
  tierInput: {
    width: '140px',
  },
  removeBtn: {
    marginTop: '30px',
  },
});

// 提交给 mutation 的表单载荷(金额已由元转分,时间为 ISO 字符串)
export interface PromotionFormPayload {
  name: string;
  activityType: PromotionActivityType;
  scope: PromotionScope;
  productId?: string | null;
  customerGroupId?: string | null;
  validFrom: string;
  validTo: string;
  tiers: PromotionTierInput[];
}

interface TierValues {
  thresholdYuan: number;
  discountAmountYuan?: number;
  discountRate?: number;
}

interface FormValues {
  name: string;
  activityType: PromotionActivityType;
  scope: PromotionScope;
  productId?: string;
  customerGroupId?: string;
  timeWindow?: [Dayjs, Dayjs];
  tiers: TierValues[];
}

interface Props {
  open: boolean;
  record: PromotionActivityVo | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: PromotionFormPayload, record: PromotionActivityVo | null) => void;
}

const ACTIVITY_TYPE_OPTIONS = [
  { value: 'FULL_REDUCE', label: '满减(满X减Y)' },
  { value: 'FULL_DISCOUNT', label: '满折(满X打N折)' },
];

const SCOPE_OPTIONS = [
  { value: 'ALL', label: '全场' },
  { value: 'PRODUCT', label: '指定商品' },
  { value: 'GROUP', label: '指定客户分组' },
];

const PromotionFormModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<FormValues>();
  const activityType = Form.useWatch('activityType', form) ?? 'FULL_REDUCE';
  const scope = Form.useWatch('scope', form) ?? 'ALL';
  const [loadGroups, { data: groupsData }] = useCustomerGroups();

  useEffect(() => {
    if (!open) return;
    if (!record) {
      form.resetFields();
      form.setFieldsValue({
        activityType: PromotionActivityType.FULL_REDUCE,
        scope: PromotionScope.ALL,
        tiers: [{ thresholdYuan: 0 }],
      });
      return;
    }
    form.setFieldsValue({
      name: record.name,
      activityType: record.activityType,
      scope: record.scope,
      productId: record.productId ?? undefined,
      customerGroupId: record.customerGroupId ?? undefined,
      timeWindow: [dayjs(record.validFrom), dayjs(record.validTo)],
      tiers: record.tiers.map((tier) => ({
        thresholdYuan: centsToYuan(tier.thresholdCents),
        ...(record.activityType === 'FULL_REDUCE'
          ? { discountAmountYuan: tier.discountAmountCents != null ? centsToYuan(tier.discountAmountCents) : undefined }
          : { discountRate: tier.discountRatePermille != null ? tier.discountRatePermille / 1000 : undefined }),
      })),
    });
  }, [open, record, form]);

  useEffect(() => {
    if (open) {
      loadGroups({ variables: { input: { page: 0, size: 200 } } });
    }
  }, [open, loadGroups]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const tiers = values.tiers;
    // 客户端校验:档位门槛必须递增且互斥
    for (let i = 1; i < tiers.length; i++) {
      if (tiers[i].thresholdYuan <= tiers[i - 1].thresholdYuan) {
        message.error(`第 ${i + 1} 档门槛必须大于第 ${i} 档门槛`);
        return;
      }
    }
    if (!values.timeWindow) {
      message.error('请选择活动时间');
      return;
    }
    const [validFrom, validTo] = values.timeWindow;
    const payload: PromotionFormPayload = {
      name: values.name,
      activityType: values.activityType,
      scope: values.scope,
      productId: values.scope === 'PRODUCT' ? (values.productId ?? null) : null,
      customerGroupId: values.scope === 'GROUP' ? (values.customerGroupId ?? null) : null,
      validFrom: validFrom.startOf('second').format('YYYY-MM-DDTHH:mm:ss'),
      validTo: validTo.startOf('second').format('YYYY-MM-DDTHH:mm:ss'),
      tiers: tiers.map((tier) =>
        values.activityType === 'FULL_REDUCE'
          ? {
              thresholdCents: yuanToCents(tier.thresholdYuan),
              discountAmountCents: yuanToCents(tier.discountAmountYuan as number),
              discountRatePermille: null,
            }
          : {
              thresholdCents: yuanToCents(tier.thresholdYuan),
              discountRatePermille: Math.round((tier.discountRate as number) * 1000),
              discountAmountCents: null,
            },
      ),
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
      width={640}
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
          <Input placeholder="如:全场满100减20" maxLength={100} showCount />
        </Form.Item>
        <Form.Item name="activityType" label="活动类型" rules={[{ required: true, message: '请选择活动类型' }]}>
          <Radio.Group options={ACTIVITY_TYPE_OPTIONS} />
        </Form.Item>
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
        <Form.Item name="timeWindow" label="活动时间" rules={[{ required: true, message: '请选择活动时间' }]}>
          <RangePicker showTime {...stylex.props(styles.fullWidth)} />
        </Form.Item>
        <Form.Item label="优惠档位(门槛递增且互斥)" required>
          <Form.List name="tiers">
            {(fields, { add, remove }) => (
              <Space direction="vertical" {...stylex.props(styles.fullWidth)}>
                {fields.map((field) => (
                  <Space key={field.key} align="start" {...stylex.props(styles.tierRow)}>
                    <Form.Item
                      name={[field.name, 'thresholdYuan']}
                      rules={[
                        { required: true, message: '请输入门槛' },
                        {
                          validator: (_, value: number | undefined) =>
                            value !== undefined && value >= 0.01 && value <= 99999
                              ? Promise.resolve()
                              : Promise.reject(new Error('门槛无效(0.01-99999元)')),
                        },
                      ]}
                      label="满(元)"
                      {...stylex.props(styles.itemNoMargin)}
                    >
                      <InputNumber min={0.01} max={99999} precision={2} placeholder="如 100" {...stylex.props(styles.tierInput)} />
                    </Form.Item>
                    {activityType === 'FULL_REDUCE' ? (
                      <Form.Item
                        name={[field.name, 'discountAmountYuan']}
                        rules={[
                          { required: true, message: '请输入减金额' },
                          {
                            validator: (_, value: number | undefined) =>
                              value !== undefined && value >= 0.01 && value <= 99999
                                ? Promise.resolve()
                                : Promise.reject(new Error('减金额无效')),
                          },
                        ]}
                        label="减(元)"
                        {...stylex.props(styles.itemNoMargin)}
                      >
                        <InputNumber min={0.01} max={99999} precision={2} placeholder="如 20" {...stylex.props(styles.tierInput)} />
                      </Form.Item>
                    ) : (
                      <Form.Item
                        name={[field.name, 'discountRate']}
                        rules={[
                          { required: true, message: '请输入折扣' },
                          {
                            validator: (_, value: number | undefined) =>
                              value !== undefined && value >= 1 && value <= 9.9
                                ? Promise.resolve()
                                : Promise.reject(new Error('折扣率超出范围(1-9.9折)')),
                          },
                        ]}
                        label="打(折)"
                        {...stylex.props(styles.itemNoMargin)}
                      >
                        <InputNumber min={1} max={9.9} precision={1} step={0.1} placeholder="如 8.5" {...stylex.props(styles.tierInput)} />
                      </Form.Item>
                    )}
                    <Button
                      type="text"
                      danger
                      icon={<MinusCircleOutlined />}
                      disabled={fields.length <= 1}
                      onClick={() => remove(field.name)}
                      {...stylex.props(styles.removeBtn)}
                    />
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add({ thresholdYuan: 0 })} icon={<PlusOutlined />} block>
                  添加档位
                </Button>
              </Space>
            )}
          </Form.List>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default PromotionFormModal;
