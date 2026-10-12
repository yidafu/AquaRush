import React from 'react';
import { DatePicker, Form, Modal, Radio, RadioGroup } from '@aquarush/admin-ui';
import type { FormInstance } from '@aquarush/admin-ui';
import dayjs from 'dayjs';
import * as stylex from '@stylexjs/stylex';

// 静态内联样式迁 StyleX;#999 为页面局部灰阶,共享 token 无对应项,保留字面量
const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
  hint: {
    fontSize: '12px',
    color: '#999',
  },
});

export interface ExecuteManualModalProps {
  open: boolean;
  form: FormInstance;
  executing: boolean;
  onCancel: () => void;
  onOk: () => void;
}

/** 手动对账弹窗:类型 + 账单日(T-1~T-30)(由 index.tsx 拆分) */
const ExecuteManualModal: React.FC<ExecuteManualModalProps> = ({
  open,
  form,
  executing,
  onCancel,
  onOk,
}) => (
  <Modal
    title="手动对账"
    open={open}
    onCancel={onCancel}
    onOk={onOk}
    okText="开始对账"
    cancelText="取消"
    confirmLoading={executing}
  >
    <Form form={form} layout="vertical" initialValues={{ taskType: 'PAYMENT' }}>
      <Form.Item name="taskType" label="账单类型" rules={[{ required: true }]}>
        <RadioGroup>
          <Radio value="PAYMENT">支付账单</Radio>
          <Radio value="REFUND">退款账单</Radio>
        </RadioGroup>
      </Form.Item>
      <Form.Item name="reconciliationDate" label="账单日" rules={[{ required: true, message: '请选择账单日' }]}>
        <DatePicker
          {...stylex.props(styles.fullWidth)}
          disabledDate={(d) => {
            const day = d.endOf('day');
            return day.isAfter(dayjs().subtract(1, 'day')) || d.isBefore(dayjs().subtract(30, 'day'), 'day');
          }}
        />
      </Form.Item>
      <div {...stylex.props(styles.hint)}>仅支持近 30 天账单（T-1 ~ T-30）；同账单日重跑幂等，已处置差异保留。</div>
    </Form>
  </Modal>
);

export default ExecuteManualModal;
