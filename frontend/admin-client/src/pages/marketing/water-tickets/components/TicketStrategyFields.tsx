import React from 'react';
import { Form, InputNumber, Switch } from '@aquarush/admin-ui';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
  groupNoMargin: {
    marginBottom: '0px',
  },
  itemMb12: {
    marginBottom: '12px',
  },
});

export interface TicketStrategyFieldsProps {
  /** 是否启用复购激活(决定「需复购单数」子项显隐与动态间距) */
  activationEnabled: boolean;
}

/** 「运营策略」表单分组(由 TicketTypeFormModal 包在 <Form> 中渲染) */
export const TicketStrategyFields: React.FC<TicketStrategyFieldsProps> = ({ activationEnabled }) => (
  <Form.Item label="运营策略" {...stylex.props(styles.groupNoMargin)}>
    <Form.Item
      name="isLeadGeneration"
      valuePropName="checked"
      {...stylex.props(styles.itemMb12)}
      extra="引流票:小额票拉新,仅无已完成订单的新用户可购买(判定口径同首单立减)"
    >
      <Switch />
    </Form.Item>
    <Form.Item
      name="activationEnabled"
      valuePropName="checked"
      // 动态间距依赖 activationEnabled state,保留 style
      style={{ marginBottom: activationEnabled ? 8 : 12 }}
      extra="复购激活:购票后需再完成 N 单复购方可核销,防囤票不复购"
    >
      <Switch />
    </Form.Item>
    {activationEnabled && (
      <Form.Item
        name="activationOrderCount"
        label="需复购单数"
        {...stylex.props(styles.itemMb12)}
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
        <InputNumber min={1} max={100} precision={0} {...stylex.props(styles.fullWidth)} placeholder="如 1" />
      </Form.Item>
    )}
    <Form.Item
      name="maxRedeemPerOrder"
      label="单次核销上限"
      {...stylex.props(styles.groupNoMargin)}
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
      <InputNumber min={0} max={100} precision={0} {...stylex.props(styles.fullWidth)} placeholder="0 = 不限" />
    </Form.Item>
  </Form.Item>
);
