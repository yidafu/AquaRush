import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Space, Button, Card, Popconfirm, Input, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import * as stylex from '@stylexjs/stylex';
import { displayCents, formatDateTime } from '@aquarush/common';
import type { FirstOrderActivityVo } from '@aquarush/common';
import {
  useFirstOrderActivities,
  useCreateFirstOrderActivity,
  useUpdateFirstOrderActivity,
  useDisableFirstOrderActivity,
} from '../../../services/marketing-graphql';
import FirstOrderFormModal, { type FirstOrderFormPayload } from './components/FirstOrderFormModal';
import { pagination } from '@/utils/table';

const { Text } = Typography;

const styles = stylex.create({
  search: {
    width: '220px',
  },
});

const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  FIRST_ORDER_REDUCE: '首单立减',
  NEWCOMER_PRICE: '新人专享价',
};

const DISPLAY_STATUS_META: Record<string, { text: string; color: string }> = {
  UPCOMING: { text: '未开始', color: 'default' },
  ONGOING: { text: '进行中', color: 'green' },
  ENDED: { text: '已结束', color: 'orange' },
  DISABLED: { text: '已停用', color: 'red' },
};

// 首单立减/新人专享价页(F-MKT-009):新用户首次下单自动享受,一单一享
const Newcomer: React.FC = () => {
  const navigate = useNavigate();
  const [loadActivities, { data, loading }] = useFirstOrderActivities();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<FirstOrderActivityVo | null>(null);

  const [createActivity, { loading: creating }] = useCreateFirstOrderActivity();
  const [updateActivity, { loading: updating }] = useUpdateFirstOrderActivity();
  const [disableActivity, { loading: disabling }] = useDisableFirstOrderActivity();

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

  const handleEdit = (record: FirstOrderActivityVo) => {
    setEditingRecord(record);
    setModalOpen(true);
  };

  const handleSubmit = async (payload: FirstOrderFormPayload, record: FirstOrderActivityVo | null) => {
    if (record) {
      await updateActivity({ variables: { id: record.id, input: payload } });
    } else {
      await createActivity({ variables: { input: payload } });
    }
    setModalOpen(false);
  };

  const handleDisable = async (record: FirstOrderActivityVo) => {
    await disableActivity({ variables: { id: record.id } });
  };

  const renderBenefit = (record: FirstOrderActivityVo) => {
    if (record.activityType === 'NEWCOMER_PRICE') {
      return record.newcomerPriceCents != null ? `新人价 ${displayCents(record.newcomerPriceCents)}` : '--';
    }
    if (record.discountAmountCents != null) {
      const threshold = record.thresholdCents != null ? `满${displayCents(record.thresholdCents)}减` : '减';
      return `${threshold}${displayCents(record.discountAmountCents)}`;
    }
    if (record.discountRatePermille != null) {
      return `满${displayCents(record.thresholdCents)}打${((record.discountRatePermille ?? 0) / 1000).toFixed(1)}折`;
    }
    return '--';
  };

  const renderScope = (record: FirstOrderActivityVo) => {
    if (record.activityType === 'NEWCOMER_PRICE') {
      return record.productName ?? '指定商品';
    }
    if (record.scope === 'ALL') return '全场';
    if (record.scope === 'PRODUCT') return '指定商品';
    return '指定分组';
  };

  const columns: ColumnsType<FirstOrderActivityVo> = [
    { title: '活动名称', dataIndex: 'name', width: 180, ellipsis: true },
    {
      title: '类型',
      dataIndex: 'activityType',
      width: 120,
      render: (type: string) => <Tag color="purple">{ACTIVITY_TYPE_LABELS[type] ?? type}</Tag>,
    },
    { title: '优惠内容', width: 200, render: (_, record) => <Text>{renderBenefit(record)}</Text> },
    { title: '适用范围', width: 130, render: (_, record) => renderScope(record) },
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
          <Button size="small" onClick={() => navigate(`/marketing/newcomer/${record.id}?name=${encodeURIComponent(record.name)}`)}>
            详情
          </Button>
          <Button size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          {record.status === 'ACTIVE' && (
            <Popconfirm
              title={`确定停用活动「${record.name}」吗?`}
              description="停用后新订单不再应用该优惠"
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

  const activities = data?.firstOrderActivities?.content ?? [];
  const total = data?.firstOrderActivities?.totalElements ?? 0;

  return (
    <Card
      title="首单立减/新人专享价"
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
            新建优惠
          </Button>
        </Space>
      }
    >
      <Table<FirstOrderActivityVo>
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
      <FirstOrderFormModal
        open={modalOpen}
        record={editingRecord}
        submitting={creating || updating}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </Card>
  );
};

export default Newcomer;
