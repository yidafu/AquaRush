import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Space, Button, Card, Popconfirm, Input, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { displayCents, formatDateTime } from '@aquarush/common';
import type { GiftActivity } from '@aquarush/common';
import {
  useGiftActivities,
  useCreateGiftActivity,
  useUpdateGiftActivity,
  useDisableGiftActivity,
} from '../../../services/marketing-graphql';
import GiftActivityFormModal, { type GiftFormPayload } from './components/GiftActivityFormModal';

const { Text } = Typography;

const GIFT_TYPE_LABELS: Record<string, string> = {
  FULL_GIFT: '满赠',
  BUY_GIFT: '买赠',
};

const DISPLAY_STATUS_META: Record<string, { text: string; color: string }> = {
  UPCOMING: { text: '未开始', color: 'default' },
  ONGOING: { text: '进行中', color: 'green' },
  ENDED: { text: '已结束', color: 'orange' },
  DISABLED: { text: '已停用', color: 'red' },
};

// 买赠/满赠活动页(F-MKT-008):订单达标自动赠指定商品,赠品为附加 0 价行随单配送
const BuyGift: React.FC = () => {
  const [loadActivities, { data, loading }] = useGiftActivities();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<GiftActivity | null>(null);

  const [createActivity, { loading: creating }] = useCreateGiftActivity();
  const [updateActivity, { loading: updating }] = useUpdateGiftActivity();
  const [disableActivity, { loading: disabling }] = useDisableGiftActivity();

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

  const handleEdit = (record: GiftActivity) => {
    setEditingRecord(record);
    setModalOpen(true);
  };

  const handleSubmit = async (payload: GiftFormPayload, record: GiftActivity | null) => {
    if (record) {
      await updateActivity({ variables: { id: record.id, input: payload } });
    } else {
      await createActivity({ variables: { input: payload } });
    }
    setModalOpen(false);
  };

  const handleDisable = async (record: GiftActivity) => {
    await disableActivity({ variables: { id: record.id } });
  };

  const renderThreshold = (record: GiftActivity) => {
    if (record.giftType === 'FULL_GIFT') {
      return record.thresholdCents != null ? `满 ${displayCents(record.thresholdCents)} 元` : '--';
    }
    return record.thresholdQuantity != null ? `满 ${record.thresholdQuantity} 桶` : '--';
  };

  const renderScope = (record: GiftActivity) => {
    if (record.scope === 'ALL') return '全场';
    if (record.scope === 'PRODUCT') return '指定商品';
    return '指定分组';
  };

  const columns: ColumnsType<GiftActivity> = [
    { title: '活动名称', dataIndex: 'name', width: 180, ellipsis: true },
    {
      title: '类型',
      dataIndex: 'giftType',
      width: 90,
      render: (type: string) => <Tag color="blue">{GIFT_TYPE_LABELS[type] ?? type}</Tag>,
    },
    { title: '达标门槛', width: 130, render: (_, record) => <Text>{renderThreshold(record)}</Text> },
    {
      title: '赠品',
      width: 200,
      render: (_, record) => `${record.giftProductName ?? '商品#'.concat(String(record.giftProductId))} ×${record.giftQuantity}`,
    },
    { title: '适用范围', width: 110, render: (_, record) => renderScope(record) },
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
      width: 150,
      render: (_, record) => (
        <Space size="small">
          <Button size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          {record.status === 'ACTIVE' && (
            <Popconfirm
              title={`确定停用活动「${record.name}」吗?`}
              description="停用后新订单不再触发赠品"
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

  const activities = data?.giftActivities?.content ?? [];
  const total = data?.giftActivities?.totalElements ?? 0;

  return (
    <Card
      title="买赠/满赠"
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
      <Table<GiftActivity>
        rowKey="id"
        columns={columns}
        dataSource={activities}
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize: size,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
        }}
        onChange={handleTableChange}
        scroll={{ x: 1200 }}
      />
      <GiftActivityFormModal
        open={modalOpen}
        record={editingRecord}
        submitting={creating || updating}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </Card>
  );
};

export default BuyGift;
