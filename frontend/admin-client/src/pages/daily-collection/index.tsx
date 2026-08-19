import React, { useState } from 'react';
import { Card, Space, Modal, Tabs, message } from 'antd';
import { useQuery, useMutation } from '@apollo/client';
import { DAILY_COLLECTION_PAGE_DATA_QUERY } from '../../graphql/queries/daily-collection.graphql';
import { CONFIRM_DAILY_COLLECTION, REJECT_DAILY_COLLECTION, REVIEW_RECONCILIATION, EXECUTE_DAILY_RECONCILIATION } from '../../graphql/mutations/daily-collection.graphql';
import type { DailyCollectionRecord, DailyReconciliation } from '../../types/daily-collection';
import dayjs from 'dayjs';
import {
  DailyCollectionStats,
  CollectionTable,
  ReconciliationTable,
  ReconciliationDetailModal,
  RejectCollectionModal,
} from './components';

const DailyCollectionPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('collections')
  const [reconciliationModalVisible, setReconciliationModalVisible] = useState(false)
  const [selectedReconciliation, setSelectedReconciliation] = useState<DailyReconciliation | null>(null)
  // 驳回弹窗(G28 争议处理)
  const [rejectTarget, setRejectTarget] = useState<DailyCollectionRecord | null>(null)

  // 单页单请求:每日收款记录 + 对账记录一次取回(规则 #10)
  const { data: pageData, loading: pageLoading, refetch } = useQuery<{
    dailyCollections: DailyCollectionRecord[]
    dailyReconciliations: DailyReconciliation[]
  }>(DAILY_COLLECTION_PAGE_DATA_QUERY)

  const collectionData = pageData
  const reconciliationData = pageData
  const collectionLoading = pageLoading
  const reconciliationLoading = pageLoading

  // 确认收款
  const [confirmCollection] = useMutation(CONFIRM_DAILY_COLLECTION, {
    onCompleted: () => {
      message.success('确认成功')
      refetch()
    },
  })

  // 驳回收款(G28:驳回退回草稿,配送员可改可重提)
  const [rejectCollection] = useMutation(REJECT_DAILY_COLLECTION, {
    onCompleted: () => {
      message.success('已驳回')
      setRejectTarget(null)
      refetch()
    },
    onError: (err) => {
      message.error('驳回失败: ' + err.message)
    },
  })

  // 复核对账记录
  const [reviewReconciliation] = useMutation(REVIEW_RECONCILIATION, {
    onCompleted: () => {
      message.success('复核成功')
      setReconciliationModalVisible(false)
      refetch()
    },
  })

  // 手动执行对账
  const [executeReconciliation, { loading: executeLoading }] = useMutation(EXECUTE_DAILY_RECONCILIATION, {
    onCompleted: () => {
      message.success('对账执行成功')
      refetch()
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

  // 驳回(G28:必填驳回原因,表单校验在弹窗内完成)
  const handleRejectSubmit = (reason: string) => {
    if (!rejectTarget) return
    rejectCollection({
      variables: {
        input: {
          recordId: rejectTarget.id,
          reason,
        },
      },
    })
  }

  // 查看对账详情
  const handleViewReconciliation = (record: DailyReconciliation) => {
    setSelectedReconciliation(record)
    setReconciliationModalVisible(true)
  }

  // 提交复核(备注表单校验在弹窗内完成)
  const handleReviewSubmit = (reviewNotes: string) => {
    if (selectedReconciliation) {
      reviewReconciliation({
        variables: {
          input: {
            reconciliationId: selectedReconciliation.id,
            reviewNotes,
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

  // 统计今日数据
  const todayStats = {
    totalCash: (collectionData?.dailyCollections || []).reduce((sum, r) => sum + Number(r.cashAmountCents || 0), 0),
    totalQrCode: (collectionData?.dailyCollections || []).reduce((sum, r) => sum + Number(r.qrCodeAmountCents || 0), 0),
    totalWaterTicket: (collectionData?.dailyCollections || []).reduce((sum, r) => sum + r.waterTicketCount, 0),
    totalOrderAmount: (collectionData?.dailyCollections || []).reduce((sum, r) => sum + Number(r.orderAmountCents || 0), 0),
  }

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 24 }}>收银对账管理</h2>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {/* 统计卡片 */}
        <DailyCollectionStats stats={todayStats} />

        {/* 收款记录和对账记录 Tab */}
        <Card>
          <Tabs activeKey={activeTab} onChange={setActiveTab}>
            <Tabs.TabPane tab="每日收款记录" key="collections">
              <CollectionTable
                dataSource={collectionData?.dailyCollections ?? []}
                loading={collectionLoading}
                onConfirm={handleConfirm}
                onReject={setRejectTarget}
              />
            </Tabs.TabPane>
            <Tabs.TabPane tab="对账记录" key="reconciliations">
              <ReconciliationTable
                dataSource={reconciliationData?.dailyReconciliations ?? []}
                loading={reconciliationLoading}
                executing={executeLoading}
                onExecute={handleExecuteReconciliation}
                onView={handleViewReconciliation}
              />
            </Tabs.TabPane>
          </Tabs>
        </Card>
      </Space>

      {/* 对账详情弹窗 */}
      <ReconciliationDetailModal
        open={reconciliationModalVisible}
        record={selectedReconciliation}
        onClose={() => setReconciliationModalVisible(false)}
        onReview={handleReviewSubmit}
      />

      {/* 驳回弹窗(G28:争议日报驳回,必填原因) */}
      <RejectCollectionModal
        target={rejectTarget}
        onCancel={() => setRejectTarget(null)}
        onSubmit={handleRejectSubmit}
      />
    </div>
  )
}

export default DailyCollectionPage
