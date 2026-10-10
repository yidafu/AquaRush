import React, { useState, useMemo } from 'react';
import { Table, Tag, Space, Button, Card, Popconfirm, Input } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { displayCents, formatDateTime } from '@aquarush/common';
import type { TicketTypeAdminVo } from '@aquarush/common';
import { useQuery } from '@apollo/client';
import { useNavigate } from 'react-router-dom';
import { WATER_TICKETS_PAGE_DATA_QUERY } from '../../../graphql/queries/marketing.graphql';
import {
  useCreateTicketType,
  useUpdateTicketType,
  useDisableTicketType,
} from '../../../services/marketing-graphql';
import TicketTypeFormModal, { type TicketTypeFormPayload } from './components/TicketTypeFormModal';
import { pagination } from '@/utils/table';

// 桶水票票种管理页(F-MKT-007):列表/新建/编辑/停用/详情统计图表(独立页面)
const WaterTickets: React.FC = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<TicketTypeAdminVo | null>(null);
  const navigate = useNavigate();

  // 详情统计页入口(独立页面:/marketing/water-tickets/:id?name=&kind=)
  const goDetail = (record: TicketTypeAdminVo): void => {
    navigate(`/marketing/water-tickets/${record.id}?name=${encodeURIComponent(record.name)}&kind=${record.ticketType}`);
  };

  const [createTicketType, { loading: creating }] = useCreateTicketType();
  const [updateTicketType, { loading: updating }] = useUpdateTicketType();
  const [disableTicketType, { loading: disabling }] = useDisableTicketType();

  // 单页单请求:票种分页 + 商品目录(指定商品票展示商品名)一次取回(规则 #10);
  // 分页/关键字变化经 variables 变化自动重取
  const { data, loading } = useQuery(WATER_TICKETS_PAGE_DATA_QUERY, {
    variables: {
      input: { page, size, keyword: keyword || undefined },
      productsInput: { page: 0, size: 1000 },
    },
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });

  const productMap = useMemo(() => {
    const map = new Map<string, string>();
    (data?.productsPaginated?.list ?? []).forEach((product: { id: number | string; name: string; specification?: string | null }) => {
      map.set(String(product.id), product.specification ? `${product.name}(${product.specification})` : product.name);
    });
    return map;
  }, [data]);

  const handleTableChange = (pagination: any) => {
    setPage(pagination.current - 1);
    setSize(pagination.pageSize);
  };

  const handleCreate = () => {
    setEditingRecord(null);
    setModalOpen(true);
  };

  const handleEdit = (record: TicketTypeAdminVo) => {
    setEditingRecord(record);
    setModalOpen(true);
  };

  const handleSubmit = async (payload: TicketTypeFormPayload, record: TicketTypeAdminVo | null) => {
    if (record) {
      await updateTicketType({ variables: { id: record.id, input: payload } });
    } else {
      await createTicketType({ variables: { input: payload } });
    }
    setModalOpen(false);
  };

  const handleDisable = async (record: TicketTypeAdminVo) => {
    await disableTicketType({ variables: { id: record.id } });
  };

  const renderValidity = (record: TicketTypeAdminVo) => {
    if (record.validityType === 'FIXED_RANGE') {
      return record.validFrom && record.validTo
        ? `${formatDateTime(record.validFrom)} ~ ${formatDateTime(record.validTo)}`
        : '—';
    }
    return `购买后${record.validDays ?? '—'}天内有效`;
  };

  const columns: ColumnsType<TicketTypeAdminVo> = [
    { title: '票种名', dataIndex: 'name', width: 180, ellipsis: true },
    {
      title: '类型',
      dataIndex: 'ticketType',
      width: 110,
      render: (type: string) =>
        type === 'SPECIFIC_PRODUCT' ? <Tag color="blue">指定商品</Tag> : <Tag color="purple">通兑票</Tag>,
    },
    {
      title: '适用',
      width: 200,
      render: (_, record) => {
        if (record.ticketType === 'SPECIFIC_PRODUCT') {
          return record.productId ? productMap.get(String(record.productId)) ?? `商品#${record.productId}` : '—';
        }
        return record.specification || '—';
      },
    },
    {
      title: '套餐',
      width: 160,
      render: (_, record) => `${record.packageCount}张 / ${displayCents(record.packagePriceCents)}`,
    },
    {
      title: '赠送',
      dataIndex: 'giftCount',
      width: 80,
      render: (giftCount: number) => (giftCount > 0 ? `+${giftCount}张` : '—'),
    },
    { title: '有效期', width: 240, render: (_, record) => renderValidity(record) },
    {
      title: '限购',
      dataIndex: 'perUserLimit',
      width: 90,
      render: (perUserLimit: number) => (perUserLimit > 0 ? `${perUserLimit}套` : '不限'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status: string) => (status === 'ACTIVE' ? <Tag color="green">上架</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      width: 220,
      render: (_, record) => (
        <Space size="small">
          <Button size="small" onClick={() => goDetail(record)}>
            详情
          </Button>
          <Button size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          {record.status === 'ACTIVE' && (
            <Popconfirm
              title={`确定停用票种「${record.name}」吗?`}
              description="停用后用户不可再购买,已购票不受影响"
              okText="停用"
              okButtonProps={{ danger: true }}
              onConfirm={() => handleDisable(record)}
            >
              <Button size="small" danger loading={disabling}>
                停用
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const ticketTypes = data?.ticketTypes?.content ?? [];
  const total = data?.ticketTypes?.totalElements ?? 0;

  return (
    <Card
      title="桶水票票种"
      extra={
        <Space>
          <Input.Search
            placeholder="搜索票种名称"
            allowClear
            style={{ width: 220 }}
            onSearch={(value) => {
              setPage(0);
              setKeyword(value);
            }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建票种
          </Button>
        </Space>
      }
    >
      <Table<TicketTypeAdminVo>
        rowKey="id"
        columns={columns}
        dataSource={ticketTypes}
        loading={loading}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: size,
          showSizeChanger: true,
        })}
        onChange={handleTableChange}
        scroll={{ x: 1200 }}
      />
      <TicketTypeFormModal
        open={modalOpen}
        record={editingRecord}
        submitting={creating || updating}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </Card>
  );
};

export default WaterTickets;
