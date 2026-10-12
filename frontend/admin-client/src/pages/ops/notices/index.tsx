import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Space, Button, Card, Popconfirm, Switch } from '@/ui';
import { PlusOutlined } from '@/ui/icons';
import type { ColumnsType } from '@/ui';
import { formatDateTime } from '@aquarush/common';
import type { NoticeVo, NoticeStatus } from '@aquarush/common';
import {
  useNotices,
  useCreateNotice,
  useUpdateNotice,
  usePublishNotice,
  useStopNotice,
  useSetNoticePinned,
  useDeleteNotice,
} from '../../../services/ops-graphql';
import NoticeFormModal, { type NoticeFormPayload } from './components/NoticeFormModal';
import { pagination } from '@/utils/table';
import {
  NOTICE_TYPE_LABELS,
  NOTICE_TYPE_COLORS,
  NOTICE_STATUS_LABELS,
  NOTICE_STATUS_COLORS,
} from './constants';

// 公告管理页(F-OPS-002):列表/新建/编辑/发布/置顶/停用/删除
const Notices: React.FC = () => {
  const [loadNotices, { data, loading }] = useNotices();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<NoticeVo | null>(null);

  const [createNotice, { loading: creating }] = useCreateNotice();
  const [updateNotice, { loading: updating }] = useUpdateNotice();
  const [publishNotice, { loading: publishing }] = usePublishNotice();
  const [stopNotice, { loading: stopping }] = useStopNotice();
  const [setNoticePinned, { loading: pinning }] = useSetNoticePinned();
  const [deleteNotice, { loading: deleting }] = useDeleteNotice();

  const load = useCallback(
    (p = page, s = size) => {
      loadNotices({ variables: { input: { page: p, size: s } } });
    },
    [loadNotices, page, size],
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

  const handleEdit = (record: NoticeVo) => {
    setEditingRecord(record);
    setModalOpen(true);
  };

  const handleSubmit = async (payload: NoticeFormPayload, record: NoticeVo | null) => {
    if (record) {
      await updateNotice({ variables: { id: record.id, input: payload } });
    } else {
      await createNotice({ variables: { input: payload } });
    }
    setModalOpen(false);
  };

  const handlePublish = async (record: NoticeVo) => {
    await publishNotice({ variables: { id: record.id } });
  };

  const handleStop = async (record: NoticeVo) => {
    await stopNotice({ variables: { id: record.id } });
  };

  const handleTogglePinned = async (record: NoticeVo, pinned: boolean) => {
    await setNoticePinned({ variables: { id: record.id, pinned } });
  };

  const handleDelete = async (record: NoticeVo) => {
    await deleteNotice({ variables: { id: record.id } });
  };

  const columns: ColumnsType<NoticeVo> = [
    { title: '标题', dataIndex: 'title', width: 220, ellipsis: true },
    {
      title: '类型',
      dataIndex: 'type',
      width: 100,
      render: (type: string) => <Tag color={NOTICE_TYPE_COLORS[type]}>{NOTICE_TYPE_LABELS[type]}</Tag>,
    },
    {
      title: '置顶',
      dataIndex: 'pinned',
      width: 80,
      render: (pinned: boolean) => (pinned ? <Tag color="gold">置顶</Tag> : '—'),
    },
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
      dataIndex: 'status',
      width: 90,
      render: (status: NoticeStatus) => <Tag color={NOTICE_STATUS_COLORS[status]}>{NOTICE_STATUS_LABELS[status]}</Tag>,
    },
    {
      title: '发布时间',
      dataIndex: 'publishedAt',
      width: 170,
      render: (publishedAt: string | null | undefined) => (publishedAt ? formatDateTime(publishedAt) : '—'),
    },
    {
      title: '操作',
      width: 300,
      render: (_, record) => (
        <Space size="small">
          {record.status !== 'STOPPED' && (
            <Button size="small" onClick={() => handleEdit(record)}>
              编辑
            </Button>
          )}
          {record.status !== 'PUBLISHED' && record.status !== 'STOPPED' && (
            <Button size="small" type="primary" ghost onClick={() => handlePublish(record)}>
              发布
            </Button>
          )}
          {record.status === 'PUBLISHED' && (
            <>
              <Switch
                size="small"
                checked={record.pinned}
                checkedChildren="置顶"
                unCheckedChildren="取消"
                loading={pinning}
                onChange={(checked) => handleTogglePinned(record, checked)}
              />
              <Popconfirm title="确定停用该公告?停用后用户端不再展示" onConfirm={() => handleStop(record)}>
                <Button size="small" danger loading={stopping}>
                  停用
                </Button>
              </Popconfirm>
            </>
          )}
          <Popconfirm title="确定删除该公告?" onConfirm={() => handleDelete(record)}>
            <Button size="small" danger loading={deleting}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const notices = data?.notices?.content ?? [];
  const total = data?.notices?.totalElements ?? 0;

  return (
    <Card
      title="公告"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建公告
        </Button>
      }
    >
      <Table<NoticeVo>
        rowKey="id"
        columns={columns}
        dataSource={notices}
        loading={loading || publishing}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: size,
          showSizeChanger: true,
        })}
        onChange={handleTableChange}
      />
      <NoticeFormModal
        open={modalOpen}
        record={editingRecord}
        submitting={creating || updating}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </Card>
  );
};

export default Notices;
