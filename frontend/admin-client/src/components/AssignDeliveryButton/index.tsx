import React, { useState } from 'react';
import { Button, Modal, Form, Select, message } from '@aquarush/admin-ui';
import { useQuery, useMutation } from '@apollo/client';
import { extractErrorMessage } from '@aquarush/common/utils/network';
import { ASSIGN_DELIVERY_WORKER_MUTATION } from '../../graphql/mutations/order.graphql';
import { GET_ONLINE_DELIVERY_WORKERS_QUERY } from '../../graphql/queries/delivery.graphql';

export interface AssignDeliveryWorker {
  // PrimaryId 序列化为字符串(后端 Long Snowflake)
  id: string;
  name: string;
  phone: string;
  onlineStatus?: string;
  currentTaskCount?: number;
}

interface AssignDeliveryButtonProps {
  orderId: string;
  isSelfCollect?: boolean;
  onAssigned?: () => void;
  type?: 'link' | 'primary' | 'default' | 'dashed' | 'text';
  size?: 'small' | 'middle' | 'large';
  children?: React.ReactNode;
  /** 父页面已通过页面级合并查询取回配送员时传入,跳过组件自身的重复请求(规则 #10) */
  workers?: AssignDeliveryWorker[];
  workersLoading?: boolean;
}

export const AssignDeliveryButton: React.FC<AssignDeliveryButtonProps> = ({
  orderId,
  isSelfCollect: initialIsSelfCollect = false,
  onAssigned,
  type = 'link',
  size = 'small',
  children = '分配配送',
  workers: providedWorkers,
  workersLoading: providedWorkersLoading,
}) => {
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedWorkerId, setSelectedWorkerId] = useState<string | null>(null);
  const [isSelfCollect, setIsSelfCollect] = useState(initialIsSelfCollect);

  // 父页面未提供配送员数据时才自行查询(避免同一页面重复发起在线配送员请求)
  const { data: workersData, loading: workersLoading } = useQuery(GET_ONLINE_DELIVERY_WORKERS_QUERY, {
    skip: Array.isArray(providedWorkers),
  });
  const [assignDeliveryWorker, { loading: assignLoading }] = useMutation(ASSIGN_DELIVERY_WORKER_MUTATION);

  const workers = providedWorkers ?? workersData?.onlineDeliveryWorkers ?? [];
  const loading = providedWorkersLoading ?? workersLoading;

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
    } catch (err) {
      message.error(extractErrorMessage(err, '分配送水员失败'));
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
              loading={loading}
              value={selectedWorkerId}
              onChange={(value) => setSelectedWorkerId(typeof value === 'string' ? value : null)}
            >
              {workers.map((worker: AssignDeliveryWorker) => (
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
