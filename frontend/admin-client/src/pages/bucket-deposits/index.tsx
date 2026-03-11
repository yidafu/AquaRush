import React, { useState, useEffect } from 'react';
import { Table, Tag, Button, Space, Card, Modal, Form, InputNumber, message, Select, Row, Col, Input, DatePicker } from 'antd';
import { useQuery, useMutation } from '@apollo/client';
import { GET_BUCKET_DEPOSITS_QUERY, GET_BUCKET_DEPOSIT_AMOUNT_QUERY } from '../../graphql/queries/bucket-deposit.graphql';
import { REFUND_BUCKET_DEPOSIT_MUTATION, SET_BUCKET_DEPOSIT_AMOUNT_MUTATION } from '../../graphql/mutations/bucket-deposit.graphql';
import { formatAdminTableAmount } from '../utils/money';

const { Option } = Select;
const { RangePicker } = DatePicker;

// 押桶状态映射
const depositStatusMap: Record<string, { text: string; color: string }> = {
  DEPOSITED: { text: '押桶中', color: 'blue' },
  REFUNDED: { text: '已退还', color: 'green' },
};

const BucketDepositsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [refundModalVisible, setRefundModalVisible] = useState(false);
  const [settingsModalVisible, setSettingsModalVisible] = useState(false);
  const [selectedDeposit, setSelectedDeposit] = useState<any>(null);
  const [refundForm] = Form.useForm();
  const [settingsForm] = Form.useForm();
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20 });

  // 查询押桶记录
  const { data: depositsData, loading: depositsLoading, refetch: refetchDeposits } = useQuery(
    GET_BUCKET_DEPOSITS_QUERY,
    {
      variables: {
        status: statusFilter,
        page: pagination.current - 1,
        size: pagination.pageSize,
      },
      errorPolicy: 'all',
    }
  );

  // 查询押桶金额配置
  const { data: amountData, refetch: refetchAmount } = useQuery(
    GET_BUCKET_DEPOSIT_AMOUNT_QUERY,
    {
      errorPolicy: 'all',
    }
  );

  // 退还押金 mutation
  const [refundBucketDeposit, { loading: refundLoading }] = useMutation(REFUND_BUCKET_DEPOSIT_MUTATION);

  // 设置押桶金额 mutation
  const [setBucketDepositAmount, { loading: setAmountLoading }] = useMutation(SET_BUCKET_DEPOSIT_AMOUNT_MUTATION);

  const deposits = depositsData?.allBucketDeposits?.list || [];
  const pageInfo = depositsData?.allBucketDeposits?.pageInfo || { total: 0, pageNum: 0, pageSize: 20, hasNext: false, hasPrevious: false, totalPages: 0 };
  const depositAmount = amountData?.bucketDepositAmount || 0;

  // 表格列定义
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '用户ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 100,
    },
    {
      title: '押桶数量',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 100,
      render: (quantity: number) => `${quantity}个`,
    },
    {
      title: '押金金额',
      dataIndex: 'amountCents',
      key: 'amountCents',
      width: 120,
      render: (_: any, record: any) => formatAdminTableAmount(record.amountCents * record.quantity),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const statusInfo = depositStatusMap[status] || { text: status, color: 'default' };
        return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>;
      },
    },
    {
      title: '支付交易号',
      dataIndex: 'paymentTransactionId',
      key: 'paymentTransactionId',
      width: 180,
      ellipsis: true,
    },
    {
      title: '押桶时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
    },
    {
      title: '退还时间',
      dataIndex: 'refundedAt',
      key: 'refundedAt',
      width: 180,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      width: 150,
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: any) => (
        <Space>
          {record.status === 'DEPOSITED' && (
            <Button
              type="link"
              onClick={() => {
                setSelectedDeposit(record);
                setRefundModalVisible(true);
              }}
            >
              退还
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const handleRefund = async () => {
    try {
      const values = await refundForm.validateFields();
      await refundBucketDeposit({
        variables: {
          depositId: parseInt(selectedDeposit.id),
          remark: values.remark,
        },
      });
      message.success('押金退还成功');
      setRefundModalVisible(false);
      refundForm.resetFields();
      refetchDeposits();
    } catch (error: any) {
      message.error(error.message || '押金退还失败');
    }
  };

  const handleSetAmount = async () => {
    try {
      const values = await settingsForm.validateFields();
      await setBucketDepositAmount({
        variables: {
          amountCents: values.amountCents,
        },
      });
      message.success('押桶金额设置成功');
      setSettingsModalVisible(false);
      settingsForm.resetFields();
      refetchAmount();
    } catch (error: any) {
      message.error(error.message || '押桶金额设置失败');
    }
  };

  const handleTableChange = (newPagination: any) => {
    setPagination(newPagination);
    refetchDeposits({
      page: newPagination.current - 1,
      size: newPagination.pageSize,
    });
  };

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={12}>
          <Card>
            <Statistic
              title="当前押桶金额"
              value={formatAdminTableAmount(depositAmount)}
              suffix="元/个"
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            <Button type="primary" onClick={() => {
              settingsForm.setFieldsValue({ amountCents: depositAmount / 100 });
              setSettingsModalVisible(true);
            }}>
            设置押桶金额
            </Button>
          </Card>
        </Col>
      </Row>

      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Select
            placeholder="筛选状态"
            allowClear
            style={{ width: 150 }}
            value={statusFilter}
            onChange={(value) => {
              setStatusFilter(value);
              refetchDeposits({ status: value, page: 0 });
            }}
          >
            <Option value="DEPOSITED">押桶中</Option>
            <Option value="REFUNDED">已退还</Option>
          </Select>
          <Button onClick={() => refetchDeposits()}>刷新</Button>
        </Space>

        <Table
          columns={columns}
          dataSource={deposits}
          loading={depositsLoading}
          rowKey="id"
          pagination={{
            ...pagination,
            total: pageInfo.total,
            showSizeChanger: true,
            showQuickJumper: true,
          }}
          onChange={handleTableChange}
        />
      </Card>

      {/* 退还押金弹窗 */}
      <Modal
        title="退还押金"
        open={refundModalVisible}
        onCancel={() => {
          setRefundModalVisible(false);
          refundForm.resetFields();
        }}
        onOk={handleRefund}
        confirmLoading={refundLoading}
      >
        <Form form={refundForm} layout="vertical">
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} placeholder="请输入退还备注（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 设置押桶金额弹窗 */}
      <Modal
        title="设置押桶金额"
        open={settingsModalVisible}
        onCancel={() => {
          setSettingsModalVisible(false);
          settingsForm.resetFields();
        }}
        onOk={handleSetAmount}
        confirmLoading={setAmountLoading}
      >
        <Form form={settingsForm} layout="vertical">
          <Form.Item
            label="每个桶的押金（元）"
            name="amountCents"
            rules={[{ required: true, message: '请输入押桶金额' }]}
          >
            <InputNumber
              min={0}
              max={99999}
              step={1}
              style={{ width: '100%' }}
              placeholder="请输入每个桶的押金金额（元）"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default BucketDepositsPage;
