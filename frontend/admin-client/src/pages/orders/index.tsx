import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, message } from '@aquarush/admin-ui';
import type { TableProps } from '@aquarush/admin-ui';
import { useQuery, useMutation } from '@apollo/client';
import { ORDERS_PAGE_DATA_QUERY } from '../../graphql/queries/order.graphql';
import { BATCH_ASSIGN_ORDERS_MUTATION } from '../../graphql/mutations/order.graphql';
import { OrderFilters, OrderFilters as FilterType } from './components/OrderFilters';
import { OrderTable, BatchAssignModal } from './components';
import type { OrdersQueryResult, OrderData } from './types';
import { yuanToCents } from '@aquarush/common';
import { pagination as createPagination } from '@/utils/table';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  page: {
    paddingTop: '24px',
    paddingRight: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
  },
  batchBar: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginBottom: '16px',
  },
});

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
    setBatchModalVisible(true);
  };

  const handleBatchAssign = async (workerId: string) => {
    try {
      const result = await batchAssignOrders({
        variables: {
          orderIds: selectedRowKeys,
          workerId,
        },
      });
      const assignedCount = result.data?.batchAssignOrders?.length ?? 0;
      message.success(`批量派单成功（${assignedCount} 单）`);
      setBatchModalVisible(false);
      setSelectedRowKeys([]);
      fetchOrders();
    } catch {
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

  const handleTableChange: TableProps<OrderData>['onChange'] = (newPagination, _filters, sorter) => {
    setPagination({
      current: newPagination.current ?? 1,
      pageSize: newPagination.pageSize ?? 20,
      total: pagination.total,
    });

    const activeSorter = Array.isArray(sorter) ? sorter[0] : sorter;
    if (activeSorter?.field && activeSorter.order) {
      const sortBy = activeSorter.field as string;
      const sortOrder = activeSorter.order === 'ascend' ? 'asc' : 'desc';
      setFilters(prev => ({ ...prev, sortBy, sortOrder }));
    }
  };

  // 仅待派单订单可勾选批量派单(R-ORD-08:同一订单仅派单一次,已派单/已接单不可再派)
  const rowSelection = {
    selectedRowKeys,
    onChange: (keys: React.Key[]) => setSelectedRowKeys(keys),
    getCheckboxProps: (record: { status: string }) => ({
      disabled: record.status !== 'PENDING_DISPATCH',
    }),
  };

  return (
    <div {...stylex.props(styles.page)}>
      <OrderFilters
        filters={filters}
        onFiltersChange={handleFiltersChange}
      />
      <div {...stylex.props(styles.batchBar)}>
        <Button
          type="primary"
          onClick={openBatchAssign}
          disabled={selectedRowKeys.length === 0}
        >
          批量派单{selectedRowKeys.length > 0 ? `（已选 ${selectedRowKeys.length} 单）` : ''}
        </Button>
      </div>
      <OrderTable
        dataSource={data?.orders?.content || []}
        loading={loading}
        pagination={createPagination(pagination.total, {
          current: pagination.current,
          pageSize: pagination.pageSize,
          showSizeChanger: true,
          showQuickJumper: true,
        })}
        rowSelection={rowSelection}
        workers={workers}
        workersLoading={workersLoading}
        onView={(record) => navigate(`/orders/${record.id}`)}
        onAssigned={fetchOrders}
        onChange={handleTableChange}
      />

      {/* 批量派单弹窗(G10):多选待派单订单一次派给同一送水工 */}
      <BatchAssignModal
        open={batchModalVisible}
        selectedCount={selectedRowKeys.length}
        workers={workers}
        workersLoading={workersLoading}
        confirmLoading={batchAssigning}
        onOk={handleBatchAssign}
        onCancel={() => setBatchModalVisible(false)}
      />
    </div>
  );
};

export default Orders;
