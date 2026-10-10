import React, { useState, useEffect } from 'react'
import { View, Text, Textarea, Input, Button, Image } from '@tarojs/components'
import Taro, { useLoad, showToast } from '@tarojs/taro'
import './index.scss'

// 导入常量(店铺信息未配置时兜底展示)
import { CONTACT_INFO } from '../../constants'
// 反馈提交服务(G04 目标形态:真实落库)
import FeedbackService from '../../services/FeedbackService'
import StoreProfileService from '../../services/StoreProfileService'
import { FeedbackType, StoreProfileVo } from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

// 反馈类型(与后端 FeedbackType 枚举一一对应)
const FEEDBACK_TYPES = [
  { value: 'quality', label: '产品质量问题' },
  { value: 'delivery', label: '配送服务问题' },
  { value: 'app', label: '小程序使用问题' },
  { value: 'suggestion', label: '功能建议' },
  { value: 'complaint', label: '投诉建议' },
  { value: 'other', label: '其他问题' }
] as const

// 前端类型 → 后端 FeedbackType 枚举映射
const TYPE_TO_ENUM: Record<string, FeedbackType> = {
  quality: FeedbackType.PRODUCT_QUALITY,
  delivery: FeedbackType.DELIVERY_SERVICE,
  app: FeedbackType.APP_USAGE,
  suggestion: FeedbackType.SUGGESTION,
  complaint: FeedbackType.COMPLAINT,
  other: FeedbackType.OTHER
}

interface FeedbackForm {
  type: string
  content: string
  images: string[]
  contactInfo: string
}

const Feedback: React.FC = () => {
  usePageViewTracking()

  const [form, setForm] = useState<FeedbackForm>({
    type: '',
    content: '',
    images: [],
    contactInfo: ''
  })

  const [submitting, setSubmitting] = useState(false)
  // 店铺信息(F-OPS-004):客服热线动态读取,未配置时兜底常量
  const [storeProfile, setStoreProfile] = useState<StoreProfileVo | null>(null)

  useLoad(() => {
    console.log('Feedback page loaded.')
  })

  useEffect(() => {
    const loadProfile = async () => {
      const result = await StoreProfileService.getStoreProfile()
      if (result.success) {
        setStoreProfile(result.data)
      } else {
        console.error('Load store profile failed:', result.error.message)
      }
    }
    loadProfile()
  }, [])


  const feedbackTypes = FEEDBACK_TYPES

  const handleInputChange = (field: keyof FeedbackForm, value: string) => {
    setForm(prev => ({
      ...prev,
      [field]: value
    }))
  }

  const handleTypeSelect = (type: string) => {
    setForm(prev => ({
      ...prev,
      type
    }))
  }

  const validateForm = (): boolean => {
    if (!form.type) {
      showToast({ title: '请选择反馈类型', icon: 'none' })
      return false
    }

    if (!form.content.trim()) {
      showToast({ title: '请输入反馈内容', icon: 'none' })
      return false
    }

    if (form.content.length < 10) {
      showToast({ title: '反馈内容至少输入10个字', icon: 'none' })
      return false
    }

    return true
  }

  const handleSubmit = async () => {
    if (!validateForm()) {
      return
    }

    setSubmitting(true)

    try {
      // 真实提交:图片先经平台存储上传,再提交反馈落库(F-USR-005 目标形态)
      const result = await FeedbackService.submitFeedback({
        feedbackType: TYPE_TO_ENUM[form.type],
        content: form.content,
        images: form.images,
        contactInfo: form.contactInfo.trim() || undefined
      })

      if (result.success) {
        showToast({ title: '提交成功，感谢您的反馈', icon: 'success' })

        // 重置表单
        setForm({
          type: '',
          content: '',
          images: [],
          contactInfo: ''
        })
      } else {
        showToast({ title: result.error.message || '提交失败，请重试', icon: 'none' })
      }
    } catch (error) {
      console.error('Submit feedback failed:', error)
      showToast({ title: '提交失败，请重试', icon: 'error' })
    } finally {
      setSubmitting(false)
    }
  }

  const handleChooseImage = () => {
    Taro.chooseImage({
      count: 3,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        setForm(prev => ({
          ...prev,
          images: [...prev.images, ...res.tempFilePaths].slice(0, 3)
        }))
      },
      fail: (err) => {
        console.error('Choose image failed:', err)
      }
    })
  }

  const handleRemoveImage = (index: number) => {
    setForm(prev => ({
      ...prev,
      images: prev.images.filter((_, i) => i !== index)
    }))
  }

  return (
    <View className='feedback-page'>
      <View className='header'>
        <Text className='title'>意见反馈</Text>
        <Text className='subtitle'>您的意见对我们很重要</Text>
      </View>

      <View className='form-container'>
        <View className='form-item'>
          <Text className='label'>反馈类型 *</Text>
          <View className='type-selector'>
            {feedbackTypes.map((type) => (
              <View
                key={type.value}
                className={`type-option ${form.type === type.value ? 'selected' : ''}`}
                onClick={() => handleTypeSelect(type.value)}
              >
                <Text className='type-text'>{type.label}</Text>
              </View>
            ))}
          </View>
        </View>

        <View className='form-item'>
          <Text className='label'>反馈内容 *</Text>
          <Textarea
            className='textarea'
            placeholder='请详细描述您遇到的问题或建议，至少输入10个字'
            value={form.content}
            onInput={(e) => handleInputChange('content', e.detail.value)}
            maxlength={500}
            showConfirmBar={false}
          />
          <Text className='char-count'>{form.content.length}/500</Text>
        </View>

        <View className='form-item'>
          <Text className='label'>上传图片 (选填)</Text>
          <Text className='helper-text'>最多上传3张图片，有助于我们更好地了解问题</Text>
          <View className='image-upload'>
            {form.images.map((image, index) => (
              <View key={index} className='image-item'>
                <Image className='image' src={image} mode='aspectFill' />
                <View
                  className='delete-btn'
                  onClick={() => handleRemoveImage(index)}
                >
                  <Text className='delete-icon'>×</Text>
                </View>
              </View>
            ))}
            {form.images.length < 3 && (
              <View className='upload-btn' onClick={handleChooseImage}>
                <Text className='upload-icon'>+</Text>
                <Text className='upload-text'>添加图片</Text>
              </View>
            )}
          </View>
        </View>

        <View className='form-item'>
          <Text className='label'>联系方式 (选填)</Text>
          <Input
            className='contact-input'
            placeholder='手机号或微信号，便于我们联系您'
            value={form.contactInfo}
            onInput={(e) => handleInputChange('contactInfo', e.detail.value)}
            maxlength={100}
          />
        </View>

        <View className='submit-section'>
          <Button
            trackTarget="feedback-submit"
            trackAction="FORM_SUBMIT"
            trackElementType="button"
            trackElementText="提交反馈"
            trackDisabled={submitting}
            onClick={handleSubmit}
            className='submit-btn'
            loading={submitting}
            disabled={submitting}
          >
            {submitting ? '提交中...' : '提交反馈'}
          </Button>
        </View>
      </View>

      <View className='footer'>
        <Text className='footer-text'>我们会在1-2个工作日内处理您的反馈</Text>
        <Text className='footer-text'>如有紧急问题，请拨打客服热线：{storeProfile?.serviceHotline || CONTACT_INFO.SERVICE_HOTLINE}</Text>
        <Text
          className='footer-link'
          trackTarget="feedback-records-link"
          trackAction="CLICK"
          trackElementType="link"
          trackElementText="我的反馈记录"
          onClick={() => Taro.navigateTo({ url: '/pages/feedback-records/index' })}
        >
          我的反馈记录 ›
        </Text>
      </View>
    </View>
  )
}

export default Feedback
