import React, { useState } from 'react';
import { Modal, Form, Select, message } from '@/ui';
import type { DeliveryWorkerOption } from '../types';

export interface BatchAssignModalProps {
  open: boolean;
  /** 已勾选订单数(展示在标题) */
  selectedCount: number;
  workers: DeliveryWorkerOption[];
  workersLoading?: boolean;
  confirmLoading?: boolean;
  /** 确认派单(页面执行 mutation 并刷新) */
  onOk: (workerId: string) => void;
  onCancel: () => void;
}

/**
 * 批量派单弹窗(G10):多选待派单订单一次派给同一送水工。
 * 送水员选择为弹窗内部状态,打开时重置(仅在线可派单,R-ORD-08)。
 */
const BatchAssignModal: React.FC<BatchAssignModalProps> = ({
  open,
  selectedCount,
  workers,
  workersLoading = false,
  confirmLoading = false,
  onOk,
  onCancel,
}) => {
  const [workerId, setWorkerId] = useState<string | null>(null);

  const handleOk = () => {
    if (!workerId) {
      message.warning('请选择送水员');
      return;
    }
    onOk(workerId);
  };

  return (
    <Modal
      title={`批量派单（${selectedCount} 单）`}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={confirmLoading}
      width={500}
      destroyOnClose
    >
      <Form layout="vertical">
        <Form.Item label="选择送水员" required>
          <Select
            placeholder="选择送水员（仅在线可派单，R-ORD-08）"
            loading={workersLoading}
            value={workerId}
            onChange={(value) => setWorkerId(typeof value === 'string' ? value : null)}
          >
            {workers.map((worker) => (
              <Select.Option key={worker.id} value={worker.id}>
                {worker.name} - {worker.phone}
                {worker.onlineStatus === 'ONLINE' ? '' : '（离线）'}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default BatchAssignModal;
