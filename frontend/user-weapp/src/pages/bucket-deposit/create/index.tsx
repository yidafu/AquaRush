import React, { useState, useEffect, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import { AtButton, AtInputNumber, AtToast } from 'taro-ui'
import Taro from '@tarojs/taro'
import BucketDepositService from '@/services/BucketDepositService'
import { displayCents } from '@/utils/money'
import BasePageLayout from '@/components/BasePageLayout'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/input-number.scss"
import "taro-ui/dist/style/components/toast.scss"
import './index.scss'

const BucketDepositCreatePage: React.FC = () => {
  const [quantity, setQuantity] = useState<number>(1)
  const [depositAmount, setDepositAmount] = useState<number>(0)
  const [totalAmount, setTotalAmount] = useState<number>(0)
  const [loading, setLoading] = useState<boolean>(false)
  const [toastOpen, setToastOpen] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')
  const [toastStatus, setToastStatus] = useState<string>('success')

  const bucketDepositService = BucketDepositService

  const loadDepositAmount = useCallback(async () => {
    try {
      const amount = await bucketDepositService.getBucketDepositAmount()
      setDepositAmount(amount)
      setTotalAmount(amount * quantity)
    } catch (error) {
      console.error('Failed to load deposit amount:', error)
      Taro.showToast({
        title: '加载失败',
        icon: 'none'
      })
    }
  }, [bucketDepositService, quantity])

  useEffect(() => {
    loadDepositAmount()
  }, [loadDepositAmount])

  useEffect(() => {
    setTotalAmount(depositAmount * quantity)
  }, [depositAmount, quantity])

  const handleQuantityChange = (value: number) => {
    setQuantity(value)
  }

  const handleSubmit = async () => {
    if (quantity <= 0) {
      setToastText('请输入押桶数量')
      setToastStatus('error')
      setToastOpen(true)
      return
    }

    setLoading(true)
    try {
      // 创建押桶记录
      const result = await bucketDepositService.createBucketDeposit(quantity)

      // TODO: 调用微信支付
      // 这里需要调用微信支付API，支付成功后更新状态

      // 模拟支付成功
      Taro.showToast({
        title: '押桶成功',
        icon: 'success'
      })

      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)
    } catch (error: any) {
      console.error('Failed to create bucket deposit:', error)
      setToastText(error.message || '押桶失败')
      setToastStatus('error')
      setToastOpen(true)
    } finally {
      setLoading(false)
    }
  }

  return (
    <BasePageLayout>
      <View className='bucket-deposit-create-page'>
        {/* 押桶说明 */}
        <View className='info-card'>
          <Text className='info-title'>押桶说明</Text>
          <Text className='info-content'>
            每桶押金 ¥{displayCents(depositAmount)}，退桶时全额退还。押桶后可在订单中抵扣桶装水费用。
          </Text>
        </View>

        {/* 押桶数量 */}
        <View className='quantity-card'>
          <Text className='quantity-label'>押桶数量</Text>
          <View className='quantity-input-container'>
            <AtInputNumber
              min={1}
              max={99}
              step={1}
              value={quantity}
              onChange={handleQuantityChange}
              className='quantity-input'
            />
            <Text className='quantity-unit'>个</Text>
          </View>
        </View>

        {/* 押金金额 */}
        <View className='amount-card'>
          <View className='amount-row'>
            <Text className='amount-label'>押金金额</Text>
            <Text className='amount-value'>¥{displayCents(totalAmount)}</Text>
          </View>
          <View className='amount-detail'>
            <Text className='detail-text'>
              {quantity}个 × ¥{displayCents(depositAmount)}/个
            </Text>
          </View>
        </View>

        {/* 提交按钮 */}
        <View className='submit-container'>
          <AtButton
            type='primary'
            className='submit-button'
            onClick={handleSubmit}
            loading={loading}
          >
            确认押桶
          </AtButton>
        </View>

        <AtToast
          isOpened={toastOpen}
          text={toastText}
          status={toastStatus}
          onClose={() => setToastOpen(false)}
        />
      </View>
    </BasePageLayout>
  )
}

export default BucketDepositCreatePage
