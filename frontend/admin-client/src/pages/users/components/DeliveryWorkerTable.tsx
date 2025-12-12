import React from 'react';
import { Table, Button, Space, Tag, Progress, Avatar } from 'antd';
import { EditOutlined, LockOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { DeliveryWorker, TableActionProps } from './types';
import { WORKER_STATUS_MAPPINGS } from './types';

interface DeliveryWorkerTableProps extends TableActionProps {
  data: DeliveryWorker[];
  loading?: boolean;
}

const DeliveryWorkerTable: React.FC<DeliveryWorkerTableProps> = ({
  data,
  loading = false,
  onEdit,
  onView,
  onDelete,
  onResetPassword,
}) => {
  const columns: ColumnsType<DeliveryWorker> = [
    {
      title: '头像',
      dataIndex: 'avatarUrl',
      key: 'avatarUrl',
      width: 80,
      render: (avatarUrl: string, record: DeliveryWorker) => (
        <Avatar
          src={avatarUrl}
          size={40}
          style={{ backgroundColor: '#87d068' }}
        >
          {record.name?.charAt(0)?.toUpperCase()}
        </Avatar>
      ),
    },
    {
      title: '姓名',
      dataIndex: 'name',
      key: 'name',
      width: 120,
      fixed: 'left',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
    },
    {
      title: '微信OpenID',
      dataIndex: 'wechatOpenId',
      key: 'wechatOpenId',
      width: 200,
      render: (wechatOpenId: string) => (
        <span title={wechatOpenId}>
          {wechatOpenId ? `${wechatOpenId.substring(0, 8)}...` : '-'}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string, record: DeliveryWorker) => {
        const statusConfig = WORKER_STATUS_MAPPINGS[status as keyof typeof WORKER_STATUS_MAPPINGS];
        return (
          <Space direction="vertical" size="small">
            <Tag color={statusConfig.color}>{statusConfig.text}</Tag>
            {!record.isAvailable && (
              <Tag color="default">不可接单</Tag>
            )}
          </Space>
        );
      },
    },
    {
      title: '评分',
      dataIndex: 'averageRating',
      key: 'averageRating',
      width: 120,
      render: (averageRating: number, record: DeliveryWorker) => {
        const rating = averageRating || record.rating || 0;
        return (
          <Space direction="vertical" size="small">
            <Progress
              percent={Math.min(rating * 20, 100)}
              size="small"
              showInfo={false}
              strokeColor={rating >= 4 ? '#52c41a' : rating >= 3 ? '#faad14' : '#ff4d4f'}
            />
            <span style={{ fontSize: '12px' }}>{rating.toFixed(1)} 分</span>
          </Space>
        );
      },
    },
    {
      title: '订单统计',
      key: 'orderStats',
      width: 120,
      render: (_, record: DeliveryWorker) => (
        <Space direction="vertical" size="small">
          <span style={{ fontSize: '12px' }}>总单: {record.totalOrders}</span>
          <span style={{ fontSize: '12px' }}>完成: {record.completedOrders}</span>
          <span style={{ fontSize: '12px', color: '#1890ff' }}>
            {record.totalOrders > 0
              ? `${((record.completedOrders / record.totalOrders) * 100).toFixed(1)}%`
              : '0%'
            }
          </span>
        </Space>
      ),
    },
    {
      title: '总收入',
      dataIndex: 'earning',
      key: 'earning',
      width: 100,
      render: (earning: number) => (
        <span style={{ fontWeight: 'bold', color: '#52c41a' }}>
          ¥{earning ? earning.toFixed(2) : '0.00'}
        </span>
      ),
    },
    {
      title: '当前位置',
      dataIndex: 'currentLocation',
      key: 'currentLocation',
      width: 150,
      render: (currentLocation: string) => (
        <span title={currentLocation}>
          {currentLocation || '-'}
        </span>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (createdAt: string) => new Date(createdAt).toLocaleString('zh-CN'),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => onView(record)}
            size="small"
          >
            查看
          </Button>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => onEdit(record)}
            size="small"
          >
            编辑
          </Button>
          <Button
            type="link"
            icon={<LockOutlined />}
            onClick={() => onResetPassword(record)}
            size="small"
          >
            重置密码
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => onDelete(record)}
            size="small"
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={data}
      rowKey="id"
      loading={loading}
      scroll={{ x: 1400 }}
      pagination={{
        showSizeChanger: true,
        showQuickJumper: true,
        showTotal: (total, range) =>
          `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`,
        pageSizeOptions: ['10', '20', '50', '100'],
        defaultPageSize: 20,
      }}
      size="middle"
    />
  );
};

export default DeliveryWorkerTable;