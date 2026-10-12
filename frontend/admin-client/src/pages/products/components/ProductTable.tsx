import React from 'react';
import { Table, Button, Space, Tag, message, Popconfirm, type PaginationProps } from '@aquarush/admin-ui';
import { EditOutlined, DeleteOutlined, EyeOutlined } from '@aquarush/admin-ui/icons';
import { useNavigate } from 'react-router-dom';
import { useDeleteProduct } from '../../../services/product-graphql';
import { displayCents } from '@aquarush/common'

interface Product {
  id: number;
  name: string;
  subtitle?: string;
  priceCents: number;
  originalPriceCents?: number;
  coverImageUrl?: string;
  imageGallery?: string;
  specification: string;
  waterSource?: string;
  mineralContent?: string;
  stock: number;
  salesVolume: number;
  status: 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK' | 'ACTIVE';
  sortOrder: number;
  tags?: string;
  detailContent?: string;
  certificateImages?: string;
  createdAt: string;
  updatedAt: string;
}

interface ProductTableProps {
  data?: Product[];
  loading?: boolean;
  pagination?: false | PaginationProps;
  // 库存预警阈值(F-SYS-003,R-PRO-02):缺省 10/50,商品列表着色按系统参数动态计算
  stockThresholds?: { low: number; mid: number };
  onViewDetail: (productId: number) => void;
  onChange?: (pagination: any, filters: any, sorter: any) => void;
}

export const ProductTable: React.FC<ProductTableProps> = ({
  data = [],
  loading = false,
  pagination = false,
  stockThresholds = { low: 10, mid: 50 },
  onViewDetail,
  onChange
}) => {
  const navigate = useNavigate();
  const [deleteProduct] = useDeleteProduct();

  const columns = [
    {
      title: '商品名称',
      dataIndex: 'name',
      key: 'name',
      ellipsis: true,
      width: 150,
    },
    {
      title: '副标题',
      dataIndex: 'subtitle',
      key: 'subtitle',
      ellipsis: true,
      width: 150,
      sorter: (a: Product, b: Product) => a.subtitle?.localeCompare(b.subtitle || '') || 0,
    },
    {
      title: '规格',
      dataIndex: 'specification',
      key: 'specification',
      width: 100,
      sorter: (a: Product, b: Product) => a.specification.localeCompare(b.specification),
    },
    {
      title: '价格',
      dataIndex: 'priceCents',
      key: 'priceCents',
      render: (price: number) => displayCents(price),
      sorter: (a: Product, b: Product) => a.priceCents - b.priceCents,
      width: 100,
    },
    {
      title: '原价',
      dataIndex: 'originalPriceCents',
      key: 'originalPriceCents',
      render: (price: number) => price ? <del>{displayCents(price)}</del> : '-',
      sorter: (a: Product, b: Product) => (a.originalPriceCents || 0) - (b.originalPriceCents || 0),
      width: 100,
    },
    {
      title: '库存',
      dataIndex: 'stock',
      key: 'stock',
      render: (stock: number) => (
        <Tag color={stock < stockThresholds.low ? 'red' : stock < stockThresholds.mid ? 'orange' : 'green'}>
          {stock}
        </Tag>
      ),
      sorter: (a: Product, b: Product) => a.stock - b.stock,
      width: 80,
    },
    {
      title: '销量',
      dataIndex: 'salesVolume',
      key: 'salesVolume',
      sorter: (a: Product, b: Product) => a.salesVolume - b.salesVolume,
      width: 100,
    },
    {
      title: '排序权重',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      sorter: (a: Product, b: Product) => a.sortOrder - b.sortOrder,
      width: 100,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const statusConfig = {
          ONLINE: { color: 'green', text: '在售' },
          OFFLINE: { color: 'red', text: '下架' },
          OUT_OF_STOCK: { color: 'orange', text: '缺货' },
          ACTIVE: { color: 'blue', text: '活跃' },
        };
        const config = statusConfig[status as keyof typeof statusConfig];
        return config ? <Tag color={config.color}>{config.text}</Tag> : <Tag>{status}</Tag>;
      },
      width: 100,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => new Date(date).toLocaleString('zh-CN'),
      sorter: (a: Product, b: Product) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
      width: 150,
    },
    {
      title: '操作',
      key: 'actions',
      width: 280,
      render: (_: unknown, record: Product) => (
        <Space size="middle">
          <Button
            icon={<EyeOutlined />}
            onClick={() => onViewDetail(record.id)}
            size="small"
          >
            详情
          </Button>
          <Button
            icon={<EditOutlined />}
            onClick={() => navigate(`/products/${record.id}/edit`)}
            size="small"
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个商品吗？"
            description="删除后无法恢复，请谨慎操作。"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button
              icon={<DeleteOutlined />}
              danger
              size="small"
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleDelete = async (id: number) => {
    try {
      await deleteProduct({ variables: { id } });
      message.success('删除成功');
    } catch (err) {
      message.error('删除失败');
    }
  };

  return (
    <Table
      columns={columns}
      dataSource={data}
      loading={loading}
      rowKey="id"
      pagination={pagination}
      onChange={onChange}
      scroll={{ x: 1200 }}
    />
  );
};
