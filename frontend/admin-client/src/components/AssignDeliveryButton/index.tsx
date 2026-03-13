import React, { useState } from 'react';
import { Button, Modal, Form, Select, message } from 'antd';
import { useQuery, useMutation } from '@apollo/client';
import { ASSIGN_DELIVERY_WORKER_MUTATION } from '../../graphql/mutations/order.graphql';
import { GET_ONLINE_DELIVERY_WORKERS_QUERY } from '../../graphql/queries/delivery.graphql';

interface AssignDeliveryButtonProps {
  orderId: number;
  isSelfCollect?: boolean;
  onAssigned?: () => void;
  type?: 'link' | 'primary' | 'default' | 'dashed' | 'text';
  size?: 'small' | 'middle' | 'large';
  children?: React.ReactNode;
}

export const AssignDeliveryButton: React.FC<AssignDeliveryButtonProps> = ({
  orderId,
  isSelfCollect: initialIsSelfCollect = false,
  onAssigned,
  type = 'link',
  size = 'small',
  children = '分配配送',
}) => {
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedWorkerId, setSelectedWorkerId] = useState<number | null>(null);
  const [isSelfCollect, setIsSelfCollect] = useState(initialIsSelfCollect);

  const { data: workersData, loading: workersLoading } = useQuery(GET_ONLINE_DELIVERY_WORKERS_QUERY);
  const [assignDeliveryWorker, { loading: assignLoading }] = useMutation(ASSIGN_DELIVERY_WORKER_MUTATION);

  const workers = workersData?.onlineDeliveryWorkers || [];

  const handleOpen = () => {
    setSelectedWorkerId(null);
    setIsSelfCollect(initialIsSelfCollect);
    setModalVisible(true);
  };

  const handleAssign = async () => {
    if (!selectedWorkerId) {
      message.warning('请选择送水员');
      return;
    }
    try {
      await assignDeliveryWorker({
        variables: {
          orderId,
          workerId: selectedWorkerId,
          isSelfCollect,
        },
      });
      message.success('分配送水员成功');
      setModalVisible(false);
      onAssigned?.();
    } catch (err: any) {
      message.error(err.message || '分配送水员失败');
    }
  };

  return (
    <>
      <Button type={type} size={size} onClick={handleOpen}>
        {children}
      </Button>
      <Modal
        title="分配送水员"
        open={modalVisible}
        onOk={handleAssign}
        onCancel={() => setModalVisible(false)}
        confirmLoading={assignLoading}
        width={500}
      >
        <Form layout="vertical">
          <Form.Item label="选择送水员" required>
            <Select
              placeholder="选择送水员"
              loading={workersLoading}
              value={selectedWorkerId}
              onChange={setSelectedWorkerId}
            >
              {workers.map((worker: any) => (
                <Select.Option key={worker.id} value={worker.id}>
                  {worker.name} - {worker.phone}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item label="收款方式">
            <Select
              value={isSelfCollect ? 'SELF_COLLECT' : 'NORMAL'}
              onChange={(value) => setIsSelfCollect(value === 'SELF_COLLECT')}
            >
              <Select.Option value="NORMAL">普通订单（现场收款）</Select.Option>
              <Select.Option value="SELF_COLLECT">自收（水钱已收）</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default AssignDeliveryButton;
