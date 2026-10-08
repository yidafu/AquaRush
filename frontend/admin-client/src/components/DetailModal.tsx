import React from 'react';
import { Modal, Button } from 'antd';

export interface DetailModalProps {
  title: React.ReactNode;
  open: boolean;
  onClose: () => void;
  /** 弹窗宽度(默认 800) */
  width?: number;
  /** 底部按钮;默认 [关闭]。复杂 footer(确认/操作按钮)走本插槽 */
  footer?: React.ReactNode;
  children: React.ReactNode;
}

/**
 * 详情弹窗壳 —— 统一 Modal 的 title/open/onCancel/footer=[关闭] 样板。
 * 内容(Descriptions/JsonViewer 等)保留在调用方;需要自定义底部时传 footer。
 */
const DetailModal: React.FC<DetailModalProps> = ({
  title,
  open,
  onClose,
  width = 800,
  footer,
  children,
}) => (
  <Modal
    title={title}
    open={open}
    onCancel={onClose}
    footer={footer ?? [<Button key="close" onClick={onClose}>关闭</Button>]}
    width={width}
  >
    {children}
  </Modal>
);

export default DetailModal;
