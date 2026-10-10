import React from 'react'
import { View, Text } from '@tarojs/components'
import { Money } from '@aquarush/common'
import './BottomActions.scss'

interface BottomActionsProps {
  totalAmount: number
  loading: boolean
  /** 计价失败/未就绪时禁用提交 */
  disabled?: boolean
  /** 总优惠金额(分,preview.totalDiscountCents);>0 时展示「已优惠 ¥x」 */
  discountCents?: number
  onSubmit: () => void
}

/**
 * 底部操作栏 —— WeUI .mp-footbar(信息列「已优惠 + 实付 .weui-price--lg」+「提交订单」主按钮,
 * 设计稿 02-order/order-confirm.html;track* 埋点属性由编译期插件改写,原样保留)
 */
const BottomActions: React.FC<BottomActionsProps> = ({
  totalAmount,
  loading,
  disabled = false,
  discountCents = 0,
  onSubmit
}) => {
  return (
    <View className='mp-footbar hair-t bottom-actions'>
      <View className='mp-footbar__info total-info'>
        {discountCents > 0 && (
          <View className='mp-footbar__lbl total-discount'>
            已优惠 <Text className='weui-price weui-price--sm'><Money cents={discountCents} /></Text>
          </View>
        )}
        <Text className='weui-price weui-price--lg'>
          <Money cents={totalAmount} />
        </Text>
      </View>
      <View
        className={`weui-btn weui-btn_primary${disabled || loading ? ' weui-btn_disabled' : ''}`}
        trackTarget='order-submit'
        trackAction='FORM_SUBMIT'
        trackElementType='button'
        trackElementText='提交订单'
        trackDisabled={disabled || loading}
        onClick={disabled || loading ? undefined : onSubmit}
      >
        {loading ? '提交中...' : '提交订单'}
      </View>
    </View>
  )
}

export default BottomActions
