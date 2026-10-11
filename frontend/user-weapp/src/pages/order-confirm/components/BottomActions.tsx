import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { Money } from '@aquarush/common'

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
 *
 * 样式:StyleX(原 BottomActions.scss 迁移;750 基准 rpx,weui / mp-footbar 类保留;
 * 原后代选择器拍平到子元素;原双写 padding-bottom(constant + env)收敛为 env)
 */
const styles = stylex.create({
  bottomActions: {
    position: 'fixed',
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 100,
    paddingBottom: 'calc(16rpx + env(safe-area-inset-bottom))',
  },
  totalInfo: {
    textAlign: 'left',
  },
  totalDiscount: {
    display: 'flex',
    alignItems: 'baseline',
    rowGap: '8rpx',
    columnGap: '8rpx',
  },
})
const BottomActions: React.FC<BottomActionsProps> = ({
  totalAmount,
  loading,
  disabled = false,
  discountCents = 0,
  onSubmit
}) => {
  return (
    <View {...props('mp-footbar hair-t', styles.bottomActions)}>
      <View {...props('mp-footbar__info', styles.totalInfo)}>
        {discountCents > 0 && (
          <View {...props('mp-footbar__lbl', styles.totalDiscount)}>
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
