import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Space, Button, Card, Popconfirm, Input, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import { displayCents, formatDateTime } from '@aquarush/common';
import { BRAND_PRIMARY } from '../../../config/theme';
import type { FlashSaleActivity } from '@aquarush/common';
import {
  useFlashSaleActivities,
  useCreateFlashSaleActivity,
  useUpdateFlashSaleActivity,
  useDisableFlashSaleActivity,
} from '../../../services/marketing-graphql';
import FlashSaleFormModal, { type FlashSaleFormPayload } from './components/FlashSaleFormModal';
import { pagination } from '@/utils/table';

const { Text } = Typography;

const DISPLAY_STATUS_META: Record<string, { text: string; color: string }> = {
  UPCOMING: { text: '未开始', color: 'default' },
  ONGOING: { text: '进行中', color: 'green' },
  ENDED: { text: '已结束', color: 'orange' },
  DISABLED: { text: '已停用', color: 'red' },
};

// 限时特价活动页(F-MKT-005):商品级活动价,活动期自动按特价计价(单价链③)
const FlashSale: React.FC = () => {
  const navigate = useNavigate();
  const [loadActivities, { data, loading }] = useFlashSaleActivities();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<FlashSaleActivity | null>(null);

  const [createActivity, { loading: creating }] = useCreateFlashSaleActivity();
  const [updateActivity, { loading: updating }] = useUpdateFlashSaleActivity();
  const [disableActivity, { loading: disabling }] = useDisableFlashSaleActivity();

  const load = useCallback(
    (p = page, s = size, kw = keyword) => {
      loadActivities({ variables: { input: { page: p, size: s, keyword: kw || undefined } } });
    },
    [loadActivities, page, size, keyword],
  );

  useEffect(() => {
    load();
  }, [load]);

  const handleTableChange = (pagination: any) => {
    const nextPage = pagination.current - 1;
    setPage(nextPage);
    setSize(pagination.pageSize);
    load(nextPage, pagination.pageSize);
  };

  const handleCreate = () => {
    setEditingRecord(null);
    setModalOpen(true);
  };

  const handleEdit = (record: FlashSaleActivity) => {
    setEditingRecord(record);
    setModalOpen(true);
  };

  const handleSubmit = async (payload: FlashSaleFormPayload, record: FlashSaleActivity | null) => {
    if (record) {
      await updateActivity({ variables: { id: record.id, input: payload } });
    } else {
      await createActivity({ variables: { input: payload } });
    }
    setModalOpen(false);
  };

  const handleDisable = async (record: FlashSaleActivity) => {
    await disableActivity({ variables: { id: record.id } });
  };

  const columns: ColumnsType<FlashSaleActivity> = [
    { title: '活动名称', dataIndex: 'name', width: 180, ellipsis: true },
    { title: '商品', dataIndex: 'productName', width: 140, ellipsis: true },
    {
      title: '目录价',
      dataIndex: 'catalogPriceCents',
      width: 110,
      render: (cents: number | null) => (cents != null ? displayCents(cents) : '--'),
    },
    {
      title: '特价',
      dataIndex: 'salePriceCents',
      width: 110,
      render: (cents: number) => (
        <Text strong style={{ color: BRAND_PRIMARY }}>
          {displayCents(cents)}
        </Text>
      ),
    },
    {
      title: '时间窗',
      width: 240,
      render: (_, record) => `${formatDateTime(record.validFrom)} ~ ${formatDateTime(record.validTo)}`,
    },
    {
      title: '展示状态',
      dataIndex: 'currentStatus',
      width: 100,
      render: (status: string) => {
        const meta = DISPLAY_STATUS_META[status] ?? { text: status, color: 'default' };
        return <Tag color={meta.color}>{meta.text}</Tag>;
      },
    },
    {
      title: '操作',
      width: 200,
      render: (_, record) => (
        <Space size="small">
          <Button size="small" onClick={() => navigate(`/marketing/flash-sale/${record.id}?name=${encodeURIComponent(record.name)}`)}>
            详情
          </Button>
          <Button size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          {record.status === 'ACTIVE' && (
            <Popconfirm
              title={`确定停用活动「${record.name}」吗?`}
              description="停用后商品立即回退分组价/目录价"
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

  const activities = data?.flashSaleActivities?.content ?? [];
  const total = data?.flashSaleActivities?.totalElements ?? 0;

  return (
    <Card
      title="限时特价"
      extra={
        <Space>
          <Input.Search
            placeholder="搜索活动名称"
            allowClear
            style={{ width: 220 }}
            onSearch={(value) => {
              setKeyword(value);
              load(0, size, value);
            }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建活动
          </Button>
        </Space>
      }
    >
      <Table<FlashSaleActivity>
        rowKey="id"
        columns={columns}
        dataSource={activities}
        loading={loading}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: size,
          showSizeChanger: true,
        })}
        onChange={handleTableChange}
        scroll={{ x: 1100 }}
      />
      <FlashSaleFormModal
        open={modalOpen}
        record={editingRecord}
        submitting={creating || updating}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </Card>
  );
};

export default FlashSale;
