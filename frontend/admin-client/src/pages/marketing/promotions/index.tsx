import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Space, Button, Card, Popconfirm, Input, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import * as stylex from '@stylexjs/stylex';
import { displayCents, formatDateTime } from '@aquarush/common';
import type { PromotionActivityVo } from '@aquarush/common';
import {
  usePromotionActivities,
  useCreatePromotionActivity,
  useUpdatePromotionActivity,
  useDisablePromotionActivity,
} from '../../../services/marketing-graphql';
import PromotionFormModal, { type PromotionFormPayload } from './components/PromotionFormModal';
import { pagination } from '@/utils/table';

const { Text } = Typography;

const styles = stylex.create({
  search: {
    width: '220px',
  },
});

const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  FULL_REDUCE: '满减',
  FULL_DISCOUNT: '满折',
};

const DISPLAY_STATUS_META: Record<string, { text: string; color: string }> = {
  UPCOMING: { text: '未开始', color: 'default' },
  ONGOING: { text: '进行中', color: 'green' },
  ENDED: { text: '已结束', color: 'orange' },
  DISABLED: { text: '已停用', color: 'red' },
};

// 满减/折扣活动页(F-MKT-004):列表/新建/编辑/停用/详情(使用情况与相关订单)
const Promotions: React.FC = () => {
  const navigate = useNavigate();
  const [loadActivities, { data, loading }] = usePromotionActivities();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<PromotionActivityVo | null>(null);

  const [createActivity, { loading: creating }] = useCreatePromotionActivity();
  const [updateActivity, { loading: updating }] = useUpdatePromotionActivity();
  const [disableActivity, { loading: disabling }] = useDisablePromotionActivity();

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

  const handleEdit = (record: PromotionActivityVo) => {
    setEditingRecord(record);
    setModalOpen(true);
  };

  const handleSubmit = async (payload: PromotionFormPayload, record: PromotionActivityVo | null) => {
    if (record) {
      await updateActivity({ variables: { id: record.id, input: payload } });
    } else {
      await createActivity({ variables: { input: payload } });
    }
    setModalOpen(false);
  };

  const handleDisable = async (record: PromotionActivityVo) => {
    await disableActivity({ variables: { id: record.id } });
  };

  const renderTiers = (record: PromotionActivityVo) => {
    if (record.activityType === 'FULL_REDUCE') {
      return record.tiers.map((tier) => `满${displayCents(tier.thresholdCents)}减${displayCents(tier.discountAmountCents)}`).join(' / ');
    }
    return record.tiers
      .map((tier) => `满${displayCents(tier.thresholdCents)}打${((tier.discountRatePermille ?? 0) / 1000).toFixed(1)}折`)
      .join(' / ');
  };

  const renderScope = (record: PromotionActivityVo) => {
    if (record.scope === 'ALL') return '全场';
    if (record.scope === 'PRODUCT') return '指定商品';
    return '指定分组';
  };

  const columns: ColumnsType<PromotionActivityVo> = [
    { title: '活动名称', dataIndex: 'name', width: 180, ellipsis: true },
    {
      title: '类型',
      dataIndex: 'activityType',
      width: 90,
      render: (type: string) => <Tag color="blue">{ACTIVITY_TYPE_LABELS[type] ?? type}</Tag>,
    },
    { title: '档位', width: 280, render: (_, record) => <Text>{renderTiers(record)}</Text> },
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
      width: 200,
      render: (_, record) => (
        <Space size="small">
          <Button size="small" onClick={() => navigate(`/marketing/promotions/${record.id}?name=${encodeURIComponent(record.name)}`)}>
            详情
          </Button>
          <Button size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          {record.status === 'ACTIVE' && (
            <Popconfirm
              title={`确定停用活动「${record.name}」吗?`}
              description="停用后活动立即失效,不再参与订单计价"
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

  const activities = data?.promotionActivities?.content ?? [];
  const total = data?.promotionActivities?.totalElements ?? 0;

  return (
    <Card
      title="满减/折扣活动"
      extra={
        <Space>
          <Input.Search
            placeholder="搜索活动名称"
            allowClear
            {...stylex.props(styles.search)}
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
      <Table<PromotionActivityVo>
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
        scroll={{ x: 1200 }}
      />
      <PromotionFormModal
        open={modalOpen}
        record={editingRecord}
        submitting={creating || updating}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </Card>
  );
};

export default Promotions;
