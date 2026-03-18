import React, { useState, useEffect } from 'react'
import { View, Text, Button, Input } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { deliveryAuthService } from '../../services/auth'
import './index.scss'

const BindPhonePage: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [phone, setPhone] = useState('')
  const [phoneError, setPhoneError] = useState('')

  // 手机号验证函数
  const validatePhone = (phoneNumber: string): boolean => {
    const phoneRegex = /^1[3-9]\d{9}$/
    return phoneRegex.test(phoneNumber)
  }

  // 手机号输入变化处理
  const handlePhoneChange = (e: any) => {
    const value = e.detail.value
    setPhone(value)
    if (value && !validatePhone(value)) {
      setPhoneError('请输入正确的手机号')
    } else {
      setPhoneError('')
    }
  }

  useEffect(() => {
    // Check if token exists (user is in pending state)
    const token = deliveryAuthService.getToken()
    if (!token) {
      Taro.showToast({
        title: '请先登录',
        icon: 'none'
      })
      setTimeout(() => {
        // Taro.redirectTo({ url: '/pages/login/index' })
      }, 1500)
    }
  }, [])

  const handleGetPhoneNumber = async (e: any) => {
    // 权限申请中
    // const { errMsg, phoneNumber } = e.detail

    // if (errMsg !== 'getPhoneNumber:ok') {
    //   console.error('Get phone number failed:', errMsg)
    //   setError('获取手机号失败，请重试')
    //   return
    // }

    // if (!phoneNumber) {
    //   setError('无法获取手机号，请检查微信权限设置')
    //   return
    // }
    console.log('get phone number')
    // 如果已输入手机号，使用输入的手机号；否则使用默认值
    const phoneNumber = phone && validatePhone(phone) ? phone : '187681193907'
    await bindPhone(phoneNumber)
  }

  const bindPhone = async (phoneNumber: string) => {
    try {
      setLoading(true)
      setError('')

      const response = await deliveryAuthService.bindPhone(phoneNumber)

      if (response.workerInfo) {
        Taro.showToast({
          title: '绑定成功',
          icon: 'success'
        })

        // Redirect to home after a short delay
        setTimeout(() => {
          Taro.switchTab({ url: '/pages/order-list/index' })
        }, 1500)
      }
    } catch (err: any) {
      console.error('Bind phone failed:', err)
      setError(err.message || '绑定失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <View className='bind-phone-page'>
      <View className='bind-container'>
        <View className='header-section'>
          <Text className='title'>绑定手机号</Text>
          <Text className='subtitle'>请绑定您的管理员或送水员手机号</Text>
        </View>

        <View className='content-section'>
          {error && (
            <View className='error-message'>
              <Text>{error}</Text>
            </View>
          )}

          <View className='phone-input-section'>
            <Text className='input-label'>手机号</Text>
            <Input
              className='phone-input'
              type='number'
              placeholder='请输入管理员或送水员手机号'
              value={phone}
              onInput={handlePhoneChange}
              maxlength={11}
            />
            {phoneError && (
              <Text className='phone-error-text'>{phoneError}</Text>
            )}
          </View>

          <View className='info-card'>
            <Text className='info-title'>绑定说明</Text>
            <Text className='info-text'>
              1. 仅限管理员或送水员账号绑定
            </Text>
            <Text className='info-text'>
              2. 绑定后可在小程序中接单和配送
            </Text>
            <Text className='info-text'>
              3. 绑定成功后下次登录无需再次绑定
            </Text>
          </View>

          <Button
            className='bind-phone-btn'
            type='primary'
            // 权限申请中，暂时忽略
            // openType='getPhoneNumber'
            // onGetPhoneNumber={handleGetPhoneNumber}
            onClick={handleGetPhoneNumber}
            loading={loading}
          >
            <Text>{loading ? '绑定中...' : '微信手机号一键绑定'}</Text>
          </Button>
        </View>

        <View className='footer-section'>
          <Button
            className='back-btn'
            type='default'
            onClick={() => Taro.navigateBack()}
          >
            <Text>返回</Text>
          </Button>
        </View>
      </View>
    </View>
  )
}

export default BindPhonePage
