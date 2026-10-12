import React from 'react';
import { Form, Input, InputNumber, Radio, Select, Checkbox, DatePicker, Row, Col, Button } from '@aquarush/admin-ui';
import { PlusOutlined, DeleteOutlined } from '@aquarush/admin-ui/icons';
import ProductSelect from '@/components/ProductSelect';
import { TIME_SLOT_OPTIONS, WEEKDAY_OPTIONS } from '../../../utils/subscription';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  itemTight: {
    marginBottom: '8px',
  },
  fullWidth: {
    width: '100%',
  },
});

const CYCLE_TYPE_OPTIONS = [
  { value: 'WEEKLY', label: '每周' },
  { value: 'BIWEEKLY', label: '每两周' },
  { value: 'MONTHLY', label: '每月' },
];

/** 商品明细行数上限(1~20 行) */
const MAX_ITEM_ROWS = 20;

/** 「商品明细」动态行区块(由 CreateSubscriptionModal 包在 <Form> 中渲染) */
export const SubscriptionItemsField: React.FC = () => (
  <Form.Item label="商品明细">
    <Form.List
      name="items"
      rules={[
        {
          validator: (_, list: unknown) =>
            Array.isArray(list) && list.length >= 1
              ? Promise.resolve()
              : Promise.reject(new Error('至少添加一个商品')),
        },
      ]}
    >
      {(fields, { add, remove }, { errors }) => (
        <>
          {fields.map((field) => (
            <Row key={field.key} gutter={8} align="middle">
              <Col span={14}>
                <Form.Item
                  {...stylex.props(styles.itemTight)}
                  name={[field.name, 'productId']}
                  rules={[{ required: true, message: '请选择商品' }]}
                >
                  <ProductSelect {...stylex.props(styles.fullWidth)} placeholder="选择商品" />
                </Form.Item>
              </Col>
              <Col span={7}>
                <Form.Item
                  {...stylex.props(styles.itemTight)}
                  name={[field.name, 'quantity']}
                  rules={[{ required: true, message: '数量必填' }]}
                >
                  <InputNumber min={1} max={999} {...stylex.props(styles.fullWidth)} placeholder="数量" />
                </Form.Item>
              </Col>
              <Col span={3}>
                <Button
                  type="text"
                  danger
                  disabled={fields.length <= 1}
                  aria-label="删除该行"
                  icon={<DeleteOutlined />}
                  onClick={() => remove(field.name)}
                />
              </Col>
            </Row>
          ))}
          <Form.Item {...stylex.props(styles.itemTight)}>
            <Button
              type="dashed"
              block
              icon={<PlusOutlined />}
              disabled={fields.length >= MAX_ITEM_ROWS}
              onClick={() => add({ quantity: 1 })}
            >
              添加商品{fields.length >= MAX_ITEM_ROWS ? '(已达上限20行)' : ''}
            </Button>
            <Form.ErrorList errors={errors} />
          </Form.Item>
        </>
      )}
    </Form.List>
  </Form.Item>
);

export interface SubscriptionScheduleFieldsProps {
  /** 当前配送周期(表单 cycleType 值) */
  cycleType: string;
}

/** 配送周期/时段/起止日期/备注区块(由 CreateSubscriptionModal 包在 <Form> 中渲染) */
export const SubscriptionScheduleFields: React.FC<SubscriptionScheduleFieldsProps> = ({ cycleType }) => (
  <>
    <Row gutter={16}>
      <Col span={12}>
        <Form.Item name="cycleType" label="配送周期" rules={[{ required: true }]}>
          <Radio.Group options={CYCLE_TYPE_OPTIONS} />
        </Form.Item>
      </Col>
      <Col span={12}>
        <Form.Item name="timeSlot" label="配送时段" rules={[{ required: true, message: '请选择配送时段' }]}>
          <Select options={TIME_SLOT_OPTIONS} placeholder="选择时段" />
        </Form.Item>
      </Col>
    </Row>

    {cycleType === 'MONTHLY' ? (
      <Form.Item
        name="monthDay"
        label="每月配送日"
        rules={[{ required: true, message: '请填写每月配送日' }]}
      >
        <InputNumber min={1} max={31} {...stylex.props(styles.fullWidth)} addonAfter="日" placeholder="1-31" />
      </Form.Item>
    ) : (
      <Form.Item
        name="weekdays"
        label="配送星期"
        rules={[
          {
            validator: (_: unknown, value: number[] | undefined) =>
              value && value.length > 0
                ? Promise.resolve()
                : Promise.reject(new Error('请选择至少一个配送星期')),
          },
        ]}
      >
        <Checkbox.Group options={WEEKDAY_OPTIONS} />
      </Form.Item>
    )}

    <Row gutter={16}>
      <Col span={8}>
        <Form.Item
          name="startDate"
          label="生效日期"
          rules={[{ required: true, message: '请选择生效日期' }]}
        >
          <DatePicker {...stylex.props(styles.fullWidth)} placeholder="开始日期" />
        </Form.Item>
      </Col>
      <Col span={8}>
        <Form.Item name="endDate" label="终止日期(可选)">
          <DatePicker {...stylex.props(styles.fullWidth)} placeholder="不填则长期有效" />
        </Form.Item>
      </Col>
      <Col span={8}>
        <Form.Item name="maxPeriods" label="期次上限(可选)">
          <InputNumber min={1} {...stylex.props(styles.fullWidth)} placeholder="不填则不限期次" />
        </Form.Item>
      </Col>
    </Row>

    <Form.Item
      name="remark"
      label="备注(可选)"
      rules={[{ max: 500, message: '备注长度不能超过500个字符' }]}
    >
      <Input.TextArea rows={2} maxLength={500} showCount placeholder="备注信息" />
    </Form.Item>
  </>
);
