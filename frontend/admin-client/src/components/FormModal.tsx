import React from 'react';
import { Modal } from '@/ui';
import type { ModalProps } from '@/ui';

export interface FormModalProps {
  open: boolean;
  /** 编辑对象;null = 新建 */
  record: unknown | null;
  /** 提交中(confirmLoading) */
  submitting?: boolean;
  onCancel: () => void;
  /** 确定按钮回调(通常为 form.validateFields() 后构造 payload 提交) */
  onOk: () => void;
  /** 标题名词:编辑{noun}/新建{noun} */
  noun?: string;
  okText?: string;
  cancelText?: string;
  width?: number;
  /** 确定按钮禁用(如编辑模式下详情未回填完成) */
  okDisabled?: boolean;
  /** 其他 Modal 透传(如 okButtonProps danger) */
  modalProps?: Omit<
    ModalProps,
    'title' | 'open' | 'onOk' | 'onCancel' | 'confirmLoading' | 'destroyOnClose' | 'okText' | 'cancelText' | 'width' | 'okButtonProps'
  >;
  okButtonProps?: ModalProps['okButtonProps'];
  children: React.ReactNode;
}

/**
 * 表单弹窗壳 —— 统一 Modal 的 title(编辑X/新建X)/confirmLoading/destroyOnClose 样板。
 * 各弹窗组件保留自己的 Form 字段与 payload 转换,只把 Modal 外壳交给本组件;
 * open/record/submitting 状态样板见 hooks/useFormModal。
 */
const FormModal: React.FC<FormModalProps> = ({
  open,
  record,
  submitting = false,
  onCancel,
  onOk,
  noun = '记录',
  okText,
  cancelText,
  width = 560,
  okDisabled,
  okButtonProps,
  modalProps,
  children,
}) => (
  <Modal
    title={record ? `编辑${noun}` : `新建${noun}`}
    open={open}
    onOk={onOk}
    onCancel={onCancel}
    confirmLoading={submitting}
    destroyOnClose
    width={width}
    okText={okText}
    cancelText={cancelText}
    okButtonProps={okDisabled ? { ...okButtonProps, disabled: true } : okButtonProps}
    {...modalProps}
  >
    {children}
  </Modal>
);

export default FormModal;
