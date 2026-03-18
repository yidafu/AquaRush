import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Tag, Space, Button, Card, Typography, message } from 'antd';
import { useLazyQuery } from '@apollo/client';
import { GET_ORDERS_QUERY } from '../../graphql/queries/order.graphql';
import AssignDeliveryButton from '../../components/AssignDeliveryButton';
import { OrderFilters, OrderFilters as FilterType } from './components/OrderFilters';
import { formatAdminTableAmount } from '../utils/money';

const { Title } = Typography;

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

  const [loadOrders, { data, loading }] = useLazyQuery<OrdersQueryResult>(GET_ORDERS_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
    onError: (error) => {
      console.error('Failed to load orders:', error);
      message.error('加载订单失败: ' + error.message);
    },
  });

  const fetchOrders = useCallback(() => {
    const sort = filters.sortBy ? `${filters.sortBy},${filters.sortOrder || 'desc'}` : 'createdAt,desc';

    loadOrders({
      variables: {
        input: {
          page: pagination.current - 1,
          size: pagination.pageSize,
          search: filters.search || undefined,
          status: filters.status || undefined,
          userId: filters.userId || undefined,
          dateFrom: filters.dateFrom || undefined,
          dateTo: filters.dateTo || undefined,
          minAmount: filters.minAmount ? filters.minAmount * 100 : undefined,
          maxAmount: filters.maxAmount ? filters.maxAmount * 100 : undefined,
          deliveryWorkerId: filters.deliveryWorkerId || undefined,
          sort: sort,
        },
      },
    });
  }, [filters, pagination.current, pagination.pageSize, loadOrders]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

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
        <span>{formatAdminTableAmount(amount)}</span>
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
          {record.status === 'PENDING_DELIVERY' && (
            <AssignDeliveryButton
              orderId={record.id}
              onAssigned={() => fetchOrders()}
            />
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
        <OrderFilters
          filters={filters}
          onFiltersChange={handleFiltersChange}
        />
        <Table
          columns={columns}
          dataSource={data?.orders?.content || []}
          loading={loading}
          rowKey="id"
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
          onChange={handleTableChange}
        />
    </div>
  );
};

export default Orders;
