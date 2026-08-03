import React, { useState, useEffect } from 'react'
import { Card, Table, Tag, Button, Space, Modal, Form, Input, message, Row, Col, Statistic, Tabs } from 'antd'
import { useQuery, useMutation } from '@apollo/client'
import { GET_DAILY_COLLECTIONS, GET_DAILY_RECONCILIATIONS } from '../../graphql/queries/daily-collection.graphql'
import { CONFIRM_DAILY_COLLECTION, REVIEW_RECONCILIATION, EXECUTE_DAILY_RECONCILIATION } from '../../graphql/mutations/daily-collection.graphql'
import type { DailyCollectionRecord, DailyReconciliation } from '../../types/daily-collection'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { formatAdminTableAmount } from '../utils/money'

// 状态颜色映射
const statusColorMap: Record<string, string> = {
  DRAFT: 'default',
  SUBMITTED: 'processing',
  CONFIRMED: 'success',
  DISPUTED: 'error',
}

const statusTextMap: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  CONFIRMED: '已确认',
  DISPUTED: '有争议',
}

const reconciliationStatusColorMap: Record<string, string> = {
  PENDING: 'default',
  MATCHED: 'success',
  DISCREPANCY: 'warning',
  REVIEWED: 'processing',
}

const reconciliationStatusTextMap: Record<string, string> = {
  PENDING: '待对账',
  MATCHED: '匹配',
  DISCREPANCY: '有差异',
  REVIEWED: '已复核',
}

const DailyCollectionPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('collections')
  const [reconciliationModalVisible, setReconciliationModalVisible] = useState(false)
  const [selectedReconciliation, setSelectedReconciliation] = useState<DailyReconciliation | null>(null)
  const [reviewForm] = Form.useForm()

  // 查询收款记录（不传日期则获取所有记录）
  const { data: collectionData, loading: collectionLoading, refetch: refetchCollections } = useQuery<{ dailyCollections: DailyCollectionRecord[] }>(
    GET_DAILY_COLLECTIONS
  )

  // 查询对账记录
  const { data: reconciliationData, loading: reconciliationLoading, refetch: refetchReconciliations } = useQuery<{ dailyReconciliations: DailyReconciliation[] }>(
    GET_DAILY_RECONCILIATIONS
  )

  // 确认收款
  const [confirmCollection] = useMutation(CONFIRM_DAILY_COLLECTION, {
    onCompleted: () => {
      message.success('确认成功')
      refetchCollections()
    },
  })

  // 复核对账记录
  const [reviewReconciliation] = useMutation(REVIEW_RECONCILIATION, {
    onCompleted: () => {
      message.success('复核成功')
      setReconciliationModalVisible(false)
      reviewForm.resetFields()
      refetchReconciliations()
    },
  })

  // 手动执行对账
  const [executeReconciliation, { loading: executeLoading }] = useMutation(EXECUTE_DAILY_RECONCILIATION, {
    onCompleted: () => {
      message.success('对账执行成功')
      refetchReconciliations()
    },
    onError: (err) => {
      message.error('对账执行失败: ' + err.message)
    },
  })

  // 确认收款
  const handleConfirm = (record: DailyCollectionRecord) => {
    Modal.confirm({
      title: '确认收款',
      content: `确认配送员 ${record.deliveryWorkerId} 的收款记录吗？`,
      onOk: () => {
        confirmCollection({
          variables: {
            input: {
              recordId: record.id,
              notes: '',
            },
          },
        })
      },
    })
  }

  // 查看对账详情
  const handleViewReconciliation = (record: DailyReconciliation) => {
    setSelectedReconciliation(record)
    setReconciliationModalVisible(true)
  }

  // 提交复核
  const handleReviewSubmit = async () => {
    const values = await reviewForm.validateFields()
    if (selectedReconciliation) {
      reviewReconciliation({
        variables: {
          input: {
            reconciliationId: selectedReconciliation.id,
            reviewNotes: values.reviewNotes,
          },
        },
      })
    }
  }

  // 手动执行对账
  const handleExecuteReconciliation = () => {
    Modal.confirm({
      title: '手动执行对账',
      content: '确定要执行对账吗？重复执行会更新已有数据',
      onOk: () => {
        executeReconciliation({
          variables: {
            reconciliationDate: dayjs().subtract(1, 'day').format('YYYY-MM-DD'),
          },
        })
      },
    })
  }

  // 收款记录表格列
  const collectionColumns: ColumnsType<DailyCollectionRecord> = [
    {
      title: '配送员ID',
      dataIndex: 'deliveryWorkerId',
      key: 'deliveryWorkerId',
    },
    {
      title: '收款日期',
      dataIndex: 'collectionDate',
      key: 'collectionDate',
    },
    {
      title: '现金金额',
      dataIndex: 'cashAmountCents',
      key: 'cashAmountCents',
      render: (cents: number) => formatAdminTableAmount(cents),
    },
    {
      title: '水票数量',
      dataIndex: 'waterTicketCount',
      key: 'waterTicketCount',
    },
    {
      title: '扫码金额',
      dataIndex: 'qrCodeAmountCents',
      key: 'qrCodeAmountCents',
      render: (cents: number) => formatAdminTableAmount(cents),
    },
    {
      title: '订单数量',
      dataIndex: 'orderCount',
      key: 'orderCount',
    },
    {
      title: '订单金额',
      dataIndex: 'orderAmountCents',
      key: 'orderAmountCents',
      render: (cents: number) => formatAdminTableAmount(cents),
    },
    {
      title: '系统计算现金',
      dataIndex: 'calculatedCashAmountCents',
      key: 'calculatedCashAmountCents',
      render: (cents: number) => formatAdminTableAmount(cents),
    },
    {
      title: '系统计算扫码',
      dataIndex: 'calculatedQrCodeAmountCents',
      key: 'calculatedQrCodeAmountCents',
      render: (cents: number) => formatAdminTableAmount(cents),
    },
    {
      title: '系统计算水票',
      dataIndex: 'calculatedWaterTicketCount',
      key: 'calculatedWaterTicketCount',
    },
    {
      title: '系统计算订单金额',
      dataIndex: 'calculatedOrderAmountCents',
      key: 'calculatedOrderAmountCents',
      render: (cents: number) => formatAdminTableAmount(cents),
    },
    {
      title: '差异',
      dataIndex: 'differenceAmountCents',
      key: 'differenceAmountCents',
      render: (cents: number) => {
        const color = Math.abs(cents) <= 100 ? '#52c41a' : '#ff4d4f'
        return <span style={{ color }}>{formatAdminTableAmount(cents)}</span>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={statusColorMap[status]}>{statusTextMap[status] || status}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          {record.status === 'SUBMITTED' && (
            <Button type="link" onClick={() => handleConfirm(record)}>
              确认
            </Button>
          )}
        </Space>
      ),
    },
  ]

  // 对账记录表格列
  const reconciliationColumns: ColumnsType<DailyReconciliation> = [
    {
      title: '对账日期',
      dataIndex: 'reconciliationDate',
      key: 'reconciliationDate',
    },
    {
      title: '订单总数',
      dataIndex: 'totalOrderCount',
      key: 'totalOrderCount',
    },
    {
      title: '订单金额',
      dataIndex: 'totalOrderAmountCents',
      key: 'totalOrderAmountCents',
      render: (cents: number) => formatAdminTableAmount(cents),
    },
    {
      title: '总收款金额',
      dataIndex: 'totalCollectionAmountCents',
      key: 'totalCollectionAmountCents',
      render: (cents: number) => formatAdminTableAmount(cents),
    },
    {
      title: '现金收款',
      dataIndex: 'totalCashAmountCents',
      key: 'totalCashAmountCents',
      render: (cents: number) => formatAdminTableAmount(cents),
    },
    {
      title: '水票数量',
      dataIndex: 'totalWaterTicketCount',
      key: 'totalWaterTicketCount',
    },
    {
      title: '扫码收款',
      dataIndex: 'totalQrCodeAmountCents',
      key: 'totalQrCodeAmountCents',
      render: (cents: number) => formatAdminTableAmount(cents),
    },
    {
      title: '差异金额',
      dataIndex: 'discrepancyAmountCents',
      key: 'discrepancyAmountCents',
      render: (cents: number) => {
        const color = Math.abs(cents) <= 100 ? '#52c41a' : '#ff4d4f'
        return <span style={{ color }}>{formatAdminTableAmount(cents)}</span>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={reconciliationStatusColorMap[status]}>
          {reconciliationStatusTextMap[status] || status}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button type="link" onClick={() => handleViewReconciliation(record)}>
          查看详情
        </Button>
      ),
    },
  ]

  // 统计今日数据
  const getTodayStats = () => {
    const collections = collectionData?.dailyCollections || []
    return {
      totalCash: collections.reduce((sum, r) => sum + Number(r.cashAmountCents || 0), 0),
      totalQrCode: collections.reduce((sum, r) => sum + Number(r.qrCodeAmountCents || 0), 0),
      totalWaterTicket: collections.reduce((sum, r) => sum + r.waterTicketCount, 0),
      totalOrderAmount: collections.reduce((sum, r) => sum + Number(r.orderAmountCents || 0), 0),
    }
  }

  const todayStats = getTodayStats()

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 24 }}>收银对账管理</h2>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 统计卡片 */}
        <Row gutter={16}>
          <Col span={6}>
            <Card>
              <Statistic
                title="现金收款"
                value={todayStats.totalCash / 100}
                precision={2}
                prefix="¥"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="扫码收款"
                value={todayStats.totalQrCode / 100}
                precision={2}
                prefix="¥"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="水票数量"
                value={todayStats.totalWaterTicket}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="订单金额"
                value={todayStats.totalOrderAmount / 100}
                precision={2}
                prefix="¥"
              />
            </Card>
          </Col>
        </Row>

        {/* 收款记录和对账记录 Tab */}
        <Card>
          <Tabs activeKey={activeTab} onChange={setActiveTab}>
            <Tabs.TabPane tab="每日收款记录" key="collections">
              <Table
                columns={collectionColumns}
                dataSource={collectionData?.dailyCollections}
                loading={collectionLoading}
                rowKey="id"
                pagination={{ pageSize: 10 }}
              />
            </Tabs.TabPane>
            <Tabs.TabPane tab="对账记录" key="reconciliations">
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div className='flex justify-end'>
                <Button type="primary" onClick={handleExecuteReconciliation} loading={executeLoading}>
                  手动执行对账
                </Button>

                </div>
                <Table
                  columns={reconciliationColumns}
                  dataSource={reconciliationData?.dailyReconciliations}
                  loading={reconciliationLoading}
                  rowKey="id"
                  pagination={{ pageSize: 10 }}
                />
              </div>
            </Tabs.TabPane>
          </Tabs>
        </Card>
      </Space>

      {/* 对账详情弹窗 */}
      <Modal
        title="对账详情"
        open={reconciliationModalVisible}
        onCancel={() => setReconciliationModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setReconciliationModalVisible(false)}>
            关闭
          </Button>,
          selectedReconciliation?.status !== 'REVIEWED' && (
            <Button key="review" type="primary" onClick={handleReviewSubmit}>
              确认复核
            </Button>
          ),
        ]}
      >
        {selectedReconciliation && (
          <div>
            <Row gutter={16}>
              <Col span={12}>
                <Statistic title="订单总数" value={selectedReconciliation.totalOrderCount} />
              </Col>
              <Col span={12}>
                <Statistic
                  title="订单金额"
                  value={selectedReconciliation.totalOrderAmountCents / 100}
                  precision={2}
                  prefix="¥"
                />
              </Col>
            </Row>
            <Row gutter={16} style={{ marginTop: 16 }}>
              <Col span={12}>
                <Statistic
                  title="现金收款"
                  value={selectedReconciliation.totalCashAmountCents / 100}
                  precision={2}
                  prefix="¥"
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="扫码收款"
                  value={selectedReconciliation.totalQrCodeAmountCents / 100}
                  precision={2}
                  prefix="¥"
                />
              </Col>
            </Row>
            <Row gutter={16} style={{ marginTop: 16 }}>
              <Col span={12}>
                <Statistic
                  title="差异金额"
                  value={selectedReconciliation.discrepancyAmountCents / 100}
                  precision={2}
                  prefix="¥"
                  valueStyle={{ color: Math.abs(selectedReconciliation.discrepancyAmountCents) <= 100 ? '#52c41a' : '#ff4d4f' }}
                />
              </Col>
              <Col span={12}>
                <Tag color={reconciliationStatusColorMap[selectedReconciliation.status]}>
                  {reconciliationStatusTextMap[selectedReconciliation.status]}
                </Tag>
              </Col>
            </Row>
            {selectedReconciliation.status !== 'REVIEWED' && (
              <div style={{ marginTop: 16 }}>
                <Form form={reviewForm}>
                  <Form.Item name="reviewNotes" label="复核备注">
                    <Input.TextArea rows={3} placeholder="请输入复核备注" />
                  </Form.Item>
                </Form>
              </div>
            )}
            {selectedReconciliation.reviewedAt && (
              <div style={{ marginTop: 16, color: '#666' }}>
                复核时间: {selectedReconciliation.reviewedAt}
                <br />
                复核备注: {selectedReconciliation.reviewNotes}
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  )
}

export default DailyCollectionPage
