import React from 'react';
import { Modal, Form, Select } from 'antd';
import { useOrderDetail } from '../OrderDetailContext';

const AssignDeliveryModal: React.FC = () => {
  const {
    assignModalVisible,
    assignLoading,
    workers,
    workersLoading,
    selectedWorkerId,
    isSelfCollect,
    closeAssignModal,
    setSelectedWorkerId,
    setIsSelfCollect,
    assignDeliveryWorker,
  } = useOrderDetail();

  const handleOk = async () => {
    await assignDeliveryWorker();
  };

  return (
    <Modal
      title="分配配送员"
      open={assignModalVisible}
      onOk={handleOk}
      onCancel={closeAssignModal}
      confirmLoading={assignLoading}
      width={500}
    >
      <Form layout="vertical">
        <Form.Item
          label="选择配送员"
          rules={[{ required: true, message: '请选择配送员' }]}
        >
          <Select
            placeholder="选择配送员"
            loading={workersLoading}
            onChange={(value) => setSelectedWorkerId(value)}
            value={selectedWorkerId}
          >
            {workers.map((worker) => (
              <Select.Option key={worker.id} value={worker.id}>
                {worker.name} - {worker.phone}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item label="收款方式">
          <Select
            placeholder="选择收款方式"
            onChange={(value) => setIsSelfCollect(value === 'SELF_COLLECT')}
            value={isSelfCollect ? 'SELF_COLLECT' : 'NORMAL'}
          >
            <Select.Option value="NORMAL">普通订单（现场收款）</Select.Option>
            <Select.Option value="SELF_COLLECT">自收（水钱已收）</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AssignDeliveryModal;
