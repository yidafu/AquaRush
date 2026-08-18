import React, { useState, useCallback } from 'react';
import { Card, Table, Tag, Button, Space, Modal, Form, Input, InputNumber, Select, Tabs, message, Typography } from 'antd';
import { useQuery, useMutation } from '@apollo/client';
import {
  BARREL_FEES_PAGE_QUERY,
  BARREL_PURCHASES_PAGE_QUERY,
  BARREL_FEES_PAGE,
  BARREL_PURCHASES_PAGE,
} from '../../graphql/queries/barrel.graphql';
import {
  CREATE_BARREL_FEE_MUTATION,
  UPDATE_BARREL_FEE_MUTATION,
  DISABLE_BARREL_FEE_MUTATION,
  REFUND_BARREL_PURCHASE_MUTATION,
} from '../../graphql/mutations/barrel.graphql';
import type { BarrelFee, BarrelPurchase, BarrelFeeStatus } from '../../types/barrel';
import type { ColumnsType } from 'antd/es/table';
import { displayCents, centsToYuan, yuanToCents } from '@aquarush/common';

const { Title } = Typography;

// 价目状态展示(G06)
const FEE_STATUS_MAP: Record<BarrelFeeStatus, { text: string; color: string }> = {
  ENABLED: { text: '启用', color: 'processing' },
  DISABLED: { text: '停用', color: 'default' },
};

// 购桶单状态展示(G06:一次性塑料水桶费,独立支付)
const PURCHASE_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待支付', color: 'warning' },
  PAID: { text: '已支付', color: 'processing' },
  CANCELLED: { text: '已取消', color: 'default' },
  REFUNDED: { text: '已退还', color: 'success' },
};

const BarrelManagementPage: React.FC = () => {
  const [feeStatusFilter, setFeeStatusFilter] = useState<string | undefined>(undefined);
  const [feePage, setFeePage] = useState(0);
  const [purchaseStatusFilter, setPurchaseStatusFilter] = useState<string | undefined>(undefined);
  const [purchasePage, setPurchasePage] = useState(0);

  const [feeModalOpen, setFeeModalOpen] = useState(false);
  const [editingFee, setEditingFee] = useState<BarrelFee | null>(null);
  const [feeForm] = Form.useForm();

  const [refundTarget, setRefundTarget] = useState<BarrelPurchase | null>(null);
  const [refundForm] = Form.useForm();

  // 单页单请求(规则 #10):价目与购桶记录各一次取回
  const feeQuery = useQuery<{ barrelFees: { content: BarrelFee[]; totalElements: number; totalPages: number } }>(
    BARREL_FEES_PAGE_QUERY,
    {
      variables: { status: feeStatusFilter || null, page: feePage, size: 10 },
      fetchPolicy: 'cache-and-network',
    },
  );
  const purchaseQuery = useQuery<{ barrelPurchases: { content: BarrelPurchase[]; totalElements: number; totalPages: number } }>(
    BARREL_PURCHASES_PAGE_QUERY,
    {
      variables: { status: purchaseStatusFilter || null, page: purchasePage, size: 10 },
      fetchPolicy: 'cache-and-network',
    },
  );

  const [createBarrelFee] = useMutation(CREATE_BARREL_FEE_MUTATION, {
    refetchQueries: [BARREL_FEES_PAGE],
    onCompleted: () => {
      message.success('价目已创建');
      setFeeModalOpen(false);
      feeForm.resetFields();
    },
    onError: (err) => message.error('创建失败: ' + err.message),
  });
  const [updateBarrelFee] = useMutation(UPDATE_BARREL_FEE_MUTATION, {
    refetchQueries: [BARREL_FEES_PAGE],
    onCompleted: () => {
      message.success('价目已更新');
      setFeeModalOpen(false);
      feeForm.resetFields();
    },
    onError: (err) => message.error('更新失败: ' + err.message),
  });
  const [disableBarrelFee] = useMutation(DISABLE_BARREL_FEE_MUTATION, {
    refetchQueries: [BARREL_FEES_PAGE],
    onCompleted: () => message.success('价目已停用'),
    onError: (err) => message.error('停用失败: ' + err.message),
  });
  const [refundBarrelPurchase, { loading: refunding }] = useMutation(REFUND_BARREL_PURCHASE_MUTATION, {
    refetchQueries: [BARREL_PURCHASES_PAGE],
    onCompleted: () => {
      message.success('水桶费已退还');
      setRefundTarget(null);
      refundForm.resetFields();
    },
    onError: (err) => message.error('退还失败: ' + err.message),
  });

  const openCreateFee = useCallback(() => {
    setEditingFee(null);
    feeForm.resetFields();
    setFeeModalOpen(true);
  }, [feeForm]);

  const openEditFee = useCallback(
    (fee: BarrelFee) => {
      setEditingFee(fee);
      feeForm.setFieldsValue({
        specification: fee.specification,
        price: centsToYuan(Number(fee.priceCents)),
        sortOrder: fee.sortOrder,
      });
      setFeeModalOpen(true);
    },
    [feeForm],
  );

  const handleFeeSubmit = useCallback(async () => {
    const values = await feeForm.validateFields();
    const payload = {
      specification: values.specification,
      priceCents: yuanToCents(values.price),
      sortOrder: values.sortOrder ?? 999,
    };
    if (editingFee) {
      updateBarrelFee({ variables: { id: editingFee.id, input: payload } });
    } else {
      createBarrelFee({ variables: { input: payload } });
    }
  }, [feeForm, editingFee, createBarrelFee, updateBarrelFee]);

  const handleDisableFee = useCallback(
    (fee: BarrelFee) => {
      Modal.confirm({
        title: '停用水桶费价目',
        content: `停用后用户端不可再购买「${fee.specification}」水桶,已生成的购桶单不受影响。确认停用?`,
        onOk: () => disableBarrelFee({ variables: { id: fee.id } }),
      });
    },
    [disableBarrelFee],
  );

  const handleRefundSubmit = useCallback(async () => {
    if (!refundTarget) return;
    const values = await refundForm.validateFields();
    refundBarrelPurchase({
      variables: {
        purchaseId: refundTarget.id,
        remark: values.refundRemark || null,
      },
    });
  }, [refundTarget, refundForm, refundBarrelPurchase]);

  const feeColumns: ColumnsType<BarrelFee> = [
    {
      title: '规格',
      dataIndex: 'specification',
      key: 'specification',
    },
    {
      title: '价格(元)',
      dataIndex: 'priceCents',
      key: 'priceCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: BarrelFeeStatus) => {
        const info = FEE_STATUS_MAP[status] || { text: status, color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 90,
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => openEditFee(record)}>
            编辑
          </Button>
          {record.status === 'ENABLED' && (
            <Button type="link" danger onClick={() => handleDisableFee(record)}>
              停用
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const purchaseColumns: ColumnsType<BarrelPurchase> = [
    {
      title: '购桶单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 170,
    },
    {
      title: '用户ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 130,
    },
    {
      title: '购买明细',
      key: 'items',
      render: (_, record) =>
        record.items.map((item) => `${item.specification} × ${item.quantity}`).join('、') || '-',
    },
    {
      title: '金额',
      dataIndex: 'totalAmountCents',
      key: 'totalAmountCents',
      width: 110,
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '状态',
      dataIndex: 'paymentStatus',
      key: 'paymentStatus',
      render: (status: string) => {
        const info = PURCHASE_STATUS_MAP[status] || { text: status, color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '支付时间',
      dataIndex: 'paidAt',
      key: 'paidAt',
      render: (t?: string) => (t ? new Date(t).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '退还时间/人',
      key: 'refunded',
      render: (_, record) =>
        record.refundedAt ? (
          <span style={{ fontSize: 12 }}>
            {new Date(record.refundedAt).toLocaleString('zh-CN')}
            <br />
            退还人: {record.refundedBy || '-'}
          </span>
        ) : (
          '-'
        ),
    },
    {
      title: '退还备注',
      dataIndex: 'refundRemark',
      key: 'refundRemark',
      ellipsis: true,
      render: (remark?: string) => remark || '-',
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 110,
      render: (_, record) =>
        record.paymentStatus === 'PAID' ? (
          <Button type="link" onClick={() => setRefundTarget(record)}>
            退还水桶费
          </Button>
        ) : null,
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3} style={{ marginBottom: 24 }}>
        水桶费管理
      </Title>
      <Card>
        <Tabs
          defaultActiveKey="fees"
          items={[
            {
              key: 'fees',
              label: '价目表',
              children: (
                <>
                  <Space style={{ marginBottom: 16 }}>
                    <Select
                      allowClear
                      placeholder="状态筛选"
                      style={{ width: 140 }}
                      value={feeStatusFilter}
                      onChange={(v) => {
                        setFeeStatusFilter(v);
                        setFeePage(0);
                      }}
                      options={Object.entries(FEE_STATUS_MAP).map(([value, info]) => ({ value, label: info.text }))}
                    />
                    <Button onClick={() => feeQuery.refetch()}>刷新</Button>
                    <Button type="primary" onClick={openCreateFee}>
                      新增价目
                    </Button>
                  </Space>
                  <Table
                    columns={feeColumns}
                    dataSource={feeQuery.data?.barrelFees.content}
                    loading={feeQuery.loading}
                    rowKey="id"
                    scroll={{ x: 800 }}
                    pagination={{
                      current: feePage + 1,
                      pageSize: 10,
                      total: feeQuery.data?.barrelFees.totalElements ?? 0,
                      onChange: (p) => setFeePage(p - 1),
                      showTotal: (total) => `共 ${total} 条`,
                    }}
                  />
                </>
              ),
            },
            {
              key: 'purchases',
              label: '购桶记录',
              children: (
                <>
                  <Space style={{ marginBottom: 16 }}>
                    <Select
                      allowClear
                      placeholder="状态筛选"
                      style={{ width: 140 }}
                      value={purchaseStatusFilter}
                      onChange={(v) => {
                        setPurchaseStatusFilter(v);
                        setPurchasePage(0);
                      }}
                      options={Object.entries(PURCHASE_STATUS_MAP).map(([value, info]) => ({ value, label: info.text }))}
                    />
                    <Button onClick={() => purchaseQuery.refetch()}>刷新</Button>
                  </Space>
                  <Table
                    columns={purchaseColumns}
                    dataSource={purchaseQuery.data?.barrelPurchases.content}
                    loading={purchaseQuery.loading}
                    rowKey="id"
                    scroll={{ x: 1100 }}
                    pagination={{
                      current: purchasePage + 1,
                      pageSize: 10,
                      total: purchaseQuery.data?.barrelPurchases.totalElements ?? 0,
                      onChange: (p) => setPurchasePage(p - 1),
                      showTotal: (total) => `共 ${total} 条`,
                    }}
                  />
                </>
              ),
            },
          ]}
        />
      </Card>

      {/* 价目新增/编辑弹窗(G06) */}
      <Modal
        title={editingFee ? '编辑水桶费价目' : '新增水桶费价目'}
        open={feeModalOpen}
        onCancel={() => {
          setFeeModalOpen(false);
          feeForm.resetFields();
        }}
        onOk={handleFeeSubmit}
        okText={editingFee ? '保存' : '创建'}
      >
        <Form form={feeForm} layout="vertical">
          <Form.Item
            name="specification"
            label="规格"
            rules={[{ required: true, message: '请输入规格(如 19L/桶)' }, { max: 100, message: '规格不能超过100个字符' }]}
          >
            <Input placeholder="如 19L/桶" />
          </Form.Item>
          <Form.Item
            name="price"
            label="价格(元)"
            rules={[{ required: true, message: '请输入价格' }]}
          >
            <InputNumber min={0} max={99999} precision={2} style={{ width: '100%' }} placeholder="一次性塑料水桶费用" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序权重(升序)">
            <InputNumber min={0} max={9999} style={{ width: '100%' }} placeholder="默认 999" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 退还水桶费弹窗(G06:管理员手动退还) */}
      <Modal
        title="退还水桶费"
        open={!!refundTarget}
        onCancel={() => {
          setRefundTarget(null);
          refundForm.resetFields();
        }}
        onOk={handleRefundSubmit}
        okText="确认退还"
        confirmLoading={refunding}
      >
        {refundTarget && (
          <div style={{ marginBottom: 12, color: '#666' }}>
            购桶单 {refundTarget.orderNo} · 金额 {displayCents(Number(refundTarget.totalAmountCents))}
            <br />
            备注:水桶费为一次性费用,退还仅适用于退桶/停止服务等特殊情况。
          </div>
        )}
        <Form form={refundForm}>
          <Form.Item name="refundRemark" label="退还备注">
            <Input.TextArea rows={3} maxLength={500} placeholder="请输入退还备注(如:空桶已退回)" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default BarrelManagementPage;
