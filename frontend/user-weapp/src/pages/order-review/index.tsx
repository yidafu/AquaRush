import React, { useState, useEffect } from 'react'
import { View, Text, Textarea, Button, Switch } from '@tarojs/components'
import Taro, { useLoad, showToast } from '@tarojs/taro'
import './index.scss'
import OrderService from '../../services/OrderService'
import { Review } from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

const MAX_COMMENT_LENGTH = 500

/**
 * 订单评价页(F-ORD-007,PRD 04-order.md §R-EVL-01):
 * - 仅「已完成」订单可评价(入口由订单详情控制,后端亦拦截);
 * - 评分 1-5 星必选;评论 ≤500 字;可匿名;
 * - 一单一评,已评价订单只读展示,不可修改。
 */
const OrderReview: React.FC = () => {
  usePageViewTracking()

  const [orderId, setOrderId] = useState('')
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  const [isAnonymous, setIsAnonymous] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [hasReviewed, setHasReviewed] = useState(false)
  const [existingReview, setExistingReview] = useState<Review | null>(null)
  const [loading, setLoading] = useState(true)

  useLoad(() => {
    const instance = Taro.getCurrentInstance()
    const { orderId } = instance.router?.params || {}
    if (!orderId) {
      showToast({ title: '订单ID缺失', icon: 'none' })
      setTimeout(() => Taro.navigateBack(), 1500)
      return
    }
    setOrderId(orderId)
  })

  useEffect(() => {
    if (!orderId) return
    const loadReview = async () => {
      const result = await OrderService.getInstance().getReviewCheck(orderId)
      if (result.success && result.data) {
        setHasReviewed(result.data.hasReviewed)
        setExistingReview(result.data.review ?? null)
        // 已评价 → 只读展示,不可修改(R-EVL-01)
        if (result.data.review) {
          setRating(result.data.review.rating)
          setComment(result.data.review.comment || '')
          setIsAnonymous(result.data.review.isAnonymous)
        }
      } else {
        console.error('获取评价信息失败:', result.error?.message)
        showToast({ title: '获取评价信息失败', icon: 'none' })
      }
      setLoading(false)
    }
    loadReview()
  }, [orderId])

  const handleSubmit = async () => {
    if (hasReviewed) return
    if (rating < 1) {
      showToast({ title: '请选择评分', icon: 'none' })
      return
    }
    if (comment.length > MAX_COMMENT_LENGTH) {
      showToast({ title: `评论不能超过${MAX_COMMENT_LENGTH}字`, icon: 'none' })
      return
    }

    setSubmitting(true)
    const result = await OrderService.getInstance().createReview({
      orderId,
      rating,
      comment: comment.trim() || undefined,
      isAnonymous
    })
    setSubmitting(false)

    if (!result.success) {
      showToast({ title: result.error?.message || '评价提交失败', icon: 'none' })
      return
    }
    showToast({ title: '评价成功', icon: 'success' })
    setTimeout(() => Taro.navigateBack(), 800)
  }

  if (loading) {
    return (
      <View className='order-review-page'>
        <View className='loading-container'>
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  return (
    <View className='order-review-page'>
      {hasReviewed && existingReview ? (
        /* 已评价只读展示(一单一评,R-EVL-01) */
        <View className='review-card'>
          <View className='review-card-title'>我的评价</View>
          <View className='stars'>
            {[1, 2, 3, 4, 5].map(star => (
              <Text key={star} className={`star ${star <= existingReview.rating ? 'active' : ''}`}>★</Text>
            ))}
          </View>
          {existingReview.comment && (
            <Text className='comment-text'>{existingReview.comment}</Text>
          )}
          <Text className='review-meta'>
            {existingReview.createdAt}
            {existingReview.isAnonymous ? ' · 匿名评价' : ''}
          </Text>
          <Text className='review-hint'>评价提交后不可修改</Text>
        </View>
      ) : (
        <View className='review-form'>
          {/* 星级评分(1-5) */}
          <View className='form-section'>
            <Text className='section-title'>服务评分</Text>
            <View className='stars picker'>
              {[1, 2, 3, 4, 5].map(star => (
                <Text
                  key={star}
                  className={`star ${star <= rating ? 'active' : ''}`}
                  onClick={() => setRating(star)}
                >
                  ★
                </Text>
              ))}
            </View>
            <Text className='section-desc'>为本次配送的送水工评分</Text>
          </View>

          {/* 文字评论(≤500 字) */}
          <View className='form-section'>
            <Text className='section-title'>文字评论</Text>
            <Textarea
              className='comment-input'
              value={comment}
              maxlength={MAX_COMMENT_LENGTH}
              placeholder='说说本次配送体验(选填,最多500字)'
              onInput={(e) => setComment(e.detail.value)}
            />
            <Text className='char-count'>{comment.length}/{MAX_COMMENT_LENGTH}</Text>
          </View>

          {/* 匿名开关 */}
          <View className='form-section row'>
            <View className='anonymous-info'>
              <Text className='section-title'>匿名评价</Text>
              <Text className='section-desc'>开启后送水工不显示您的身份</Text>
            </View>
            <Switch
              checked={isAnonymous}
              color='#3A9BD9'
              onChange={(e) => setIsAnonymous(e.detail.value)}
            />
          </View>

          <Button
            className='submit-button'
            type='primary'
            loading={submitting}
            disabled={submitting}
            onClick={handleSubmit}
          >
            提交评价
          </Button>
        </View>
      )}
    </View>
  )
}

export default OrderReview
