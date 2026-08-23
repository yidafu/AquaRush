import React, { useState } from 'react';
import { Card, Select, Table, Tag, Empty, Alert } from 'antd';
import { useQuery } from '@apollo/client';
import { USER_TICKETS_QUERY } from '../../../../graphql/queries/user.graphql';
import type { UserTicketVo, UserTicketStatus } from '@aquarush/common';
import {
  TICKET_STATUS_LABELS,
  TICKET_STATUS_TONE,
  UserTicketStatus as UserTicketStatusEnum,
  formatDateTime,
} from '@aquarush/common';
import { getToneColor } from '../../../../constants/status';

interface TicketSectionProps {
  userId: string;
}

const STATUS_OPTIONS = [
  { value: 'ALL', label: '全部' },
  { value: UserTicketStatusEnum.UNUSED, label: '未用' },
  { value: UserTicketStatusEnum.USED, label: '已核销' },
  { value: UserTicketStatusEnum.RETURNED, label: '已退回' },
  { value: UserTicketStatusEnum.EXPIRED, label: '已过期' },
];

/**
 * 用户详情页「水票」Tab(F-MKT-007):展示该用户持有的桶水票,支持状态筛选。
 */
const TicketSection: React.FC<TicketSectionProps> = ({ userId }) => {
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const status: UserTicketStatus | undefined =
    statusFilter === 'ALL' ? undefined : (statusFilter as UserTicketStatus);

  const { data, loading, error } = useQuery(USER_TICKETS_QUERY, {
    variables: { userId, status },
    skip: !userId,
    errorPolicy: 'all',
  });


  const tickets: UserTicketVo[] = data?.userTickets || [];

  if (error) {
    return <Alert type="error" showIcon message="水票加载失败" description={error.message} />;
  }

  const columns = [
    {
      title: '票号',
      dataIndex: 'ticketNo',
      key: 'ticketNo',
      width: 200,
      render: (v: string) => (v ? <span style={{ fontFamily: 'monospace' }}>{v}</span> : '—'),
    },
    {
      title: '票种名称',
      dataIndex: 'ticketTypeName',
      key: 'ticketTypeName',
      width: 220,
    },
    {
      title: '限定规格',
      dataIndex: 'specification',
      key: 'specification',
      width: 120,
      render: (v: string | null | undefined) => v || '—',
    },
    {
      title: '票类型',
      key: 'isGift',
      width: 100,
      render: (_: unknown, record: UserTicketVo) =>
        record.isGift ? <Tag color="orange">赠送票</Tag> : <Tag>购买票</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: UserTicketStatus) => (
        <Tag color={getToneColor(TICKET_STATUS_TONE[s])}>{TICKET_STATUS_LABELS[s] ?? s}</Tag>
      ),
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      key: 'expireTime',
      width: 160,
      render: (t: string) => formatDateTime(t),
    },
  ];

  return (
    <Card
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span>桶水票</span>
          <Select
            value={statusFilter}
            onChange={setStatusFilter}
            options={STATUS_OPTIONS}
            style={{ width: 130 }}
            size="small"
          />
        </div>
      }
    >
      <Table
        columns={columns}
        dataSource={tickets}
        rowKey="id"
        loading={loading}
        pagination={false}
        scroll={{ x: 800 }}
        locale={{ emptyText: <Empty description="该用户暂无桶水票" /> }}
      />
    </Card>
  );
};

export default TicketSection;
