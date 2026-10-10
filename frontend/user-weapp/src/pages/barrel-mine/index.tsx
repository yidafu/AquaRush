import React, { useState, useCallback, useMemo } from 'react'
import { View, Text, Input, Textarea, Picker } from '@tarojs/components'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import BarrelService from '../../services/BarrelService'
import { authService } from '../../utils/auth'
import {
  BarrelAccountSpecVo,
  DepositRecordVo,
  DepositRefundApplicationVo,
  DepositRefundApplicationStatus,
  DepositStatus,
  StatusTone,
  displayCents,
  normalizeCents,
  formatDateTime
} from '@aquarush/common'
import BasePageLayout from '../../components/BasePageLayout'
import StatusTag from '../../components/StatusTag'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

import './index.scss'

const PAGE_SIZE = 10
const APPLICATION_SIZE = 20

/** 退押申请状态(F-PRO-010:待处理 → 已回收 → 已退还 / 已驳回) */
const REFUND_STATUS_MAP: Record<DepositRefundApplicationStatus, { text: string; tone: StatusTone }> = {
  PENDING: { text: '待处理', tone: 'warning' },
  RECOVERED: { text: '已回收', tone: 'info' },
  REFUNDED: { text: '已退还', tone: 'success' },
  REJECTED: { text: '已驳回', tone: 'error' }
}

/** 押金记录状态(R-MON-03:收取 → 在租 → 退还) */
const DEPOSIT_STATUS_MAP: Record<DepositStatus, { text: string; tone: StatusTone }> = {
  PENDING: { text: '待收取', tone: 'warning' },
  HELD: { text: '在租', tone: 'info' },
  REFUNDED: { text: '已退还', tone: 'success' }
}

/** 退押申请表单(选规格、桶数 ≤ 在租、回收地址三栏、备注) */
interface RefundForm {
  specification: string
  bucketCount: string
  receiverName: string
  receiverPhone: string
  receiverDetail: string
  remark: string
}

const emptyForm: RefundForm = {
  specification: '',
  bucketCount: '1',
  receiverName: '',
  receiverPhone: '',
  receiverDetail: '',
  remark: ''
}

/**
 * 水桶账户(F-PRO-010 水桶账户与退押)
 * 顶部押金合计/欠桶合计 → 按规格在租/欠桶列表 → 押金记录分页 → 退押申请入口与申请记录。
 * 文案统一「水桶账户/押金/退押」(R-MON-03 押金桶,不再是一次性买桶语义)。
 */
const BarrelMinePage: React.FC = () => {
  usePageViewTracking()

  // Long 标量 TS 类型为 number,运行时 GraphQL 实际传输字符串 —— 展示层统一 normalizeCents
  const [depositHeldCents, setDepositHeldCents] = useState<number | string>(0)
  const [owedBucketCount, setOwedBucketCount] = useState<number>(0)
  const [specs, setSpecs] = useState<BarrelAccountSpecVo[]>([])

  const [records, setRecords] = useState<DepositRecordVo[]>([])
  const [recordTotal, setRecordTotal] = useState<number>(0)
  const [recordPage, setRecordPage] = useState<number>(0)
  const [recordHasMore, setRecordHasMore] = useState<boolean>(false)
  const [loadingMore, setLoadingMore] = useState<boolean>(false)

  const [applications, setApplications] = useState<DepositRefundApplicationVo[]>([])

  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  const [formVisible, setFormVisible] = useState<boolean>(false)
  const [form, setForm] = useState<RefundForm>(emptyForm)
  const [submitting, setSubmitting] = useState<boolean>(false)

  const service = BarrelService.getInstance()

  /** 可退押规格(仅在租桶数 > 0 可选) */
  const refundableSpecs = useMemo(() => specs.filter((s) => s.heldBucketCount > 0), [specs])

  const loadAccount = useCallback(
    async (page: number = 0, append: boolean = false) => {
      try {
        if (page === 0) {
          setLoading(true)
          setError(null)
        } else {
          setLoadingMore(true)
        }

        const result = await service.getBarrelAccount(page, PAGE_SIZE)
        if (!result.success || !result.data) {
          setError(result.error?.message || '获取水桶账户失败')
          return
        }

        const data = result.data
        setDepositHeldCents(data.depositHeldCents)
        setOwedBucketCount(data.owedBucketCount)
        setSpecs([...(data.specs ?? [])])
        setRecords((prev) => (append ? [...prev, ...(data.records ?? [])] : [...(data.records ?? [])]))
        const fetched = data.records ?? []
        setRecordTotal(data.recordTotal ?? 0)
        setRecordHasMore((append ? records.length + fetched.length : fetched.length) < (data.recordTotal ?? 0))
        setRecordPage(page)
      } catch (e) {
        console.error('加载水桶账户失败:', e)
        if (!append) setError('加载失败，请重试')
      } finally {
        setLoading(false)
        setLoadingMore(false)
      }
    },
    [service, records.length]
  )

  const loadApplications = useCallback(async () => {
    const result = await service.getDepositRefundApplications(0, APPLICATION_SIZE)
    if (result.success && result.data) {
      setApplications([...(result.data.content ?? [])])
    } else {
      console.error('加载退押申请记录失败:', result.error?.message)
    }
  }, [service])

  const reload = useCallback(async () => {
    await Promise.all([loadAccount(0, false), loadApplications()])
  }, [loadAccount, loadApplications])

  useDidShow(() => {
    if (authService.isAuthenticated()) {
      reload()
    } else {
      setLoading(false)
    }
  })

  useReachBottom(() => {
    if (recordHasMore && !loadingMore && !loading) {
      loadAccount(recordPage + 1, true)
    }
  })

  usePullDownRefresh(async () => {
    await reload()
    Taro.stopPullDownRefresh()
  })

  const updateForm = useCallback((field: keyof RefundForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }))
  }, [])

  const openForm = useCallback(() => {
    if (refundableSpecs.length === 0) {
      Taro.showToast({ title: '当前没有可退押的在租水桶', icon: 'none' })
      return
    }
    setForm({ ...emptyForm, specification: refundableSpecs[0].specification })
    setFormVisible(true)
  }, [refundableSpecs])

  const closeForm = useCallback(() => {
    setFormVisible(false)
    setForm(emptyForm)
  }, [])

  const selectedSpec = useMemo(
    () => refundableSpecs.find((s) => s.specification === form.specification) ?? null,
    [refundableSpecs, form.specification]
  )

  const specPickerIndex = useMemo(() => {
    const idx = refundableSpecs.findIndex((s) => s.specification === form.specification)
    return idx >= 0 ? idx : 0
  }, [refundableSpecs, form.specification])

  const validateForm = useCallback((): boolean => {
    if (!form.specification) {
      Taro.showToast({ title: '请选择退押规格', icon: 'none' })
      return false
    }
    const count = Number(form.bucketCount)
    const max = selectedSpec?.heldBucketCount ?? 0
    if (!Number.isInteger(count) || count <= 0) {
      Taro.showToast({ title: '请输入正确的桶数', icon: 'none' })
      return false
    }
    if (count > max) {
      Taro.showToast({ title: `桶数不能超过在租 ${max} 桶`, icon: 'none' })
      return false
    }
    if (!form.receiverName.trim()) {
      Taro.showToast({ title: '请输入回收联系人', icon: 'none' })
      return false
    }
    if (!/^1\d{10}$/.test(form.receiverPhone.trim())) {
      Taro.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return false
    }
    if (!form.receiverDetail.trim()) {
      Taro.showToast({ title: '请输入回收地址', icon: 'none' })
      return false
    }
    return true
  }, [form, selectedSpec])

  const handleSubmit = useCallback(async () => {
    if (!validateForm() || submitting) return

    setSubmitting(true)
    try {
      const result = await service.submitDepositRefund({
        specification: form.specification,
        bucketCount: Number(form.bucketCount),
        receiverName: form.receiverName.trim(),
        receiverPhone: form.receiverPhone.trim(),
        receiverDetail: form.receiverDetail.trim(),
        remark: form.remark.trim() || undefined
      })
      if (result.success) {
        Taro.showToast({ title: '退押申请已提交', icon: 'success' })
        closeForm()
        await reload()
      } else {
        Taro.showToast({ title: result.error?.message || '提交失败，请重试', icon: 'none' })
      }
    } catch (e) {
      console.error('提交退押申请失败:', e)
      Taro.showToast({ title: '提交失败，请重试', icon: 'none' })
    } finally {
      setSubmitting(false)
    }
  }, [validateForm, submitting, service, form, closeForm, reload])

  const money = (cents: number | string | null | undefined): string =>
    displayCents(normalizeCents(cents) ?? 0)

  return (
    <BasePageLayout
      requireAuth={true}
      loading={loading}
      error={error}
      onRetry={reload}
      empty={specs.length === 0 && !loading && !error}
      emptyTitle='暂无水桶账户'
      emptySubtitle='使用循环水桶配送后,押金与桶数会记录在这里'
      className='barrel-mine-page'
    >
      {/* 押金合计 / 欠桶合计 */}
      <View className='barrel-mine-summary'>
        <View className='barrel-mine-summary-item'>
          <Text className='barrel-mine-summary-value'>{money(depositHeldCents)}</Text>
          <Text className='barrel-mine-summary-label'>押金合计(在租)</Text>
        </View>
        <View className='barrel-mine-summary-divider' />
        <View className='barrel-mine-summary-item'>
          <Text className='barrel-mine-summary-value'>{owedBucketCount}</Text>
          <Text className='barrel-mine-summary-label'>欠桶合计(个)</Text>
        </View>
      </View>

      {/* 按规格桶账 */}
      <View className='barrel-mine-card'>
        <View className='barrel-mine-card-header'>
          <Text className='barrel-mine-card-title'>按规格桶账</Text>
          <View
            trackTarget='barrel-open-refund'
            trackElementType='button'
            trackElementText='申请退押'
            className='barrel-mine-refund-btn'
            onClick={openForm}
          >
            <Text className='barrel-mine-refund-btn-text'>申请退押</Text>
          </View>
        </View>
        {specs.map((spec) => (
          <View key={spec.specification} className='barrel-mine-spec'>
            <View className='barrel-mine-spec-main'>
              <Text className='barrel-mine-spec-name'>{spec.specification}</Text>
              <Text className='barrel-mine-spec-deposit'>{money(spec.depositHeldCents)}</Text>
            </View>
            <View className='barrel-mine-spec-sub'>
              <Text className='barrel-mine-spec-meta'>在租 {spec.heldBucketCount} 桶</Text>
              <Text className='barrel-mine-spec-meta'>欠桶 {spec.owedBucketCount} 个</Text>
            </View>
          </View>
        ))}
        {specs.length === 0 && (
          <Text className='barrel-mine-spec-empty'>暂无在租水桶</Text>
        )}
      </View>

      {/* 退押申请表单 */}
      {formVisible && (
        <View className='barrel-mine-card'>
          <View className='barrel-mine-card-header'>
            <Text className='barrel-mine-card-title'>申请退押</Text>
            <Text className='barrel-mine-form-close' onClick={closeForm}>
              收起
            </Text>
          </View>

          <View className='barrel-mine-form-item'>
            <Text className='barrel-mine-form-label'>水桶规格</Text>
            <Picker
              mode='selector'
              range={refundableSpecs.map((s) => s.specification)}
              value={specPickerIndex}
              onChange={(e) => updateForm('specification', refundableSpecs[Number(e.detail.value)]?.specification ?? '')}
            >
              <View className='barrel-mine-form-picker'>
                <Text>{form.specification || '请选择规格'}</Text>
              </View>
            </Picker>
            {selectedSpec && (
              <Text className='barrel-mine-form-help'>可退 {selectedSpec.heldBucketCount} 桶(在租)</Text>
            )}
          </View>

          <View className='barrel-mine-form-item'>
            <Text className='barrel-mine-form-label'>退押桶数</Text>
            <Input
              className='barrel-mine-form-input'
              type='number'
              value={form.bucketCount}
              onInput={(e) => updateForm('bucketCount', e.detail.value)}
              placeholder='请输入退回的桶数'
            />
          </View>

          <View className='barrel-mine-form-item'>
            <Text className='barrel-mine-form-label'>回收联系人</Text>
            <Input
              className='barrel-mine-form-input'
              value={form.receiverName}
              onInput={(e) => updateForm('receiverName', e.detail.value)}
              placeholder='请输入联系人姓名'
            />
          </View>

          <View className='barrel-mine-form-item'>
            <Text className='barrel-mine-form-label'>联系电话</Text>
            <Input
              className='barrel-mine-form-input'
              type='number'
              maxlength={11}
              value={form.receiverPhone}
              onInput={(e) => updateForm('receiverPhone', e.detail.value)}
              placeholder='请输入手机号'
            />
          </View>

          <View className='barrel-mine-form-item'>
            <Text className='barrel-mine-form-label'>回收地址</Text>
            <Input
              className='barrel-mine-form-input'
              value={form.receiverDetail}
              onInput={(e) => updateForm('receiverDetail', e.detail.value)}
              placeholder='请输入空桶回收地址'
            />
          </View>

          <View className='barrel-mine-form-item'>
            <Text className='barrel-mine-form-label'>备注(选填)</Text>
            <Textarea
              className='barrel-mine-form-textarea'
              value={form.remark}
              onInput={(e) => updateForm('remark', e.detail.value)}
              maxlength={200}
              showConfirmBar={false}
              placeholder='补充说明,如方便回收的时间'
            />
          </View>

          <View
            trackTarget='barrel-submit-refund'
            trackElementType='button'
            trackElementText='提交申请'
            className={`barrel-mine-submit-btn ${submitting ? 'barrel-mine-submit-btn--disabled' : ''}`}
            onClick={handleSubmit}
          >
            <Text className='barrel-mine-submit-text'>{submitting ? '提交中...' : '提交申请'}</Text>
          </View>
        </View>
      )}

      {/* 押金记录 */}
      <View className='barrel-mine-card'>
        <View className='barrel-mine-card-header'>
          <Text className='barrel-mine-card-title'>押金记录</Text>
          <Text className='barrel-mine-card-count'>共 {recordTotal} 条</Text>
        </View>
        {records.map((record) => (
          <View key={record.id} className='barrel-mine-record'>
            <View className='barrel-mine-record-main'>
              <Text className='barrel-mine-record-spec'>{record.specification || '水桶押金'}</Text>
              <Text className='barrel-mine-record-amount'>{money(record.depositAmountCents)}</Text>
            </View>
            <View className='barrel-mine-record-sub'>
              <Text className='barrel-mine-record-meta'>
                {record.bucketCount != null ? `${record.bucketCount} 桶` : ''}
                {record.unitDepositCents != null ? ` · 单桶押金 ${money(record.unitDepositCents)}` : ''}
              </Text>
              <StatusTag
                tone={DEPOSIT_STATUS_MAP[record.status]?.tone}
                text={DEPOSIT_STATUS_MAP[record.status]?.text ?? record.status}
                size='small'
              />
            </View>
            <Text className='barrel-mine-record-time'>{formatDateTime(record.createdAt)}</Text>
            {record.refundRemark && (
              <Text className='barrel-mine-record-remark'>退还备注:{record.refundRemark}</Text>
            )}
          </View>
        ))}
        {records.length === 0 && <Text className='barrel-mine-spec-empty'>暂无押金记录</Text>}
        {loadingMore && <Text className='barrel-mine-load-more'>加载中...</Text>}
        {!recordHasMore && records.length > 0 && (
          <Text className='barrel-mine-load-more'>没有更多了</Text>
        )}
      </View>

      {/* 退押申请记录 */}
      <View className='barrel-mine-card'>
        <View className='barrel-mine-card-header'>
          <Text className='barrel-mine-card-title'>退押申请记录</Text>
        </View>
        {applications.map((app) => (
          <View key={app.id} className='barrel-mine-record'>
            <View className='barrel-mine-record-main'>
              <Text className='barrel-mine-record-spec'>
                {app.specification} × {app.bucketCount} 桶
              </Text>
              <StatusTag
                tone={REFUND_STATUS_MAP[app.status]?.tone}
                text={REFUND_STATUS_MAP[app.status]?.text ?? app.status}
                size='small'
              />
            </View>
            <View className='barrel-mine-record-sub'>
              <Text className='barrel-mine-record-meta'>退押金额 {money(app.depositCents)}</Text>
              <Text className='barrel-mine-record-meta'>{formatDateTime(app.createdAt)}</Text>
            </View>
            {app.recoveredAt && (
              <Text className='barrel-mine-record-time'>空桶回收:{formatDateTime(app.recoveredAt)}</Text>
            )}
            {app.refundedAt && (
              <Text className='barrel-mine-record-time'>押金退还:{formatDateTime(app.refundedAt)}</Text>
            )}
            {app.status === 'REJECTED' && (
              <Text className='barrel-mine-record-reject'>
                驳回原因:{app.rejectReason || '无'}
              </Text>
            )}
          </View>
        ))}
        {applications.length === 0 && (
          <Text className='barrel-mine-spec-empty'>暂无退押申请</Text>
        )}
      </View>
    </BasePageLayout>
  )
}

export default BarrelMinePage
