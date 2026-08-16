import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Tag, Space, Button, Card, message, Modal, Form, Select } from 'antd';
import { useQuery, useMutation } from '@apollo/client';
import { ORDERS_PAGE_DATA_QUERY } from '../../graphql/queries/order.graphql';
import { BATCH_ASSIGN_ORDERS_MUTATION } from '../../graphql/mutations/order.graphql';
import AssignDeliveryButton from '../../components/AssignDeliveryButton';
import { OrderFilters, OrderFilters as FilterType } from './components/OrderFilters';
import { displayCents, yuanToCents } from '@aquarush/common';

interface OrderData {
  id: number;
  orderNo: string;
  user: {
    id: number;
    nickname: string;
    phone: string;
  };
  status: string;
  amount: number;
  address: {
    receiverName: string;
    phone: string;
    province: string;
    city: string;
    district: string;
    detailAddress: string;
  };
  deliveryWorker: {
    id: number;
    name: string;
    phone: string;
  } | null;
  createdAt: string;
  updatedAt: string;
}

interface OrdersQueryResult {
  orders: {
    content: OrderData[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  };
  onlineDeliveryWorkers: Array<{
    id: number;
    name: string;
    phone: string;
    onlineStatus: string;
    currentTaskCount?: number;
  }>;
}

const ORDER_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING_PAYMENT: { text: '待支付', color: 'default' },
  PENDING_DELIVERY: { text: '待配送', color: 'blue' },
  DELIVERING: { text: '配送中', color: 'processing' },
  COMPLETED: { text: '已完成', color: 'green' },
  CANCELLED: { text: '已取消', color: 'red' },
  REFUNDED: { text: '已退款', color: 'magenta' },
};

const Orders: React.FC = () => {
  const navigate = useNavigate();
  const [filters, setFilters] = useState<FilterType>({
    search: '',
    status: undefined,
    userId: undefined,
    dateFrom: undefined,
    dateTo: undefined,
    minAmount: undefined,
    maxAmount: undefined,
    deliveryWorkerId: undefined,
    sortBy: 'createdAt',
    sortOrder: 'desc',
  });

  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });

  // ==================== 批量派单(F-ORD-022,G10) ====================
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [batchModalVisible, setBatchModalVisible] = useState(false);
  const [batchWorkerId, setBatchWorkerId] = useState<number | null>(null);
  const [batchAssignOrders, { loading: batchAssigning }] = useMutation(BATCH_ASSIGN_ORDERS_MUTATION, {
    onError: (error) => {
      console.error('批量派单失败:', error);
      message.error('批量派单失败: ' + error.message);
    },
  });

  const openBatchAssign = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先勾选待派单订单');
      return;
    }
    setBatchWorkerId(null);
    setBatchModalVisible(true);
  };

  const handleBatchAssign = async () => {
    if (!batchWorkerId) {
      message.warning('请选择送水员');
      return;
    }
    try {
      const result = await batchAssignOrders({
        variables: {
          orderIds: selectedRowKeys,
          workerId: batchWorkerId,
        },
      });
      const assignedCount = result.data?.batchAssignOrders?.length ?? 0;
      message.success(`批量派单成功（${assignedCount} 单）`);
      setBatchModalVisible(false);
      setSelectedRowKeys([]);
      fetchOrders();
    } catch (err) {
      // onError 已提示
    }
  };

  // 单页单请求:订单分页 + 在线配送员(批量派单下拉)一次取回(规则 #10);
  // 筛选/分页/排序变化经 variables 变化自动重取
  const { data, loading, refetch } = useQuery<OrdersQueryResult>(ORDERS_PAGE_DATA_QUERY, {
    variables: {
      input: {
        page: pagination.current - 1,
        size: pagination.pageSize,
        search: filters.search || undefined,
        status: filters.status || undefined,
        userId: filters.userId || undefined,
        dateFrom: filters.dateFrom || undefined,
        dateTo: filters.dateTo || undefined,
        minAmount: filters.minAmount ? yuanToCents(filters.minAmount) : undefined,
        maxAmount: filters.maxAmount ? yuanToCents(filters.maxAmount) : undefined,
        deliveryWorkerId: filters.deliveryWorkerId || undefined,
        sort: filters.sortBy ? `${filters.sortBy},${filters.sortOrder || 'desc'}` : 'createdAt,desc',
      },
    },
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
    onError: (error) => {
      console.error('Failed to load orders:', error);
      message.error('加载订单失败: ' + error.message);
    },
  });

  const fetchOrders = useCallback(() => {
    refetch();
  }, [refetch]);

  const workers = data?.onlineDeliveryWorkers ?? [];
  const workersLoading = loading;

  useEffect(() => {
    if (data?.orders) {
      setPagination(prev => ({
        ...prev,
        total: data.orders.totalElements,
      }));
    }
  }, [data]);

  const handleFiltersChange = (newFilters: FilterType) => {
    setFilters(prev => ({ ...prev, ...newFilters }));
    setPagination(prev => ({ ...prev, current: 1 }));
  };

  const handleTableChange = (newPagination: any, _filters: any, sorter: any) => {
    setPagination({
      current: newPagination.current,
      pageSize: newPagination.pageSize,
      total: pagination.total,
    });

    if (sorter.field && sorter.order) {
      const sortBy = sorter.field as string;
      const sortOrder = sorter.order === 'ascend' ? 'asc' : 'desc';
      setFilters(prev => ({ ...prev, sortBy, sortOrder }));
    }
  };

  const columns = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      sorter: true,
    },
    {
      title: '用户',
      dataIndex: ['user', 'nickname'],
      key: 'userName',
      render: (_: any, record: OrderData) => (
        <Space direction="vertical" size={0}>
          <span>{record.user?.nickname || '-'}</span>
          <span style={{ fontSize: '12px', color: '#999' }}>{record.user?.phone || '-'}</span>
        </Space>
      ),
    },
    {
      title: '收货地址',
      dataIndex: ['address', 'detailAddress'],
      key: 'address',
      render: (_: any, record: OrderData) => {
        const addr = record.address;
        if (!addr) return '-';
        return `${addr.province}${addr.city}${addr.district}${addr.detailAddress}`;
      },
      ellipsis: true,
    },
    {
      title: '金额',
      dataIndex: 'amount',
      key: 'amount',
      sorter: true,
      render: (amount: number | null | undefined) => (
        <span>{displayCents(amount)}</span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const statusInfo = ORDER_STATUS_MAP[status] || { text: status, color: 'default' };
        return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>;
      },
    },
    {
      title: '配送员',
      dataIndex: ['deliveryWorker', 'name'],
      key: 'deliveryWorker',
      render: (_name: string | null, record: OrderData) => (
        record.deliveryWorker ? (
          <Space direction="vertical" size={0}>
            <span>{record.deliveryWorker.name || '-'}</span>
            <span style={{ fontSize: '12px', color: '#999' }}>{record.deliveryWorker.phone || '-'}</span>
          </Space>
        ) : '-'
      ),
    },
    {
      title: '下单时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      sorter: true,
      render: (date: string) => date ? new Date(date).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: OrderData) => (
        <Space>
          <Button type="link" size="small" onClick={() => navigate(`/orders/${record.id}`)}>
            查看
          </Button>
          {/* 派单前置状态=待派单(R-ORD-01 状态机「待派单→配送中」,G09) */}
          {record.status === 'PENDING_DISPATCH' && (
            <AssignDeliveryButton
              orderId={record.id}
              workers={workers}
              workersLoading={workersLoading}
              onAssigned={() => fetchOrders()}
            />
          )}
        </Space>
      ),
    },
  ];

  // 仅待派单订单可勾选批量派单(R-ORD-08:同一订单仅派单一次,已派单/已接单不可再派)
  const rowSelection = {
    selectedRowKeys,
    onChange: (keys: React.Key[]) => setSelectedRowKeys(keys),
    getCheckboxProps: (record: OrderData) => ({
      disabled: record.status !== 'PENDING_DISPATCH',
    }),
  };

  return (
    <div style={{ padding: '24px' }}>
        <OrderFilters
          filters={filters}
          onFiltersChange={handleFiltersChange}
        />
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '16px' }}>
          <Button
            type="primary"
            onClick={openBatchAssign}
            disabled={selectedRowKeys.length === 0}
          >
            批量派单{selectedRowKeys.length > 0 ? `（已选 ${selectedRowKeys.length} 单）` : ''}
          </Button>
        </div>
        <Table
          columns={columns}
          dataSource={data?.orders?.content || []}
          loading={loading}
          rowKey="id"
          rowSelection={rowSelection}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
          onChange={handleTableChange}
        />

        {/* 批量派单弹窗(G10):多选待派单订单一次派给同一送水工 */}
        <Modal
          title={`批量派单（${selectedRowKeys.length} 单）`}
          open={batchModalVisible}
          onOk={handleBatchAssign}
          onCancel={() => setBatchModalVisible(false)}
          confirmLoading={batchAssigning}
          width={500}
        >
          <Form layout="vertical">
            <Form.Item label="选择送水员" required>
              <Select
                placeholder="选择送水员（仅在线可派单，R-ORD-08）"
                loading={workersLoading}
                value={batchWorkerId}
                onChange={setBatchWorkerId}
              >
                {workers.map((worker: any) => (
                  <Select.Option key={worker.id} value={worker.id}>
                    {worker.name} - {worker.phone}
                    {worker.onlineStatus === 'ONLINE' ? '' : '（离线）'}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Form>
        </Modal>
    </div>
  );
};

export default Orders;
