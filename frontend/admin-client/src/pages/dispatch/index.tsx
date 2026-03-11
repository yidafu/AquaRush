import React, { useState, useEffect } from 'react';
import { Table, Tag, Button, Space, Card, Tabs, Modal, Form, Select, message, Row, Col, Statistic } from 'antd';
import { useQuery, useMutation } from '@apollo/client';
import { ASSIGN_DELIVERY_WORKER_MUTATION, BATCH_ASSIGN_DELIVERY_WORKER_MUTATION } from '../../graphql/mutations/order.graphql';
import { GET_PENDING_DELIVERY_ORDERS_QUERY, GET_ONLINE_DELIVERY_WORKERS_QUERY } from '../../graphql/queries/delivery.graphql';
import { formatAdminTableAmount } from '../utils/money.ts';

const { Option } = Select;

// 订单状态映射
const orderStatusMap: Record<string, { text: string; color: string }> = {
  PENDING_PAYMENT: { text: '待支付', color: 'orange' },
  PENDING_DELIVERY: { text: '待配送', color: 'blue' },
  DELIVERING: { text: '配送中', color: 'cyan' },
  COMPLETED: { text: '已完成', color: 'green' },
  CANCELLED: { text: '已取消', color: 'red' },
};

const DispatchPage: React.FC = () => {
  const [selectedOrderIds, setSelectedOrderIds] = useState<number[]>([]);
  const [selectedWorkerId, setSelectedWorkerId] = useState<number | null>(null);
  const [isSelfCollect, setIsSelfCollect] = useState<boolean>(false);
  const [assignModalVisible, setAssignModalVisible] = useState(false);
  const [assignForm] = Form.useForm();

  // 查询待派单订单
  const { data: pendingOrdersData, loading: pendingOrdersLoading, refetch: refetchPendingOrders } = useQuery(
    GET_PENDING_DELIVERY_ORDERS_QUERY,
    {
      errorPolicy: 'all',
    }
  );

  // 查询在线配送员
  const { data: workersData, loading: workersLoading } = useQuery(
    GET_ONLINE_DELIVERY_WORKERS_QUERY,
    {
      errorPolicy: 'all',
    }
  );

  // 派单 mutation
  const [assignDeliveryWorker, { loading: assignLoading }] = useMutation(ASSIGN_DELIVERY_WORKER_MUTATION);

  // 批量派单 mutation
  const [batchAssignDeliveryWorker, { loading: batchAssignLoading }] = useMutation(
    BATCH_ASSIGN_DELIVERY_WORKER_MUTATION
  );

  const pendingOrders = pendingOrdersData?.pendingDeliveryOrders || [];
  const workers = workersData?.onlineDeliveryWorkers || [];

  // 表格列定义
  const columns = [
    {
      title: '订单号',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      width: 150,
    },
    {
      title: '用户',
      dataIndex: ['user', 'nickname'],
      key: 'userName',
    },
    {
      title: '电话',
      dataIndex: ['user', 'phone'],
      key: 'phone',
    },
    {
      title: '地址',
      dataIndex: ['address', 'detailAddress'],
      key: 'address',
      ellipsis: true,
    },
    {
      title: '产品',
      dataIndex: ['product', 'name'],
      key: 'productName',
    },
    {
      title: '数量',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 80,
    },
    {
      title: '金额',
      dataIndex: 'amount',
      key: 'amount',
      render: (amount: number) => formatAdminTableAmount(amount),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const statusInfo = orderStatusMap[status] || { text: status, color: 'default' };
        return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>;
      },
    },
    {
      title: '下单时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: any) => (
        <Space>
          <Button
            type="link"
            onClick={() => {
              setSelectedOrderIds([record.id]);
              setAssignModalVisible(true);
            }}
          >
            派单
          </Button>
        </Space>
      ),
    },
  ];

  // 行选择配置
  const rowSelection = {
    selectedRowKeys: selectedOrderIds,
    onChange: (selectedRowKeys: React.Key[]) => {
      setSelectedOrderIds(selectedRowKeys as number[]);
    },
  };

  // 处理派单
  const handleAssign = async () => {
    if (!selectedWorkerId) {
      message.warning('请选择配送员');
      return;
    }

    try {
      if (selectedOrderIds.length === 1) {
        // 单个派单
        await assignDeliveryWorker({
          variables: {
            orderId: selectedOrderIds[0],
            workerId: selectedWorkerId,
            isSelfCollect: isSelfCollect,
          },
        });
        message.success('派单成功');
      } else {
        // 批量派单
        await batchAssignDeliveryWorker({
          variables: {
            orderIds: selectedOrderIds,
            workerId: selectedWorkerId,
          },
        });
        message.success(`成功派单 ${selectedOrderIds.length} 个订单`);
      }

      setAssignModalVisible(false);
      setSelectedOrderIds([]);
      setSelectedWorkerId(null);
      setIsSelfCollect(false);
      assignForm.resetFields();
      refetchPendingOrders();
    } catch (error: any) {
      message.error(error.message || '派单失败');
    }
  };

  // Tab 项
  const tabItems = [
    {
      key: 'pending',
      label: `待派单 (${pendingOrders.length})`,
      children: (
        <Card>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={8}>
              <Statistic
                title="待派单订单"
                value={pendingOrders.length}
                valueStyle={{ color: '#1890ff' }}
              />
            </Col>
            <Col span={8}>
              <Statistic
                title="已选订单"
                value={selectedOrderIds.length}
                valueStyle={{ color: '#52c41a' }}
              />
            </Col>
            <Col span={8}>
              <Statistic
                title="在线配送员"
                value={workers.length}
                valueStyle={{ color: '#722ed1' }}
              />
            </Col>
          </Row>

          {selectedOrderIds.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <Space>
                <span>已选择 {selectedOrderIds.length} 个订单</span>
                <Button
                  type="primary"
                  onClick={() => setAssignModalVisible(true)}
                  disabled={workers.length === 0}
                >
                  批量派单
                </Button>
                <Button onClick={() => setSelectedOrderIds([])}>取消选择</Button>
              </Space>
            </div>
          )}

          <Table
            rowSelection={rowSelection}
            columns={columns}
            dataSource={pendingOrders}
            loading={pendingOrdersLoading}
            rowKey="id"
            pagination={{ pageSize: 10 }}
          />
        </Card>
      ),
    },
  ];

  return (
    <div>
      <h1>派单管理</h1>
      <Tabs items={tabItems} />

      {/* 派单弹窗 */}
      <Modal
        title="派单"
        open={assignModalVisible}
        onOk={handleAssign}
        onCancel={() => {
          setAssignModalVisible(false);
          setSelectedWorkerId(null);
          setIsSelfCollect(false);
          assignForm.resetFields();
        }}
        confirmLoading={assignLoading || batchAssignLoading}
        width={500}
      >
        <Form form={assignForm} layout="vertical">
          <Form.Item label="订单数量">
            <span>{selectedOrderIds.length} 个订单</span>
          </Form.Item>

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
              {workers.map((worker: any) => (
                <Option key={worker.id} value={worker.id}>
                  {worker.name} - {worker.phone}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item label="收款方式">
            <Select
              placeholder="选择收款方式"
              onChange={(value) => setIsSelfCollect(value === 'SELF_COLLECT')}
              value={isSelfCollect ? 'SELF_COLLECT' : 'NORMAL'}
            >
              <Option value="NORMAL">普通订单（现场收款）</Option>
              <Option value="SELF_COLLECT">自收（水钱已收）</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DispatchPage;
