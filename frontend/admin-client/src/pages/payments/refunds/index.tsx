import React, { useState, useCallback } from 'react';
import { Card, Table, Tag, Button, Space, Modal, Form, Input, Select, message } from 'antd';
import { useQuery, useMutation } from '@apollo/client';
import { REFUND_REQUESTS_PAGE_QUERY, REFUND_REQUESTS_PAGE } from '../../../graphql/queries/refund.graphql';
import { PROCESS_REFUND_MUTATION } from '../../../graphql/mutations/refund.graphql';
import type { RefundRequest } from '../../../types/refund';
import type { ColumnsType } from 'antd/es/table';
import { displayCents } from '@aquarush/common';
import { pagination } from '@/utils/table';


// 退款状态展示(R-PAY-02:待审核/已通过/已拒绝/已完成/失败)
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待审核', color: 'processing' },
  APPROVED: { text: '已通过', color: 'blue' },
  REJECTED: { text: '已拒绝', color: 'red' },
  COMPLETED: { text: '已完成', color: 'success' },
  FAILED: { text: '失败', color: 'error' },
};

const RefundManagementPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [reviewTarget, setReviewTarget] = useState<RefundRequest | null>(null);
  const [reviewAction, setReviewAction] = useState<'APPROVE' | 'REJECT'>('APPROVE');
  const [reviewForm] = Form.useForm();

  // 单页单请求:退款申请分页一次取回(规则 #10)
  const { data, loading, refetch } = useQuery<{ refundRequests: { content: RefundRequest[]; totalElements: number; totalPages: number } }>(
    REFUND_REQUESTS_PAGE_QUERY,
    {
      variables: { status: statusFilter || null, page, size: 10 },
      fetchPolicy: 'cache-and-network',
    },
  );

  const [processRefund, { loading: processing }] = useMutation(PROCESS_REFUND_MUTATION, {
    refetchQueries: [REFUND_REQUESTS_PAGE],
    onCompleted: () => {
      message.success(reviewAction === 'APPROVE' ? '退款审核通过' : '已驳回退款');
      setReviewTarget(null);
      reviewForm.resetFields();
    },
    onError: (err) => {
      message.error('审核失败: ' + err.message);
    },
  });

  const handleReviewSubmit = useCallback(async () => {
    if (!reviewTarget) return;
    const values = await reviewForm.validateFields();
    processRefund({
      variables: {
        input: {
          refundRequestId: reviewTarget.id,
          originalTransactionId: reviewTarget.originalTransactionId,
          orderId: reviewTarget.orderId,
          userId: reviewTarget.userId,
          amountCents: Number(reviewTarget.amountCents),
          reason: reviewTarget.reason,
          action: reviewAction,
          adminNote: values.adminNote || null,
        },
      },
    });
  }, [reviewTarget, reviewAction, reviewForm, processRefund]);

  const openReview = (record: RefundRequest, action: 'APPROVE' | 'REJECT') => {
    setReviewTarget(record);
    setReviewAction(action);
    reviewForm.resetFields();
  };

  const columns: ColumnsType<RefundRequest> = [
    {
      title: '申请时间',
      dataIndex: 'requestedAt',
      key: 'requestedAt',
      render: (t: string) => new Date(t).toLocaleString('zh-CN'),
      width: 160,
    },
    {
      title: '订单ID',
      dataIndex: 'orderId',
      key: 'orderId',
      width: 150,
    },
    {
      title: '用户ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 130,
    },
    {
      title: '退款金额',
      dataIndex: 'amountCents',
      key: 'amountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '退款原因',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const info = STATUS_MAP[status] || { text: status, color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '处理时间/人',
      key: 'processed',
      render: (_, record) =>
        record.processedAt ? (
          <span style={{ fontSize: 12 }}>
            {new Date(record.processedAt).toLocaleString('zh-CN')}
            <br />
            处理人: {record.processedBy || '-'}
          </span>
        ) : (
          '-'
        ),
    },
    {
      title: '审核备注',
      dataIndex: 'adminNote',
      key: 'adminNote',
      ellipsis: true,
      render: (note?: string) => note || '-',
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 130,
      render: (_, record) => (
        <Space>
          {(record.status === 'PENDING' || record.status === 'FAILED') && (
            <Button type="link" onClick={() => openReview(record, 'APPROVE')}>
              {record.status === 'FAILED' ? '重试' : '通过'}
            </Button>
          )}
          {record.status === 'PENDING' && (
            <Button type="link" danger onClick={() => openReview(record, 'REJECT')}>
              驳回
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h1 className="page-title" style={{ marginBottom: 24 }}>
        退款管理
      </h1>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Select
            allowClear
            placeholder="状态筛选"
            style={{ width: 160 }}
            value={statusFilter}
            onChange={(v) => {
              setStatusFilter(v);
              setPage(0);
            }}
            options={Object.entries(STATUS_MAP).map(([value, info]) => ({ value, label: info.text }))}
          />
          <Button onClick={() => refetch()}>刷新</Button>
        </Space>
        <Table
          columns={columns}
          dataSource={data?.refundRequests.content}
          loading={loading}
          rowKey="id"
          scroll={{ x: 1100 }}
          pagination={pagination(data?.refundRequests.totalElements ?? 0, {
            current: page + 1,
            pageSize: 10,
            onChange: (p) => setPage(p - 1),
          })}
        />
      </Card>

      {/* 审核弹窗(通过/驳回) */}
      <Modal
        title={reviewAction === 'APPROVE' ? (reviewTarget?.status === 'FAILED' ? '重试退款' : '审核通过退款') : '驳回退款'}
        open={!!reviewTarget}
        onCancel={() => {
          setReviewTarget(null);
          reviewForm.resetFields();
        }}
        onOk={handleReviewSubmit}
        okText={reviewAction === 'APPROVE' ? '确认通过' : '确认驳回'}
        okButtonProps={{ danger: reviewAction === 'REJECT' }}
        confirmLoading={processing}
      >
        {reviewTarget && (
          <div style={{ marginBottom: 12, color: '#666' }}>
            订单 {reviewTarget.orderId} · 金额 {displayCents(Number(reviewTarget.amountCents))}
            <br />
            原因: {reviewTarget.reason}
          </div>
        )}
        <Form form={reviewForm}>
          <Form.Item name="adminNote" label="审核备注">
            <Input.TextArea rows={3} maxLength={1000} placeholder={reviewAction === 'REJECT' ? '请输入驳回原因(留痕)' : '请输入备注(可选)'} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default RefundManagementPage;
