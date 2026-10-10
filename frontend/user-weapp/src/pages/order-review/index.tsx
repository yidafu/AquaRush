import React, { useState, useEffect } from 'react'
import { View, Text, Textarea } from '@tarojs/components'
import Taro, { useLoad, showToast } from '@tarojs/taro'
import './index.scss'
import OrderService from '../../services/OrderService'
import { ReviewVo } from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import StarRating from '../../components/StarRating'

const MAX_COMMENT_LENGTH = 500

/** 星数 → 评价文案(设计稿 02-order/order-review.html「配送评分」右侧提示) */
const RATING_LABELS: string[] = ['请选择评分', '非常不满意', '不满意', '一般', '满意', '非常满意']

/**
 * 订单评价页(F-ORD-007,PRD 04-order.md §R-EVL-01;设计稿 02-order/order-review.html WeUI 视觉):
 * - 仅「已完成」订单可评价(入口由订单详情控制,后端亦拦截);
 * - 评分 1-5 星必选(星级品牌蓝);评论 ≤500 字;可匿名(.weui-switch);
 * - 一单一评,已评价订单只读展示,不可修改(weui-notice 提示 + 只读回显)。
 */
const OrderReview: React.FC = () => {
  usePageViewTracking()

  const [orderId, setOrderId] = useState('')
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  const [isAnonymous, setIsAnonymous] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [hasReviewed, setHasReviewed] = useState(false)
  const [existingReview, setExistingReview] = useState<ReviewVo | null>(null)
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
          <Text className='loading-text'>加载中...</Text>
        </View>
      </View>
    )
  }

  return (
    <View className='order-review-page'>
      {hasReviewed && existingReview ? (
        /* 已评价只读展示(一单一评,R-EVL-01) */
        <>
          <View className='weui-notice weui-notice--brand review-notice'>
            <Text>该订单已评价，评价内容不可修改</Text>
          </View>
          <View className='weui-cells__title title-row'>
            <Text>我的评价</Text>
            {existingReview.isAnonymous && <Text className='weui-tag weui-tag--outline'>匿名</Text>}
          </View>
          <View className='weui-cells'>
            <View className='weui-cell weui-cell_vertical'>
              <View className='review-head'>
                <StarRating rating={existingReview.rating} />
                <Text className='review-time'>{existingReview.createdAt}</Text>
              </View>
              {existingReview.comment && (
                <Text className='review-comment'>{existingReview.comment}</Text>
              )}
            </View>
          </View>
        </>
      ) : (
        <>
          {/* 星级评分(1-5,品牌蓝选中) */}
          <View className='weui-cells__title title-row'>
            <Text>配送评分</Text>
            <Text className='title-extra'>{RATING_LABELS[rating] ?? RATING_LABELS[0]}</Text>
          </View>
          <View className='weui-cells'>
            <View className='weui-cell weui-cell_vertical'>
              <StarRating rating={rating} readonly={false} onChange={setRating} />
              <Text className='section-desc'>为本次配送的送水工评分</Text>
            </View>
          </View>

          {/* 文字评论(≤500 字,.weui-textarea + 计数) */}
          <View className='weui-cells__title'>评价内容</View>
          <View className='weui-cells'>
            <Textarea
              className='weui-textarea comment-input'
              value={comment}
              maxlength={MAX_COMMENT_LENGTH}
              placeholder='分享这次送水体验，帮助其他用户参考（选填，最多500字）'
              placeholderClass='ph'
              onInput={(e) => setComment(e.detail.value)}
            />
            <View className='weui-textarea__counter'>{comment.length}/{MAX_COMMENT_LENGTH}</View>
          </View>

          {/* 匿名开关(.weui-cell_switch + .weui-switch) */}
          <View className='weui-cells'>
            <View className='weui-cell weui-cell_switch'>
              <View className='weui-cell__bd'>
                <View>匿名评价</View>
                <View className='switch-desc'>开启后对送水工隐藏你的身份信息</View>
              </View>
              <View className='weui-cell__ft'>
                <View
                  className={`weui-switch${isAnonymous ? ' on' : ''}`}
                  onClick={() => setIsAnonymous(!isAnonymous)}
                />
              </View>
            </View>
          </View>

          <View className='weui-btn_area'>
            <View
              className={`weui-btn weui-btn_primary flex1${submitting ? ' weui-btn_disabled' : ''}`}
              onClick={submitting ? undefined : handleSubmit}
            >
              提交评价
            </View>
          </View>
        </>
      )}
    </View>
  )
}

export default OrderReview
