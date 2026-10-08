import { useCallback, useState } from 'react';

/**
 * FormModal 通用状态样板:open(打开)/record(编辑对象,null=新建)/submitting(提交中)。
 *
 * 页面侧用法:
 *   const { open, record, submitting, openModal, closeModal, setSubmitting } = useFormModal<T>();
 *   <XxxFormModal open={open} record={record} submitting={submitting} onCancel={closeModal} onSubmit={...} />
 *
 * 各弹窗的字段与 payload 转换仍保留在各自组件内(见 FormModal 壳)。
 */
export interface UseFormModalResult<T> {
  open: boolean;
  /** 编辑对象;null = 新建 */
  record: T | null;
  submitting: boolean;
  openModal: (record?: T | null) => void;
  closeModal: () => void;
  setSubmitting: (submitting: boolean) => void;
}

export function useFormModal<T>(): UseFormModalResult<T> {
  const [open, setOpen] = useState(false);
  const [record, setRecord] = useState<T | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const openModal = useCallback((nextRecord?: T | null) => {
    setRecord(nextRecord ?? null);
    setOpen(true);
  }, []);

  const closeModal = useCallback(() => {
    setOpen(false);
  }, []);

  return { open, record, submitting, openModal, closeModal, setSubmitting };
}
