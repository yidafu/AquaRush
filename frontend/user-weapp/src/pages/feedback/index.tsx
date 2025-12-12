import React, { useState } from 'react'
import { View, Text, Textarea, Button, Image } from '@tarojs/components'
import Taro, { useLoad, showToast } from '@tarojs/taro'
import './index.scss'

// 导入常量
import { CONTACT_INFO } from '../../constants'

// 反馈类型
const FEEDBACK_TYPES = [
  { value: 'quality', label: '产品质量问题' },
  { value: 'delivery', label: '配送服务问题' },
  { value: 'app', label: '小程序使用问题' },
  { value: 'suggestion', label: '功能建议' },
  { value: 'complaint', label: '投诉建议' },
  { value: 'other', label: '其他问题' }
] as const

interface FeedbackForm {
  type: string
  content: string
  images: string[]
}

const Feedback: React.FC = () => {
  const [form, setForm] = useState<FeedbackForm>({
    type: '',
    content: '',
    images: []
  })

  const [submitting, setSubmitting] = useState(false)

  useLoad(() => {
    console.log('Feedback page loaded.')
  })


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
      // 这里应该调用API提交反馈数据
      // await submitFeedback(form)

      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1500))

      showToast({ title: '提交成功，感谢您的反馈', icon: 'success' })

      // 重置表单
      setForm({
        type: '',
        content: '',
        images: []
      })

      // 延迟返回上一页
      setTimeout(() => {
        // navigateBack() 如果需要的话
      }, 1500)

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

        <View className='submit-section'>
          <Button
            className='submit-btn'
            onClick={handleSubmit}
            loading={submitting}
            disabled={submitting}
          >
            {submitting ? '提交中...' : '提交反馈'}
          </Button>
        </View>
      </View>

      <View className='footer'>
        <Text className='footer-text'>我们会在1-2个工作日内处理您的反馈</Text>
        <Text className='footer-text'>如有紧急问题，请拨打客服热线：{CONTACT_INFO.SERVICE_HOTLINE}</Text>
      </View>
    </View>
  )
}

export default Feedback
