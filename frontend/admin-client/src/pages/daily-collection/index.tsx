import React, { useCallback, useState } from 'react';
import { Card, Space, Modal, Tabs, message, DatePicker, Tag } from '@aquarush/admin-ui';
import { useQuery, useMutation } from '@apollo/client';
import { DAILY_COLLECTION_PAGE_DATA_QUERY } from '../../graphql/queries/daily-collection.graphql';
import { CONFIRM_DAILY_COLLECTION, REJECT_DAILY_COLLECTION, REVIEW_RECONCILIATION, EXECUTE_DAILY_RECONCILIATION } from '../../graphql/mutations/daily-collection.graphql';
import type { DailyCollectionRecord, DailyReconciliation } from '../../types/daily-collection';
import dayjs from 'dayjs';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import {
  DailyCollectionStats,
  CollectionTable,
  ReconciliationTable,
  ReconciliationDetailModal,
  RejectCollectionModal,
} from './components';

const PAGE_SIZE = 10;

// .page-title(index.css 自研类)内联进组件;原 margin: 0 拆 longhand,
// 再叠加原内联 marginBottom 24px
const styles = stylex.create({
  pageTitle: {
    marginTop: '0px',
    marginRight: '0px',
    marginBottom: '24px',
    marginLeft: '0px',
    fontSize: tokens.fsPage,
    lineHeight: tokens.lhPage,
    fontWeight: 600,
  },
  titleTag: {
    marginLeft: '12px',
  },
  full: {
    width: '100%',
  },
  filterRow: {
    marginBottom: '16px',
  },
});

const DailyCollectionPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('collections')
  const [reconciliationModalVisible, setReconciliationModalVisible] = useState(false)
  const [selectedReconciliation, setSelectedReconciliation] = useState<DailyReconciliation | null>(null)
  // 驳回弹窗(G28 争议处理)
  const [rejectTarget, setRejectTarget] = useState<DailyCollectionRecord | null>(null)
  // 分页/筛选状态(服务端分页,规则 #13;页签切换各自独立)
  const [collectionsPage, setCollectionsPage] = useState(0)
  const [collectionsSize, setCollectionsSize] = useState(PAGE_SIZE)
  const [collectionDate, setCollectionDate] = useState<string | undefined>(undefined)
  const [reconciliationsPage, setReconciliationsPage] = useState(0)
  const [reconciliationsSize, setReconciliationsSize] = useState(PAGE_SIZE)

  // 单页单请求:每日收款记录(分页) + 对账记录(分页) + 汇总卡一次取回(规则 #10)
  const { data: pageData, loading: pageLoading, refetch } = useQuery<{
    dailyCollections: { list: DailyCollectionRecord[]; pageInfo: { total: number } }
    dailyReconciliations: { list: DailyReconciliation[]; pageInfo: { total: number } }
    dailyCollectionSummary: {
      collectionDate: string
      totalCashAmountCents: number | string
      totalQrCodeAmountCents: number | string
      totalWaterTicketCount: number
      totalOrderAmountCents: number | string
      totalSubmittedCount: number
    }
  }>(DAILY_COLLECTION_PAGE_DATA_QUERY, {
    variables: {
      collectionsInput: {
        collectionDate: collectionDate ?? null,
        page: collectionsPage,
        size: collectionsSize,
      },
      reconciliationsInput: {
        page: reconciliationsPage,
        size: reconciliationsSize,
      },
      summaryDate: collectionDate ?? dayjs().format('YYYY-MM-DD'),
    },
    fetchPolicy: 'cache-and-network',
  })

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

  // 汇总卡数据来自服务端聚合(指定日期,默认今日)—— 不再客户端 reduce 全量记录
  const summary = pageData?.dailyCollectionSummary
  const todayStats = {
    totalCash: Number(summary?.totalCashAmountCents ?? 0),
    totalQrCode: Number(summary?.totalQrCodeAmountCents ?? 0),
    totalWaterTicket: summary?.totalWaterTicketCount ?? 0,
    totalOrderAmount: Number(summary?.totalOrderAmountCents ?? 0),
  }

  const handleCollectionsPageChange = useCallback((page: number, size: number) => {
    setCollectionsPage(page - 1)
    setCollectionsSize(size)
  }, [])

  const handleReconciliationsPageChange = useCallback((page: number, size: number) => {
    setReconciliationsPage(page - 1)
    setReconciliationsSize(size)
  }, [])

  const collectionPageInfo = pageData?.dailyCollections.pageInfo
  const reconciliationPageInfo = pageData?.dailyReconciliations.pageInfo

  return (
    <div>
      <h2 {...stylex.props(styles.pageTitle)}>
        收银对账管理
        <Tag {...stylex.props(styles.titleTag)} color="blue">
          {summary ? `${summary.collectionDate} 已提交日报 ${summary.totalSubmittedCount} 人` : '汇总加载中'}
        </Tag>
      </h2>
      <Space direction="vertical" {...stylex.props(styles.full)} size="large">
        {/* 统计卡片 */}
        <DailyCollectionStats stats={todayStats} />

        {/* 收款记录和对账记录 Tab */}
        <Card>
          <Tabs activeKey={activeTab} onChange={setActiveTab}>
            <Tabs.TabPane tab="每日收款记录" key="collections">
              <Space {...stylex.props(styles.filterRow)}>
                <DatePicker
                  placeholder="按收款日期筛选"
                  allowClear
                  value={collectionDate ? dayjs(collectionDate) : null}
                  onChange={(date) => {
                    setCollectionDate(date ? date.format('YYYY-MM-DD') : undefined)
                    setCollectionsPage(0)
                  }}
                />
              </Space>
              <CollectionTable
                dataSource={pageData?.dailyCollections.list ?? []}
                loading={collectionLoading}
                onConfirm={handleConfirm}
                onReject={setRejectTarget}
                pagination={{
                  current: collectionsPage + 1,
                  pageSize: collectionsSize,
                  total: collectionPageInfo?.total ?? 0,
                  showSizeChanger: true,
                  onChange: handleCollectionsPageChange,
                }}
              />
            </Tabs.TabPane>
            <Tabs.TabPane tab="对账记录" key="reconciliations">
              <ReconciliationTable
                dataSource={pageData?.dailyReconciliations.list ?? []}
                loading={reconciliationLoading}
                executing={executeLoading}
                onExecute={handleExecuteReconciliation}
                onView={handleViewReconciliation}
                pagination={{
                  current: reconciliationsPage + 1,
                  pageSize: reconciliationsSize,
                  total: reconciliationPageInfo?.total ?? 0,
                  showSizeChanger: true,
                  onChange: handleReconciliationsPageChange,
                }}
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
