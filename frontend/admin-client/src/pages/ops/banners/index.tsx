import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Space, Button, Card, Image, Popconfirm, Switch } from 'antd';
import { PlusOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { formatDateTime } from '@aquarush/common';
import type { BannerVo } from '@aquarush/common';
import {
  useBanners,
  useCreateBanner,
  useUpdateBanner,
  useDeleteBanner,
  useReorderBanner,
} from '../../../services/ops-graphql';
import BannerFormModal, { type BannerFormPayload } from './components/BannerFormModal';
import { pagination } from '@/utils/table';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  previewImg: {
    objectFit: 'cover',
    borderRadius: '6px',
  },
});

// 轮播图管理页(F-OPS-001):列表/新建/编辑/删除/上移下移/启用停用
const Banners: React.FC = () => {
  const [loadBanners, { data, loading }] = useBanners();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<BannerVo | null>(null);

  const [createBanner, { loading: creating }] = useCreateBanner();
  const [updateBanner, { loading: updating }] = useUpdateBanner();
  const [deleteBanner] = useDeleteBanner();
  const [reorderBanner] = useReorderBanner();

  const load = useCallback(
    (p = page, s = size) => {
      loadBanners({ variables: { input: { page: p, size: s } } });
    },
    [loadBanners, page, size],
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

  const handleEdit = (record: BannerVo) => {
    setEditingRecord(record);
    setModalOpen(true);
  };

  const handleSubmit = async (payload: BannerFormPayload, record: BannerVo | null) => {
    if (record) {
      await updateBanner({
        variables: { id: record.id, input: { ...payload, enabled: payload.enabled } },
      });
    } else {
      await createBanner({ variables: { input: { ...payload, enabled: payload.enabled } } });
    }
    setModalOpen(false);
  };

  const handleDelete = async (record: BannerVo) => {
    await deleteBanner({ variables: { id: record.id } });
  };

  const handleReorder = async (record: BannerVo, direction: 'UP' | 'DOWN') => {
    await reorderBanner({ variables: { id: record.id, direction } });
  };

  const handleToggleEnabled = async (record: BannerVo, enabled: boolean) => {
    await updateBanner({
      variables: {
        id: record.id,
        input: {
          imageUrl: record.imageUrl,
          title: record.title,
          subtitle: record.subtitle,
          linkType: record.linkType,
          linkTarget: record.linkTarget,
          sortOrder: record.sortOrder,
          enabled,
          startAt: record.startAt,
          endAt: record.endAt,
        },
      },
    });
  };

  const linkLabel = (type: string) => (type === 'PRODUCT' ? '商品详情' : '自定义页面');

  const columns: ColumnsType<BannerVo> = [
    {
      title: '图片',
      dataIndex: 'imageUrl',
      width: 140,
      render: (url: string) => (
        <Image src={url} width={120} height={56} {...stylex.props(styles.previewImg)} />
      ),
    },
    { title: '标题', dataIndex: 'title', width: 160 },
    {
      title: '跳转目标',
      width: 200,
      render: (_, record) => (
        <Tag color={record.linkType === 'PRODUCT' ? 'blue' : 'default'}>{linkLabel(record.linkType)}</Tag>
      ),
    },
    { title: '排序', dataIndex: 'sortOrder', width: 70 },
    {
      title: '时间窗',
      width: 220,
      render: (_, record) => {
        if (!record.startAt && !record.endAt) return <span>长期</span>;
        return (
          <span>
            {record.startAt ? formatDateTime(record.startAt) : '—'} ~ {record.endAt ? formatDateTime(record.endAt) : '—'}
          </span>
        );
      },
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 90,
      render: (enabled: boolean) => (enabled ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      width: 260,
      render: (_, record) => (
        <Space size="small">
          <Button size="small" type="text" icon={<ArrowUpOutlined />} onClick={() => handleReorder(record, 'UP')} />
          <Button size="small" type="text" icon={<ArrowDownOutlined />} onClick={() => handleReorder(record, 'DOWN')} />
          <Switch size="small" checked={record.enabled} onChange={(checked) => handleToggleEnabled(record, checked)} />
          <Button size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除该轮播图?" onConfirm={() => handleDelete(record)}>
            <Button size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const banners = data?.banners?.content ?? [];
  const total = data?.banners?.totalElements ?? 0;

  return (
    <Card
      title="首页轮播"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建轮播图
        </Button>
      }
    >
      <Table<BannerVo>
        rowKey="id"
        columns={columns}
        dataSource={banners}
        loading={loading}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: size,
          showSizeChanger: true,
        })}
        onChange={handleTableChange}
      />
      <BannerFormModal
        open={modalOpen}
        record={editingRecord}
        submitting={creating || updating}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </Card>
  );
};

export default Banners;
