import React, { useState, useEffect } from 'react'
import { View, Text, ScrollView, Input } from '@tarojs/components'
import Taro from '@tarojs/taro'
// @ts-ignore - taro-ui与React 18类型不兼容
import { AtForm, AtInput, AtButton, AtList, AtListItem, AtModal, AtModalContent, AtModalHeader, AtModalAction, AtNoticebar } from 'taro-ui'
import 'taro-ui/dist/style/components/form.scss'
import 'taro-ui/dist/style/components/input.scss'
import 'taro-ui/dist/style/components/button.scss'
import 'taro-ui/dist/style/components/list.scss'
import 'taro-ui/dist/style/components/modal.scss'
import "taro-ui/dist/style/components/noticebar.scss";

import { formatCentsToCurrency } from '../../utils/money'
import { submitDailyCollection, getTodayCollection, SubmitDailyCollectionInput, MyTodayCollectionVO } from '../../services/dailyCollection'
import { useAuth } from '../../hooks/useAuth'
import { PageContainer } from '../../components/PageContainer'
import OrderInfoRow from '../../components/OrderInfoRow'

import './index.scss'

// 容差阈值（分）
const TOLERANCE_CENTS = 100 // 1元 = 100分

// 状态文本映射
const statusTextMap: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  CONFIRMED: '已确认',
  DISPUTED: '有争议',
}

const DailyCollectionPage: React.FC = () => {
  const { workerInfo } = useAuth()
  const [formData, setFormData] = useState({
    cashAmount: '', // 元
    waterTicketCount: 0,
    qrCodeAmount: '', // 元
  })
  const [orderStats, setOrderStats] = useState<MyTodayCollectionVO | null>(null)
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  // 确认弹窗相关状态
  const [showConfirmModal, setShowConfirmModal] = useState(false)
  const [pendingCashDiff, setPendingCashDiff] = useState(0) // 现金差异
  const [pendingWaterTicketDiff, setPendingWaterTicketDiff] = useState(0) // 水票差异
  const [pendingQrCodeDiff, setPendingQrCodeDiff] = useState(0) // 扫码差异
  const [notes, setNotes] = useState('') // 差异说明

  // 加载今日收款记录
  useEffect(() => {
    loadTodayRecord()
  }, [])

  const loadTodayRecord = async () => {
    if (!workerInfo?.id) return

    setLoading(true)
    try {
      const res = await getTodayCollection()
      if (res?.myTodayCollection) {
        // 设置订单统计和已填写的数据
        setOrderStats(res.myTodayCollection)
        // 回填表单（如果有已填写的数据）
        if (res.myTodayCollection.cashAmountCents != null) {
          setFormData({
            cashAmount: String(res.myTodayCollection.cashAmountCents! / 100),
            waterTicketCount: res.myTodayCollection.waterTicketCount ?? 0,
            qrCodeAmount: String(res.myTodayCollection.qrCodeAmountCents! / 100),
          })
        }
      }
    } catch (error) {
      console.error('加载收款记录失败:', error)
    }
    setLoading(false)
  }

  // 输入变化处理 - AtInput 的 onChange 可能返回 number 类型
  const handleInputChange = (field: keyof typeof formData, value: string | number) => {
    // 处理 number 类型（如 AtInput type=number 返回数字）
    const processedValue = typeof value === 'number' ? (field === 'waterTicketCount' ? value : String(value)) : value
    setFormData((prev) => ({ ...prev, [field]: processedValue }))
  }

  // 提交收款
  const handleSubmit = async () => {
    if (!workerInfo?.id) {
      Taro.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    const cashAmount = parseFloat(formData.cashAmount) || 0
    const qrCodeAmount = parseFloat(formData.qrCodeAmount) || 0

    if (cashAmount === 0 && formData.waterTicketCount === 0 && qrCodeAmount === 0) {
      Taro.showToast({ title: '请填写收款金额', icon: 'none' })
      return
    }

    // 先计算差异，判断是否需要二次确认（分别判断3个分项）
    if (orderStats) {
      const cashCents = Math.round(cashAmount * 100)
      const qrCodeCents = Math.round(qrCodeAmount * 100)
      const waterTicketNum = formData.waterTicketCount

      // 现金差异
      const cashDiff = cashCents - (orderStats.calculatedCashAmountCents ?? 0)
      // 水票差异
      const waterTicketDiff = waterTicketNum - (orderStats.calculatedWaterTicketCount ?? 0)
      // 扫码差异
      const qrCodeDiff = qrCodeCents - (orderStats.calculatedQrCodeAmountCents ?? 0)

      // 任一差异超出容差，显示确认弹窗
      if (Math.abs(cashDiff) > TOLERANCE_CENTS ||
        waterTicketDiff !== 0 ||  // 水票数需要完全匹配
        Math.abs(qrCodeDiff) > TOLERANCE_CENTS) {
        setPendingCashDiff(cashDiff)
        setPendingWaterTicketDiff(waterTicketDiff)
        setPendingQrCodeDiff(qrCodeDiff)
        setShowConfirmModal(true)
        return
      }
    }

    // 容差内，直接提交
    await doSubmit('')
  }

  // 执行提交
  const doSubmit = async (submitNotes: string) => {
    if (!workerInfo?.id) return

    const cashAmount = parseFloat(formData.cashAmount) || 0
    const qrCodeAmount = parseFloat(formData.qrCodeAmount) || 0

    setSubmitting(true)
    try {
      const input: SubmitDailyCollectionInput = {
        cashAmountCents: Math.round(cashAmount * 100),
        waterTicketCount: formData.waterTicketCount,
        qrCodeAmountCents: Math.round(qrCodeAmount * 100),
        notes: submitNotes || undefined,
      }

      const res = await submitDailyCollection(input)
      if (res?.submitDailyCollection) {
        setOrderStats({
          ...orderStats!,
          cashAmountCents: input.cashAmountCents,
          waterTicketCount: input.waterTicketCount,
          qrCodeAmountCents: input.qrCodeAmountCents,
          status: 'SUBMITTED',
        })
        Taro.showToast({
          title: '提交成功',
          icon: 'success',
        })
      }
    } catch (error) {
      console.error('提交收款失败:', error)
      Taro.showToast({ title: '提交失败', icon: 'none' })
    }
    setSubmitting(false)
    setShowConfirmModal(false)
    setNotes('')
  }

  // 确认提交（有差异需要说明）
  const handleConfirm = async () => {
    if (!notes.trim()) {
      Taro.showToast({ title: '请输入情况说明', icon: 'none' })
      return
    }
    await doSubmit(notes.trim())
  }

  // 取消确认
  const handleCancelConfirm = () => {
    setShowConfirmModal(false)
    setNotes('')
    setPendingCashDiff(0)
    setPendingWaterTicketDiff(0)
    setPendingQrCodeDiff(0)
  }

  // 获取今日日期字符串
  const getTodayDate = () => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  }

  // 计算对比结果
  const calculateComparison = () => {
    const cashCents = Math.round(parseFloat(formData.cashAmount || '0') * 100)
    const qrCodeCents = Math.round(parseFloat(formData.qrCodeAmount || '0') * 100)
    const totalInput = cashCents + qrCodeCents

    if (orderStats) {
      const diff = totalInput - orderStats.orderAmountCents
      return {
        inputAmount: totalInput,
        orderAmount: orderStats.orderAmountCents,
        difference: diff,
        orderCount: orderStats.orderCount,
        matched: Math.abs(diff) <= 100, // 容差1元
      }
    }
    return null
  }


  return (
    <PageContainer title='每日收款'>
      <ScrollView className='daily-collection-page' scrollY>
        <AtNoticebar icon='volume-plus'>
          <Text>
            1. 现金和扫码收款将自动与今日完成订单金额对比
          </Text>
          <View>
            2. 差异在1元以内视为正常
          </View>
          <View>
            3. 提交后可由管理员确认审核
          </View>
        </AtNoticebar>

        <View className='flex flex-row section'>
          <View className='section-title'>收款日期</View>
          <View className='date-display'>{getTodayDate()}</View>
        </View>

        {/* 当前状态 */}
        {orderStats && orderStats.status && (
          <View className='section'>
            <View className='section-title'>当前状态</View>
            <View className='status-card'>
              <View className='status-item'>
                <Text className='label'>状态：</Text>
                <Text className={`value status-${orderStats.status!.toLowerCase()}`}>
                  {statusTextMap[orderStats.status!] || orderStats.status}
                </Text>
              </View>
            </View>
          </View>
        )}

        {/* 收款输入 */}
        <View className='section'>
          <View className='section-title'>今日收款录入</View>

          {/* 订单数据展示 */}
          <View className='order-info'>
            <View className='order-info-title'>今日订单情况</View>
            <OrderInfoRow
              label='订单总数：'
              value={`${orderStats?.orderCount ?? 0} 单`}
            />
            <OrderInfoRow
              label='现金应收：'
              value={formatCentsToCurrency(orderStats?.calculatedCashAmountCents ?? 0)}
            />
            <OrderInfoRow
              label='扫码应收：'
              value={formatCentsToCurrency(orderStats?.calculatedQrCodeAmountCents ?? 0)}
            />
            <OrderInfoRow
              label='水票应收：'
              value={`${orderStats?.calculatedWaterTicketCount ?? 0} 张`}
            />
          </View>

          <AtForm>
            <AtInput
              name='cashAmount'
              title='现金收款'
              type='digit'
              placeholder='请输入现金收款金额'
              value={formData.cashAmount}

              onChange={(value) => handleInputChange('cashAmount', value)}
            >
              <Text>元</Text>
            </AtInput>

            <AtInput
              name='waterTicketCount'
              title='水票数量'
              type='number'
              placeholder='请输入水票数量'
              value={String(formData.waterTicketCount)}
              onChange={(value) => handleInputChange('waterTicketCount', value)}
            >
              <Text>张</Text>
            </AtInput>

            <AtInput
              name='qrCodeAmount'
              title='扫码收款'
              type='digit'
              placeholder='请输入扫码收款金额'
              value={formData.qrCodeAmount}
              onChange={(value) => handleInputChange('qrCodeAmount', value)}
            >
              <Text>元</Text>
            </AtInput>
          </AtForm>

          <View className='form-item total'>
            <Text className='label'>合计：</Text>
            <Text className='value'>
              {formatCentsToCurrency(
                Math.round(parseFloat(formData.cashAmount || '0') * 100) +
                Math.round(parseFloat(formData.qrCodeAmount || '0') * 100)
              )}
            </Text>
          </View>
        </View>

        {/* 提交按钮 */}
        {/* @ts-ignore */}
        <AtButton
          type='primary'
          loading={submitting}
          onClick={handleSubmit}
        >
          {submitting ? '提交中...' : '提交收款'}
        </AtButton>

        {/* 确认弹窗 - 使用 taro-ui Modal */}
        {/* @ts-ignore */}
        <AtModal
          isOpened={showConfirmModal}
        >
          <AtModalHeader>金额差异确认</AtModalHeader>

          <AtModalContent>
            <View className='modal-input-wrap'>
              <View>现金差异：{formatCentsToCurrency(pendingCashDiff)}</View>
              <View>水票差异：{pendingWaterTicketDiff > 0 ? `+${pendingWaterTicketDiff}` : pendingWaterTicketDiff} 张</View>
              <View>扫码差异：{formatCentsToCurrency(pendingQrCodeDiff)}</View>
              <View>请输入差异原因说明：</View>
              <AtInput
                className='modal-input'
                type='text'
                value={notes}
                onChange={(value) => setNotes(value)}
                placeholder='请输入情况说明（如：某订单未收款、额外收到水票等）'
                maxlength={200}
              />
            </View>
          </AtModalContent>
          <AtModalAction>
            <AtButton onClick={handleCancelConfirm}>取消</AtButton>
            <AtButton type='secondary' onClick={handleConfirm}>确定</AtButton>
          </AtModalAction>

        </AtModal>
      </ScrollView>
    </PageContainer>
  )
}

export default DailyCollectionPage
