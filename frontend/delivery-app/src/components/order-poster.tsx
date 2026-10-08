import type { RefObject } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Image } from 'expo-image';
import { captureRef } from 'react-native-view-shot';
import { formatCentsToCurrency, formatDateTime, normalizeCents } from '@aquarush/common';
import type { Order } from '@aquarush/common';

import { resolveAssetUrl } from '@/utils/url';

/** 海报数据（与 weapp PosterCanvas 的 OrderData 对应） */
export interface PosterOrderData {
  orderNo?: string;
  amountCents?: number | string;
  createdAt?: string;
  /** 商品明细(F-ORD-004 多商品下单,海报展示首件) */
  items?: Array<{
    productId?: string | number;
    quantity?: number;
    product?: {
      name?: string;
      coverImageUrl?: string;
    } | null;
  }>;
  address?: {
    receiverName?: string;
    phone?: string;
    province?: string;
    city?: string;
    district?: string;
    detailAddress?: string;
  };
}

/** 订单详情 Order → 海报数据（处理 Maybe 空值） */
export function toPosterOrderData(order: Order): PosterOrderData {
  return {
    orderNo: order.orderNo,
    amountCents: order.amountCents,
    createdAt: order.createdAt,
    // F-ORD-004 多商品:海报展示首件商品
    items: order.items
      ? order.items.map(item => ({
          productId: item.productId,
          quantity: item.quantity,
          product: {
            name: item.product?.name ?? undefined,
            coverImageUrl: item.product?.coverImageUrl ?? undefined,
          },
        }))
      : undefined,
    address: order.address
      ? {
          receiverName: order.address.receiverName ?? undefined,
          phone: order.address.phone ?? undefined,
          province: order.address.province ?? undefined,
          city: order.address.city ?? undefined,
          district: order.address.district ?? undefined,
          detailAddress: order.address.detailAddress ?? undefined,
        }
      : undefined,
  };
}

// 海报固定白底 + 固定色（weapp wxml-to-canvas 坐标，380×304）。
// 刻意不走主题：海报是微信分享产物，任何模式下都必须保持白底白卡。
// 价格按品牌规则用主色（#3A9BD9）加粗，不用强调橙。
const POSTER_WIDTH = 380;
const POSTER_HEIGHT = 304;
const COLORS = {
  card: '#ffffff',
  cardBorder: '#f5f5f4',
  imagePlaceholder: '#f5f5f5',
  textPrimary: '#333333',
  textSecondary: '#666666',
  textTertiary: '#999999',
  price: '#3A9BD9',
  divider: '#f0f0f0',
  footerBg: '#f8f8f8',
};

// 手机号脱敏：138****5678
function maskPhone(phone?: string): string {
  if (!phone || phone.length < 7) return phone ?? '';
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
}

function buildFullAddress(order: PosterOrderData): string {
  const a = order.address;
  return a
    ? `${a.province || ''}${a.city || ''}${a.district || ''}${a.detailAddress || ''}`
    : '';
}

/**
 * 订单分享海报视图（380×304 白底卡片，固定色，不走主题）。
 * 预览直接渲染本组件；导出用 capturePosterView 捕获。
 */
export function PosterView({ order }: { order: PosterOrderData }) {
  const firstItem = order.items?.[0];
  const productName = (firstItem?.product?.name || '商品').slice(0, 12);
  const receiver = order.address?.receiverName || '';
  const maskedPhone = maskPhone(order.address?.phone);
  const coverImageUrl = firstItem?.product?.coverImageUrl
    ? resolveAssetUrl(firstItem.product.coverImageUrl)
    : null;

  return (
    <View style={styles.poster}>
      {/* 商品行 */}
      <View style={styles.productRow}>
        <View style={styles.imagePlaceholder}>
          {coverImageUrl ? (
            <Image source={{ uri: coverImageUrl }} style={styles.productImage} contentFit="cover" />
          ) : null}
        </View>
        <View style={styles.productInfo}>
          <Text style={styles.productName} numberOfLines={1}>
            {productName}
          </Text>
          <View style={styles.productMeta}>
            <Text style={styles.productPrice}>
              {formatCentsToCurrency(normalizeCents(order.amountCents) ?? 0)}
            </Text>
            <Text style={styles.productQuantity}>X {firstItem?.quantity ?? 1}</Text>
          </View>
        </View>
      </View>

      {/* 派送地址 */}
      <View style={styles.addressSection}>
        <Text style={styles.sectionTitle}>📍 派送地址</Text>
        <Text style={styles.addressReceiver}>
          {receiver} {maskedPhone}
        </Text>
        <Text style={styles.addressDetail} numberOfLines={2}>
          {buildFullAddress(order)}
        </Text>
      </View>

      {/* 底部信息带 */}
      <View style={styles.footer}>
        <Text style={styles.footerText}>订单号: {order.orderNo || ''}</Text>
        <Text style={styles.footerText}>下单时间: {formatDateTime(order.createdAt)}</Text>
      </View>
    </View>
  );
}

/**
 * 捕获海报视图为 PNG 文件 uri（web/失败返回 null）。
 * 捕获的是调用方传入的视图 ref（如 Modal 中的预览视图），所见即所得。
 */
export async function capturePosterView(
  viewRef: RefObject<View | null>,
): Promise<string | null> {
  try {
    return await captureRef(viewRef, {
      format: 'png',
      quality: 1,
      result: 'tmpfile',
    });
  } catch (e) {
    console.error('生成海报失败:', e);
    return null;
  }
}

const styles = StyleSheet.create({
  poster: {
    width: POSTER_WIDTH,
    height: POSTER_HEIGHT,
    backgroundColor: COLORS.card,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: COLORS.cardBorder,
    padding: 20,
  },
  productRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  imagePlaceholder: {
    width: 80,
    height: 80,
    borderRadius: 8,
    marginRight: 15,
    backgroundColor: COLORS.imagePlaceholder,
    overflow: 'hidden',
  },
  productImage: {
    width: 80,
    height: 80,
  },
  productInfo: {
    flex: 1,
    alignSelf: 'stretch',
    paddingVertical: 4,
  },
  productName: {
    fontSize: 16,
    color: COLORS.textPrimary,
  },
  productMeta: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 12,
  },
  productPrice: {
    fontSize: 16,
    color: COLORS.price,
    fontWeight: 'bold',
  },
  productQuantity: {
    fontSize: 14,
    color: COLORS.textTertiary,
  },
  addressSection: {
    borderTopWidth: 1,
    borderTopColor: COLORS.divider,
    paddingTop: 10,
    gap: 4,
  },
  sectionTitle: {
    fontSize: 14,
    color: COLORS.textPrimary,
  },
  addressReceiver: {
    fontSize: 14,
    color: COLORS.textSecondary,
  },
  addressDetail: {
    fontSize: 14,
    color: COLORS.textSecondary,
  },
  footer: {
    marginTop: 'auto',
    backgroundColor: COLORS.footerBg,
    borderRadius: 8,
    padding: 12,
    gap: 4,
  },
  footerText: {
    fontSize: 12,
    color: COLORS.textSecondary,
  },
});