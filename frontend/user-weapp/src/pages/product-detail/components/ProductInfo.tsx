import React from "react";
import { View, Text } from "@tarojs/components";
import * as stylex from "@stylexjs/stylex";
import { tokens } from "@aquarush/common/theme/tokens.stylex";
import { useProduct } from "../context/ProductContext";
import { displayCents, normalizeCents } from "@aquarush/common";

// 原文件为 tailwind 工具类(web 375 基准)→ StyleX:长度 ×2 转 rpx(750 基准)。
// 价格红用 tokens.price(微信电商惯例金额红),灰阶用 text 系 token,
// 优惠标签底 rgba(250,81,81,0.10) 为本组件局部色值,未入共享 token。
const fadeInUp = stylex.keyframes({
  '0%': { opacity: '0', transform: 'translateY(40rpx)' },
  '100%': { opacity: '1', transform: 'translateY(0)' },
});

const styles = stylex.create({
  section: {
    paddingTop: '16rpx',
    paddingBottom: '16rpx',
    paddingLeft: '32rpx',
    paddingRight: '32rpx',
    marginTop: '32rpx',
    backgroundColor: tokens.card,
    animationName: fadeInUp,
    animationDuration: '0.8s',
    animationTimingFunction: 'ease-out',
    animationDelay: '0.2s',
    animationFillMode: 'both',
  },
  titleBlock: {
    marginBottom: '16rpx',
  },
  nameWrap: {
    marginBottom: '8rpx',
  },
  name: {
    marginBottom: '8rpx',
    fontSize: '40rpx',
    fontWeight: '700',
    lineHeight: 1.25,
    color: tokens.text,
  },
  subtitleWrap: {
    display: 'flex',
    flexDirection: 'column',
    rowGap: '8rpx',
  },
  subtitle: {
    fontSize: '28rpx',
    fontWeight: '400',
    lineHeight: 1.625,
    color: tokens.textSub,
  },
  priceBlock: {
    display: 'flex',
    flexDirection: 'column',
    marginBottom: '16rpx',
    borderBottomWidth: '1rpx',
    borderBottomStyle: 'solid',
    borderBottomColor: tokens.border,
    animationName: fadeInUp,
    animationDuration: '0.8s',
    animationTimingFunction: 'ease-out',
    animationDelay: '0.2s',
    animationFillMode: 'both',
  },
  priceRow: {
    display: 'flex',
    alignItems: 'baseline',
    columnGap: '8rpx',
  },
  price: {
    fontSize: '40rpx',
    fontWeight: '800',
    letterSpacing: '-0.025em',
    color: tokens.price,
  },
  priceTag: {
    paddingLeft: '8rpx',
    paddingRight: '8rpx',
    fontSize: '24rpx',
    fontWeight: '500',
    color: tokens.price,
    backgroundColor: 'rgba(250, 81, 81, 0.10)',
    borderRadius: '8rpx',
  },
  metaRow: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  strikedWrap: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-end',
    rowGap: '16rpx',
    textAlign: 'right',
  },
  strikedLabel: {
    fontSize: '24rpx',
    fontWeight: '400',
    color: tokens.textWeak,
  },
  striked: {
    textDecorationLine: 'line-through',
  },
  soldWrap: {
    display: 'flex',
    alignItems: 'flex-end',
    columnGap: '16rpx',
    textAlign: 'right',
  },
  soldText: {
    fontSize: '24rpx',
    fontWeight: '400',
    color: tokens.textWeak,
  },
  specBlock: {
    paddingTop: '8rpx',
    paddingBottom: '8rpx',
    marginBottom: '16rpx',
    borderTopWidth: '1rpx',
    borderTopStyle: 'solid',
    borderTopColor: tokens.border,
    borderBottomWidth: '1rpx',
    borderBottomStyle: 'solid',
    borderBottomColor: tokens.border,
    animationName: fadeInUp,
    animationDuration: '0.8s',
    animationTimingFunction: 'ease-out',
    animationDelay: '0.2s',
    animationFillMode: 'both',
  },
  specRow: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '32rpx',
    marginBottom: '32rpx',
  },
  specRowLast: {
    marginBottom: 0,
  },
  specLabel: {
    fontSize: '28rpx',
    fontWeight: '500',
    color: tokens.textSub,
    minWidth: '160rpx',
  },
  specValue: {
    flexGrow: 1,
    flexShrink: 1,
    fontSize: '28rpx',
    fontWeight: '400',
    color: tokens.text,
  },
});

const ProductInfo: React.FC = () => {
  const { product } = useProduct();

  // 价格展示(R-MKT-02 单价链):按 priceSource 解析标签与划线价
  // GROUP_PRICE→熟客价、FLASH_SALE_PRICE→特价、NEWCOMER_PRICE→新人价;非目录价时划线目录价
  const priceCents = normalizeCents(product.priceCents) ?? 0;
  const effectivePriceCents = normalizeCents(product.effectivePriceCents) ?? priceCents;
  const priceSource = product.priceSource;
  const hasDiscountedPrice =
    (priceSource === 'GROUP_PRICE' || priceSource === 'FLASH_SALE_PRICE' || priceSource === 'NEWCOMER_PRICE') &&
    effectivePriceCents < priceCents;
  const priceTag =
    priceSource === 'GROUP_PRICE' ? '熟客价' : priceSource === 'FLASH_SALE_PRICE' ? '特价' : priceSource === 'NEWCOMER_PRICE' ? '新人价' : '';
  const originalPriceCents = normalizeCents(product.originalPriceCents);
  const hasMarketPrice = originalPriceCents !== null && originalPriceCents > 0;

  return (
    <View {...stylex.props(styles.section)}>
      {/* 产品标题和认证 */}
      <View {...stylex.props(styles.titleBlock)}>
        {/* 品牌名 + 商品名 + 规格 + 特性短语 */}
        <View {...stylex.props(styles.nameWrap)}>
          <Text {...stylex.props(styles.name)}>
            {product.name}
          </Text>
        </View>

        {/* 副标题/认证信息 */}
        {product.subtitle && (
          <View {...stylex.props(styles.subtitleWrap)}>
            <Text {...stylex.props(styles.subtitle)}>
              {product.subtitle}
            </Text>
          </View>
        )}
      </View>

      {/* 价格部分 */}
      <View {...stylex.props(styles.priceBlock)}>
        {/* 价格信息 */}
        <View {...stylex.props(styles.priceRow)}>
          <Text {...stylex.props(styles.price)}>
            {displayCents(hasDiscountedPrice ? effectivePriceCents : priceCents)}
          </Text>
          {hasDiscountedPrice && priceTag && (
            <Text {...stylex.props(styles.priceTag)}>
              {priceTag}
            </Text>
          )}
        </View>
        <View {...stylex.props(styles.metaRow)}>
          <View>
            {/* 特价/熟客价/新人价:划线展示目录价;否则展示原价(如配置) */}
            {hasDiscountedPrice ? (
              <View {...stylex.props(styles.strikedWrap)}>
                <Text {...stylex.props(styles.strikedLabel)}>
                  目录价 <Text {...stylex.props(styles.striked)}>
                  {displayCents(priceCents)}
                  </Text>

                </Text>
              </View>
            ) : (
              hasMarketPrice && (
                <View {...stylex.props(styles.strikedWrap)}>
                  <Text {...stylex.props(styles.strikedLabel)}>
                    原价 <Text {...stylex.props(styles.striked)}>
                    {displayCents(originalPriceCents)}
                    </Text>

                  </Text>
                </View>
              )
            )}
          </View>

          <View {...stylex.props(styles.soldWrap)}>
            <Text {...stylex.props(styles.soldText)}>
              已售 {product.salesVolume ?? 0} | 剩余 {product.stock ?? 0}
            </Text>
          </View>
        </View>
      </View>

      {/* 已选规格和发货信息 */}
      <View {...stylex.props(styles.specBlock)}>
        <View {...stylex.props(styles.specRow)}>
          <Text {...stylex.props(styles.specLabel)}>
            规格
          </Text>
          <Text {...stylex.props(styles.specValue)}>
            {product.specification}
          </Text>
        </View>

        <View {...stylex.props(styles.specRow, styles.specRowLast)}>
          <Text {...stylex.props(styles.specLabel)}>
            送货
          </Text>
          <Text {...stylex.props(styles.specValue)}>
            当日达
          </Text>
        </View>
      </View>

    </View>
  );
};

export default ProductInfo;
