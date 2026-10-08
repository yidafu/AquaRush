import { Platform } from 'react-native';
import { createURL } from 'expo-linking';
import { formatCentsToCurrency, normalizeCents } from '@aquarush/common';
import { isWXAppInstalled, registerApp, shareWebpage } from '@/services/wechat-sdk';

import type { PosterOrderData } from '@/components/order-poster';

// 微信分享场景：聊天界面（WXScene.WXSceneSession = 0）。枚举未从 d.ts 导出，直接使用数值。
const WX_SCENE_SESSION = 0;

let registered = false;

/** 微信 SDK 注册（幂等）。未配置开放平台移动应用 appId 时返回 false。 */
export async function registerWechat(): Promise<boolean> {
  const appId = process.env.EXPO_PUBLIC_WECHAT_MOBILE_APP_ID;
  const universalLink = process.env.EXPO_PUBLIC_WECHAT_UNIVERSAL_LINK;
  if (!appId) return false;
  if (registered) return true;
  try {
    registered = await registerApp(appId, universalLink || undefined);
    return registered;
  } catch (e) {
    console.error('微信注册失败:', e);
    return false;
  }
}

/** 订单深链：app.config.js 配置 EXPO_PUBLIC_DEEP_LINK_DOMAIN 后返回 https 链接，否则为 deliveryapp:// scheme 链接。 */
export function buildOrderDeepLink(orderNo: string): string {
  return createURL(`/order/${orderNo}`);
}

/**
 * 分享订单卡片到微信（海报作缩略图 + 深链）。返回 false 表示未安装/未配置，由调用方走系统分享兜底。
 * 微信缩略图 ≤32KB：带缩略图失败时降级为不带缩略图重试。
 */
export async function shareOrderToWechat(
  order: PosterOrderData,
  posterUri: string | null,
): Promise<boolean> {
  if (Platform.OS === 'web') return false;

  const registeredOk = await registerWechat();
  if (!registeredOk) return false;

  try {
    if (!(await isWXAppInstalled())) return false;
  } catch (e) {
    console.error('检查微信安装失败:', e);
    return false;
  }

  const { address, amountCents } = order;
  const firstItem = Array.isArray(order.items) && order.items.length > 0 ? order.items[0] : undefined;
  const fullAddress = address
    ? `${address.province || ''}${address.city || ''}${address.district || ''}${address.detailAddress || ''}`
    : '';
  const title = address ? `${address.receiverName || ''} - ${fullAddress}` : '订单详情';
  const description = `${firstItem?.product?.name || '商品'} x${firstItem?.quantity ?? 1} ${formatCentsToCurrency(normalizeCents(amountCents) ?? 0)}`;
  const webpageUrl = buildOrderDeepLink(order.orderNo || '');

  const share = (thumbImageUrl?: string) =>
    shareWebpage({
      webpageUrl,
      title,
      description,
      thumbImageUrl,
      scene: WX_SCENE_SESSION,
    });

  try {
    await share(posterUri ?? undefined);
    return true;
  } catch (e) {
    // 缩略图超限（32KB）等失败：降级不带缩略图重试
    console.error('微信分享失败（重试不带缩略图）:', e);
    try {
      await share(undefined);
      return true;
    } catch (e2) {
      console.error('微信分享失败:', e2);
      return false;
    }
  }
}