import React, { useState, useEffect } from 'react'
import { View, Text, Textarea, Input, Image } from '@tarojs/components'
import Taro, { useLoad, showToast } from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { pageTheme } from '../../styles/theme.stylex'

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

/**
 * 意见反馈样式(原 feedback 目录的 scss 迁移;750 基准 rpx,
 * 嵌套后代选择器已拍平;weui 平台变量保留 var 字符串引用)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    boxSizing: 'border-box',
    backgroundColor: 'var(--weui-BG-0)',
  },
  intro: {
    paddingTop: '24rpx',
  },
  introText: {
    fontSize: '26rpx',
    color: 'var(--weui-FG-1)',
  },
  // 类型芯片容器(可换行)
  types: {
    display: 'flex',
    flexWrap: 'wrap',
    rowGap: '16rpx',
    columnGap: '16rpx',
  },
  type: {
    height: '56rpx',
    paddingTop: '0',
    paddingBottom: '0',
    paddingLeft: '24rpx',
    paddingRight: '24rpx',
    borderRadius: '8rpx',
    cursor: 'default',
  },
  // 内容文本域(去自带 padding 贴 cell)
  textarea: {
    minHeight: '192rpx',
    paddingTop: '16rpx',
    paddingRight: '0',
    paddingBottom: '0',
    paddingLeft: '0',
    backgroundColor: 'transparent',
  },
  // 图片上传格(图片铺满 + 删除角标)
  fileImg: {
    position: 'absolute',
    left: '0',
    top: '0',
    width: '100%',
    height: '100%',
  },
  fileDel: {
    position: 'absolute',
    top: '0',
    right: '0',
    width: '44rpx',
    height: '44rpx',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    borderRadius: '0 0 0 var(--weui-radius)',
    color: tokens.onBrand,
    fontSize: '28rpx',
    lineHeight: 1,
  },
  // 页脚(热线 + 反馈记录链接,居中)
  footerTip: {
    textAlign: 'center',
  },
  footer: {
    paddingTop: '16rpx',
    paddingBottom: '48rpx',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    rowGap: '12rpx',
  },
  footerText: {
    fontSize: '26rpx',
    color: 'var(--weui-FG-1)',
  },
  footerLink: {
    fontSize: '26rpx',
    color: 'var(--weui-LINK)',
  },
})

/**
 * 意见反馈(feedback.html:WeUI 表单 —— 类型 .weui-tag--md 芯片、
 * 内容 .weui-textarea/__counter、图片 .weui-uploader、联系方式 cells、
 * 提交 .weui-btn_area;页脚热线 + 反馈记录链接)
 */
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
    <View {...stylex.props(pageTheme, styles.page)}>
      <View {...props('mp-pad-16', styles.intro)}>
        <Text {...stylex.props(styles.introText)}>您的意见对我们很重要</Text>
      </View>

      {/* 反馈类型(feedback.html .weui-tag--md 芯片,选中 --brand) */}
      <View className='weui-cells__title'>反馈类型</View>
      <View className='weui-cells'>
        <View className='weui-cell weui-cell_vertical'>
          <View {...stylex.props(styles.types)}>
            {feedbackTypes.map((type) => (
              <View
                key={type.value}
                {...props('weui-tag weui-tag--md', styles.type, form.type === type.value && 'weui-tag--brand')}
                trackTarget='feedback-type'
                trackElementType='tag'
                trackElementText={type.label}
                onClick={() => handleTypeSelect(type.value)}
              >
                <Text>{type.label}</Text>
              </View>
            ))}
          </View>
        </View>
      </View>

      {/* 反馈内容(.weui-textarea + __counter) */}
      <View className='weui-cells__title'>反馈内容</View>
      <View className='weui-cells'>
        <View className='weui-cell weui-cell_vertical'>
          <Textarea
            {...props('weui-textarea', styles.textarea)}
            placeholderClass='ph'
            placeholder='请详细描述您遇到的问题或建议，至少输入10个字'
            value={form.content}
            onInput={(e) => handleInputChange('content', e.detail.value)}
            maxlength={500}
            showConfirmBar={false}
          />
          <View className='weui-textarea__counter'>
            反馈内容至少输入10个字　{form.content.length}/500
          </View>
        </View>
      </View>

      {/* 图片(≤3 张,.weui-uploader) */}
      <View className='weui-cells__title'>图片（≤3 张）</View>
      <View className='weui-cells'>
        <View className='weui-cell weui-cell_vertical'>
          <View className='weui-uploader'>
            {form.images.map((image, index) => (
              <View key={index} className='weui-uploader__file'>
                <Image {...stylex.props(styles.fileImg)} src={image} mode='aspectFill' />
                <View
                  {...stylex.props(styles.fileDel)}
                  onClick={() => handleRemoveImage(index)}
                >
                  <Text>×</Text>
                </View>
              </View>
            ))}
            {form.images.length < 3 && (
              <View
                className='weui-uploader__input'
                trackTarget='feedback-upload'
                trackElementType='button'
                trackElementText='添加图片'
                onClick={handleChooseImage}
              />
            )}
          </View>
        </View>
      </View>
      <View className='weui-cells__tips'>最多上传 3 张,支持相册 / 拍照,可删除。</View>

      {/* 联系方式(选填) */}
      <View className='weui-cells mt3'>
        <View className='weui-cell'>
          <View className='weui-cell__hd'>
            <View className='weui-label'>联系方式</View>
          </View>
          <View className='weui-cell__bd'>
            <Input
              className='weui-input'
              placeholderClass='ph'
              placeholder='手机号 / 微信，便于我们联系您'
              value={form.contactInfo}
              onInput={(e) => handleInputChange('contactInfo', e.detail.value)}
              maxlength={100}
            />
          </View>
        </View>
      </View>

      {/* 提交(.weui-btn_area,提交中禁用防重复) */}
      <View className='weui-btn_area'>
        <View
          trackTarget="feedback-submit"
          trackAction="FORM_SUBMIT"
          trackElementType="button"
          trackElementText="提交反馈"
          trackDisabled={submitting}
          onClick={handleSubmit}
          className={`weui-btn weui-btn_primary flex1 ${submitting ? 'weui-btn_disabled' : ''}`}
        >
          {submitting ? '提交中...' : '提交反馈'}
        </View>
      </View>

      {/* 页脚说明 + 反馈记录链接(feedback.html) */}
      <View {...props('weui-cells__tips', styles.footerTip)}>我们会在1-2个工作日内处理您的反馈</View>
      <View {...stylex.props(styles.footer)}>
        <Text {...stylex.props(styles.footerText)}>客服热线 {storeProfile?.serviceHotline || CONTACT_INFO.SERVICE_HOTLINE}</Text>
        <Text
          {...stylex.props(styles.footerLink)}
          trackTarget="feedback-records-link"
          trackAction="CLICK"
          trackElementType="link"
          trackElementText="我的反馈记录"
          onClick={() => Taro.navigateTo({ url: '/pages/feedback-records/index' })}
        >
          我的反馈记录 ›
        </Text>
      </View>
      <View className='safe-bottom' />
    </View>
  )
}

export default Feedback
