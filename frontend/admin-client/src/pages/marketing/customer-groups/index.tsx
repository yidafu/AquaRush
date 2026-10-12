import React, { useState, useEffect, useCallback } from 'react';
import { Table, Space, Button, Card, Popconfirm } from '@/ui';
import { PlusOutlined } from '@/ui/icons';
import type { ColumnsType } from '@/ui';
import { formatDateTime } from '@aquarush/common';
import type { CustomerGroupVo } from '@aquarush/common';
import {
  useCustomerGroups,
  useCreateCustomerGroup,
  useUpdateCustomerGroup,
  useDeleteCustomerGroup,
} from '../../../services/marketing-graphql';
import GroupFormModal, { type GroupFormPayload } from './components/GroupFormModal';
import { pagination } from '@/utils/table';

// 客户分组管理页(F-MKT-001):列表/新建/编辑/删除(删除后成员回到未分组)
const CustomerGroups: React.FC = () => {
  const [loadGroups, { data, loading }] = useCustomerGroups();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<CustomerGroupVo | null>(null);

  const [createGroup, { loading: creating }] = useCreateCustomerGroup();
  const [updateGroup, { loading: updating }] = useUpdateCustomerGroup();
  const [deleteGroup, { loading: deleting }] = useDeleteCustomerGroup();

  const load = useCallback(
    (p = page, s = size) => {
      loadGroups({ variables: { input: { page: p, size: s } } });
    },
    [loadGroups, page, size],
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

  const handleEdit = (record: CustomerGroupVo) => {
    setEditingRecord(record);
    setModalOpen(true);
  };

  const handleSubmit = async (payload: GroupFormPayload, record: CustomerGroupVo | null) => {
    const { name, description, memberIds } = payload;
    // 成员覆盖式同步由后端完成(F-MKT-001:memberIds 划入/移出分组)
    if (record) {
      await updateGroup({
        variables: { id: record.id, input: { name, description, memberIds } },
      });
    } else {
      await createGroup({ variables: { input: { name, description, memberIds } } });
    }
    setModalOpen(false);
  };

  const handleDelete = async (record: CustomerGroupVo) => {
    await deleteGroup({ variables: { id: record.id } });
  };

  const columns: ColumnsType<CustomerGroupVo> = [
    { title: '分组名称', dataIndex: 'name', width: 200 },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
      render: (description: string | null | undefined) => description || '—',
    },
    { title: '成员数', dataIndex: 'memberCount', width: 100 },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: (createdAt: string) => formatDateTime(createdAt),
    },
    {
      title: '操作',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title={`确定删除分组「${record.name}」吗?`}
            description="删除后该分组下所有成员将回到未分组,已设置的分组价格将一并清除"
            okText="删除"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleDelete(record)}
          >
            <Button size="small" danger loading={deleting}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const groups = data?.customerGroups?.content ?? [];
  const total = data?.customerGroups?.totalElements ?? 0;

  return (
    <Card
      title="客户分组"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建分组
        </Button>
      }
    >
      <Table<CustomerGroupVo>
        rowKey="id"
        columns={columns}
        dataSource={groups}
        loading={loading}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: size,
          showSizeChanger: true,
        })}
        onChange={handleTableChange}
      />
      <GroupFormModal
        open={modalOpen}
        record={editingRecord}
        submitting={creating || updating}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </Card>
  );
};

export default CustomerGroups;
