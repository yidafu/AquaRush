import React from 'react';
import { Form, Input, Modal, Radio, RadioGroup } from '@aquarush/admin-ui';
import type { FormInstance } from '@aquarush/admin-ui';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { discrepancyLabel } from '../helpers';
import type { SourceSystem } from '../../../../types/reconciliation';
import type { DiscrepancyRow } from './constants';

// 静态内联样式迁 StyleX;#666 / #999 为页面局部灰阶,共享 token 无对应项,保留字面量
const styles = stylex.create({
  resolveSummary: {
    marginBottom: '12px',
    color: '#666',
    fontSize: '13px',
  },
  resolveAmount: {
    color: tokens.statusErrorFg,
  },
  hint: {
    fontSize: '12px',
    color: '#999',
  },
});

export interface ResolveDiscrepancyModalProps {
  target: DiscrepancyRow | null;
  action: 'RESOLVE' | 'IGNORE';
  form: FormInstance;
  resolving: boolean;
  onActionChange: (action: 'RESOLVE' | 'IGNORE') => void;
  onCancel: () => void;
  onOk: () => void;
}

/** 差异处置弹窗:解决/忽略 + 备注必填(由 index.tsx 拆分) */
const ResolveDiscrepancyModal: React.FC<ResolveDiscrepancyModalProps> = ({
  target,
  action,
  form,
  resolving,
  onActionChange,
  onCancel,
  onOk,
}) => (
  <Modal
    title={target ? `处置差异 · ${discrepancyLabel(target.discrepancyType, target.sourceSystem as SourceSystem).text} ${target.recordId}` : '处置差异'}
    open={!!target}
    onCancel={onCancel}
    onOk={onOk}
    okText="提交处置"
    cancelText="取消"
    confirmLoading={resolving}
  >
    {target && (
      <div {...stylex.props(styles.resolveSummary)}>
        记录标识: {target.recordId}
        {target.wechatCents != null && (
          <>
            {' ｜ '}微信金额: <span {...stylex.props(styles.resolveAmount)}>{displayCents(target.wechatCents)}</span>
          </>
        )}
        {target.localCents != null && <> ｜ 本地金额: {displayCents(target.localCents)}</>}
      </div>
    )}
    <Form form={form} layout="vertical">
      <Form.Item label="处置结果" required>
        <RadioGroup
          value={action}
          onChange={(e) => onActionChange(e.target.value as 'RESOLVE' | 'IGNORE')}
        >
          <Radio value="RESOLVE">解决（已核实无资金损失或已修复）</Radio>
          <Radio value="IGNORE">忽略（跨日边界等可解释差异）</Radio>
        </RadioGroup>
      </Form.Item>
      <Form.Item
        name="resolutionNotes"
        label="处理说明（备注必填）"
        rules={[{ required: true, message: '请填写处理说明' }]}
      >
        <Input.TextArea rows={3} maxLength={500} placeholder="请填写处理说明" />
      </Form.Item>
      <div {...stylex.props(styles.hint)}>
        处置留痕可审计；重跑幂等保留已处置记录；已处置差异不可再改。
      </div>
    </Form>
  </Modal>
);

export default ResolveDiscrepancyModal;
