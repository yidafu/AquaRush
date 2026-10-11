import React from 'react'
import { View, Text, Image, Button } from '@tarojs/components'
import { AtFloatLayout, AtInputNumber } from 'taro-ui'
import "taro-ui/dist/style/components/float-layout.scss"
import "taro-ui/dist/style/components/input-number.scss"
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { Money, calculateTotalCents, formatCentsToCurrency } from '@aquarush/common'

// 原 tailwind 工具类 + 内联样式 → StyleX;tailwind 值为 web 375 基准,长度 ×2 转 rpx。
// 内联 px 同样 ×2 转 rpx(taro 运行时不转换内联 px,渲染原始 px;×2 后 750 基准
// rpx 与原 375 基准 px 视口占比一致 —— 见 agents/frontend.md「StyleX 约定」)。
const styles = stylex.create({
  // AtFloatLayout 最小高(原内联 minHeight: 300px → ×2)
  floatLayout: {
    minHeight: '600rpx',
  },
  body: {
    paddingTop: '32rpx',
    paddingBottom: '32rpx',
    paddingLeft: '32rpx',
    paddingRight: '32rpx',
  },
  productRow: {
    display: 'flex',
    columnGap: '48rpx',
    paddingBottom: '24rpx',
    marginBottom: '32rpx',
    borderBottomWidth: '1rpx',
    borderBottomStyle: 'solid',
    borderBottomColor: 'var(--weui-FG-3)',
  },
  cover: {
    width: '160rpx',
    height: '160rpx',
    borderRadius: '24rpx',
    objectFit: 'cover',
  },
  productCol: {
    display: 'flex',
    flexDirection: 'column',
    flexGrow: 1,
    flexShrink: 1,
    rowGap: '24rpx',
  },
  productName: {
    fontSize: '32rpx',
    fontWeight: '600',
    lineHeight: 1.25,
    color: tokens.text,
  },
  productPrice: {
    fontSize: '28rpx',
    fontWeight: '700',
    color: tokens.price,
  },
  productMeta: {
    fontSize: '28rpx',
    color: tokens.textSub,
  },
  quantityRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: '64rpx',
  },
  quantityLabel: {
    display: 'block',
    marginBottom: 0,
    fontSize: '32rpx',
    fontWeight: '500',
    color: tokens.text,
  },
  totalRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: '16rpx',
    borderTopWidth: '1rpx',
    borderTopStyle: 'solid',
    borderTopColor: 'var(--weui-FG-3)',
  },
  totalLabel: {
    fontSize: '32rpx',
    color: tokens.textSub,
  },
  totalValue: {
    fontSize: '40rpx',
    fontWeight: '700',
    color: tokens.price,
  },
  actions: {
    position: 'sticky',
    bottom: '0',
    backgroundColor: tokens.card,
    paddingTop: '32rpx',
    paddingBottom: '32rpx',
    paddingLeft: '40rpx',
    paddingRight: '40rpx',
    borderTopWidth: '1rpx',
    borderTopStyle: 'solid',
    borderTopColor: 'var(--weui-FG-3)',
    display: 'flex',
    columnGap: '24rpx',
  },
  actionBtn: {
    flexGrow: 1,
    height: '64rpx',
    fontSize: '32rpx',
    borderRadius: '16rpx',
    backgroundColor: 'var(--weui-BG-1)',
    color: tokens.textSub,
  },
  confirmBtn: {
    flexGrow: 1,
    height: '64rpx',
    fontSize: '32rpx',
    borderRadius: '16rpx',
    backgroundColor: tokens.action, // 立即购买 CTA:强调橙
    borderColor: tokens.action,
  },
})

interface BuyModalProps {
  isOpened: boolean
  product: {
    id: string
    name: string
    priceCents: number
    originalPriceCents?: number
    specification: string
    coverImageUrl: string
    stock: number
  } | null
  quantity: number
  onQuantityChange: (value: number) => void
  onConfirm: () => void
  onCancel: () => void
}

const BuyModal: React.FC<BuyModalProps> = ({
  isOpened,
  product,
  quantity,
  onQuantityChange,
  onConfirm,
  onCancel
}) => {
  if (!product) return null

  // 水桶费不随商品订单捆绑收取(2026-08 改造,见 14-misc.md §G06)
  const totalCents = calculateTotalCents(quantity, product.priceCents)

  return (
    <AtFloatLayout
      isOpened={isOpened}
      onClose={onCancel}
      className={stylex.props(styles.floatLayout).className}
      title="确认购买"
    >
      <View {...stylex.props(styles.body)}>
        {/* 产品信息 */}
        <View {...stylex.props(styles.productRow)}>
          <Image
            src={product.coverImageUrl}
            mode='aspectFill'
            {...stylex.props(styles.cover)}
          />
          <View {...stylex.props(styles.productCol)}>
            <Text {...stylex.props(styles.productName)}>{product.name}</Text>
            <Text {...stylex.props(styles.productPrice)}><Money cents={product.priceCents} /></Text>
            <Text {...stylex.props(styles.productMeta)}>规格: {product.specification}</Text>
            <Text {...stylex.props(styles.productMeta)}>库存: {product.stock}</Text>
          </View>
        </View>

        {/* 数量选择 */}
        <View {...stylex.props(styles.quantityRow)}>
          <Text {...stylex.props(styles.quantityLabel)}>购买数量</Text>
          <AtInputNumber
            value={quantity}
            min={1}
            max={product.stock || 1}
            onChange={onQuantityChange}
            size='normal'
            type='number'
          />
        </View>

        {/* 总价 */}
        <View {...stylex.props(styles.totalRow)}>
          <Text {...stylex.props(styles.totalLabel)}>合计：</Text>
          <Text {...stylex.props(styles.totalValue)}>
            {formatCentsToCurrency(totalCents)}
          </Text>
        </View>
      </View>

      {/* 自定义操作按钮 */}
      <View {...stylex.props(styles.actions)}>
        <Button
          onClick={onCancel}
          {...stylex.props(styles.actionBtn)}
        >
          取消
        </Button>
        <Button
          trackTarget="buy-confirm"
          trackElementType="button"
          trackElementText="确认购买"
          trackProps={{ productId: product.id }}
          onClick={onConfirm}
          type='primary'
          {...stylex.props(styles.confirmBtn)}
        >
          确认购买
        </Button>
      </View>
    </AtFloatLayout>
  )
}

export default BuyModal
