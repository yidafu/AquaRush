import React, { useMemo, useState } from 'react';
import { Alert, Card, Col, DatePicker, Empty, Row, Statistic, Table, Tag } from 'antd';
import { useQuery } from '@apollo/client';
import { GET_DAILY_COLLECTIONS } from '../../../../../graphql/queries/daily-collection.graphql';
import type { DailyCollectionRecord, DailyCollectionStatus } from '../../../../../types/daily-collection';
import { displayCents } from '@aquarush/common';
import dayjs from 'dayjs';
import { pagination } from '@/utils/table';

interface DailyCollectionSectionProps {
  /** 配送身份 id(送水员档案并入 admins 单表后=账号 id;F-USR-021/023) */
  deliveryWorkerId: string;
}

const STATUS_MAP: Record<DailyCollectionStatus, { color: string; text: string }> = {
  DRAFT: { color: 'default', text: '草稿' },
  SUBMITTED: { color: 'processing', text: '已提交' },
  CONFIRMED: { color: 'success', text: '已确认' },
  DISPUTED: { color: 'error', text: '差异' },
};

/**
 * 账号详情页「每日收款」Tab(R-COL-04):
 * 展示该送水员/兼送水管理员的收款日报(dailyCollections 按 deliveryWorkerId 过滤),
 * 含现金/扫码/水票/订单合计与差异。
 */
const DailyCollectionSection: React.FC<DailyCollectionSectionProps> = ({ deliveryWorkerId }) => {
  const [collectionDate, setCollectionDate] = useState<string | undefined>(undefined);

  const { data, loading, error } = useQuery(GET_DAILY_COLLECTIONS, {
    variables: collectionDate ? { collectionDate } : undefined,
    errorPolicy: 'all',
  });

  const records: DailyCollectionRecord[] = useMemo(
    () =>
      ((data?.dailyCollections ?? []) as DailyCollectionRecord[]).filter(
        (r) => String(r.deliveryWorkerId) === String(deliveryWorkerId),
      ),
    [data, deliveryWorkerId],
  );

  // 合计(现金/扫码/水票/订单数/订单金额/差异)
  const totals = useMemo(() => {
    const sum = (key: 'cashAmountCents' | 'qrCodeAmountCents' | 'orderAmountCents' | 'differenceAmountCents') =>
      records.reduce((acc, r) => acc + Number(r[key] ?? 0), 0);
    return {
      cashAmountCents: sum('cashAmountCents'),
      qrCodeAmountCents: sum('qrCodeAmountCents'),
      orderAmountCents: sum('orderAmountCents'),
      differenceAmountCents: sum('differenceAmountCents'),
      waterTicketCount: records.reduce((acc, r) => acc + Number(r.waterTicketCount ?? 0), 0),
      orderCount: records.reduce((acc, r) => acc + Number(r.orderCount ?? 0), 0),
    };
  }, [records]);

  const columns = [
    {
      title: '收款日期',
      dataIndex: 'collectionDate',
      key: 'collectionDate',
      width: 130,
    },
    {
      title: '现金收款',
      dataIndex: 'cashAmountCents',
      key: 'cashAmountCents',
      width: 120,
      render: (v: number | string) => displayCents(Number(v)),
    },
    {
      title: '扫码收款',
      dataIndex: 'qrCodeAmountCents',
      key: 'qrCodeAmountCents',
      width: 120,
      render: (v: number | string) => displayCents(Number(v)),
    },
    {
      title: '水票数量',
      dataIndex: 'waterTicketCount',
      key: 'waterTicketCount',
      width: 100,
    },
    {
      title: '订单数量',
      dataIndex: 'orderCount',
      key: 'orderCount',
      width: 100,
    },
    {
      title: '订单金额',
      dataIndex: 'orderAmountCents',
      key: 'orderAmountCents',
      width: 120,
      render: (v: number | string) => displayCents(Number(v)),
    },
    {
      title: '差异金额',
      dataIndex: 'differenceAmountCents',
      key: 'differenceAmountCents',
      width: 120,
      render: (v: number | string) => {
        const diff = Number(v ?? 0);
        return (
          <span style={{ color: diff === 0 ? undefined : diff > 0 ? '#f5222d' : '#52c41a' }}>
            {displayCents(diff)}
          </span>
        );
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: DailyCollectionStatus) => {
        const config = STATUS_MAP[status];
        return <Tag color={config?.color ?? 'default'}>{config?.text ?? status}</Tag>;
      },
    },
    {
      title: '确认时间',
      dataIndex: 'confirmedAt',
      key: 'confirmedAt',
      width: 150,
      render: (v?: string) => (v ? new Date(v).toLocaleString('zh-CN') : '—'),
    },
  ];

  if (error) {
    return <Alert type="error" showIcon message="每日收款加载失败" description={error.message} />;
  }

  return (
    <Card title="每日收款">
      <Row gutter={16} style={{ marginBottom: 16 }} align="middle">
        <Col>
          <DatePicker
            placeholder="按收款日期筛选"
            allowClear
            value={collectionDate ? dayjs(collectionDate) : null}
            onChange={(date) => setCollectionDate(date ? date.format('YYYY-MM-DD') : undefined)}
          />
        </Col>
        <Col>
          <Statistic title="现金收款合计" value={totals.cashAmountCents} formatter={(v) => displayCents(Number(v))} />
        </Col>
        <Col>
          <Statistic title="扫码收款合计" value={totals.qrCodeAmountCents} formatter={(v) => displayCents(Number(v))} />
        </Col>
        <Col>
          <Statistic title="水票合计" value={totals.waterTicketCount} suffix="张" />
        </Col>
        <Col>
          <Statistic title="订单合计" value={totals.orderCount} suffix="单" />
        </Col>
        <Col>
          <Statistic title="差异合计" value={totals.differenceAmountCents} formatter={(v) => displayCents(Number(v))} />
        </Col>
      </Row>

      <Table
        columns={columns}
        dataSource={records}
        rowKey="id"
        loading={loading}
        pagination={pagination(records.length, { pageSize: 10, showTotal: 'range' })}
        scroll={{ x: 1000 }}
        locale={{ emptyText: <Empty description="该账号暂无收款日报" /> }}
      />
    </Card>
  );
};

export default DailyCollectionSection;
