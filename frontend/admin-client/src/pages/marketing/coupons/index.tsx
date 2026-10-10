import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Space, Button, Card, Popconfirm, Input } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import { displayCents, formatDateTime, COUPON_TYPE_LABELS } from '@aquarush/common';
import type { CouponTemplateAdminVo, CouponType } from '@aquarush/common';
import {
  useCouponTemplates,
  useCreateCouponTemplate,
  useUpdateCouponTemplate,
  useDisableCouponTemplate,
} from '../../../services/marketing-graphql';
import CouponFormModal, { type CouponFormPayload } from './components/CouponFormModal';
import IssueCouponModal from './components/IssueCouponModal';
import { pagination } from '@/utils/table';

// 优惠券管理页(F-MKT-003):列表/新建/编辑/停用/定向发放/详情(使用情况与相关订单)
const Coupons: React.FC = () => {
  const navigate = useNavigate();
  const [loadTemplates, { data, loading }] = useCouponTemplates();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<CouponTemplateAdminVo | null>(null);
  const [issueTarget, setIssueTarget] = useState<CouponTemplateAdminVo | null>(null);

  const [createTemplate, { loading: creating }] = useCreateCouponTemplate();
  const [updateTemplate, { loading: updating }] = useUpdateCouponTemplate();
  const [disableTemplate, { loading: disabling }] = useDisableCouponTemplate();

  const load = useCallback(
    (p = page, s = size, kw = keyword) => {
      loadTemplates({ variables: { input: { page: p, size: s, keyword: kw || undefined } } });
    },
    [loadTemplates, page, size, keyword],
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

  const handleEdit = (record: CouponTemplateAdminVo) => {
    setEditingRecord(record);
    setModalOpen(true);
  };

  const handleSubmit = async (payload: CouponFormPayload, record: CouponTemplateAdminVo | null) => {
    if (record) {
      await updateTemplate({ variables: { id: record.id, input: payload } });
    } else {
      await createTemplate({ variables: { input: payload } });
    }
    setModalOpen(false);
  };

  const handleDisable = async (record: CouponTemplateAdminVo) => {
    await disableTemplate({ variables: { id: record.id } });
  };

  const renderDiscount = (record: CouponTemplateAdminVo) => {
    if (record.couponType === 'DISCOUNT') {
      return record.discountRatePermille != null ? `${(record.discountRatePermille / 1000).toFixed(1)}折` : '—';
    }
    return displayCents(record.discountAmountCents);
  };

  const renderValidity = (record: CouponTemplateAdminVo) => {
    if (record.validityType === 'FIXED_RANGE') {
      return record.validFrom && record.validTo
        ? `${formatDateTime(record.validFrom)} ~ ${formatDateTime(record.validTo)}`
        : '—';
    }
    return `领取后${record.validDays ?? '—'}天内有效`;
  };

  const columns: ColumnsType<CouponTemplateAdminVo> = [
    { title: '券名称', dataIndex: 'name', width: 180, ellipsis: true },
    {
      title: '类型',
      dataIndex: 'couponType',
      width: 120,
      render: (type: string) => <Tag color="blue">{COUPON_TYPE_LABELS[type as CouponType] ?? type}</Tag>,
    },
    {
      title: '面额或折扣率',
      width: 110,
      render: (_, record) => renderDiscount(record),
    },
    {
      title: '门槛',
      width: 100,
      render: (_, record) =>
        record.couponType === 'NO_THRESHOLD_CASH' || !record.thresholdCents ? '无门槛' : displayCents(record.thresholdCents),
    },
    {
      title: '适用范围',
      width: 130,
      render: (_, record) => (record.scope === 'ALL' ? '全场' : '指定商品'),
    },
    { title: '有效期', width: 240, render: (_, record) => renderValidity(record) },
    {
      title: '发放方式',
      width: 100,
      render: (_, record) => (record.issuanceType === 'AUTO_CLAIM' ? '自动领取' : '定向发放'),
    },
    { title: '已领未用', dataIndex: 'receivedCount', width: 90 },
    { title: '已核销', dataIndex: 'usedCount', width: 80 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status: string) => (status === 'ACTIVE' ? <Tag color="green">上架</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      width: 260,
      render: (_, record) => (
        <Space size="small">
          <Button size="small" onClick={() => navigate(`/marketing/coupons/${record.id}?name=${encodeURIComponent(record.name)}`)}>
            详情
          </Button>
          <Button size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          {record.issuanceType === 'TARGETED' && record.status === 'ACTIVE' && (
            <Button size="small" type="primary" ghost onClick={() => setIssueTarget(record)}>
              定向发放
            </Button>
          )}
          {record.status === 'ACTIVE' && (
            <Popconfirm
              title={`确定停用优惠券「${record.name}」吗?`}
              description="已领取的优惠券按领取时的快照规则继续有效,但用户不可再领取"
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

  const templates = data?.couponTemplates?.content ?? [];
  const total = data?.couponTemplates?.totalElements ?? 0;

  return (
    <Card
      title="优惠券"
      extra={
        <Space>
          <Input.Search
            placeholder="搜索券名称"
            allowClear
            style={{ width: 220 }}
            onSearch={(value) => {
              setKeyword(value);
              load(0, size, value);
            }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建优惠券
          </Button>
        </Space>
      }
    >
      <Table<CouponTemplateAdminVo>
        rowKey="id"
        columns={columns}
        dataSource={templates}
        loading={loading}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: size,
          showSizeChanger: true,
        })}
        onChange={handleTableChange}
        scroll={{ x: 1500 }}
      />
      <CouponFormModal
        open={modalOpen}
        record={editingRecord}
        submitting={creating || updating}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
      <IssueCouponModal
        open={issueTarget !== null}
        couponTemplate={issueTarget}
        onCancel={() => setIssueTarget(null)}
      />
    </Card>
  );
};

export default Coupons;
