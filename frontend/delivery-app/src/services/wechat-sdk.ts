/**
 * 微信 SDK 平台入口（native）：透出 react-native-wechat-lib 的已用成员。
 * web 端由 wechat-sdk.web.ts 桩实现替换（Metro 按平台解析）。
 *
 * react-native-wechat-lib 在模块顶层读取 NativeModules.WeChat（见其 index.js），
 * 在 Expo Go / 未链接原生模块的构建中该原生模块为 null，顶层执行会抛
 * `TypeError: Cannot read property 'registerApp' of null`。
 * 因此这里不静态 import（会在模块加载期崩溃），改用运行时懒加载：
 * 仅在真正调用微信能力时才动态加载原生模块，缺失时返回「不支持」，与 web 桩语义一致。
 */
import type * as WechatSdk from 'react-native-wechat-lib';

type WechatSdkModule = typeof WechatSdk;

let sdkPromise: Promise<WechatSdkModule | null> | null = null;

function loadWechatSdk(): Promise<WechatSdkModule | null> {
  if (sdkPromise === null) {
    sdkPromise = import('react-native-wechat-lib').catch((e) => {
      console.error('react-native-wechat-lib 加载失败（原生模块缺失，微信分享不可用）:', e);
      return null;
    });
  }
  return sdkPromise;
}

export async function registerApp(appId: string, universalLink?: string): Promise<boolean> {
  const sdk = await loadWechatSdk();
  return sdk ? await sdk.registerApp(appId, universalLink) : false;
}

export async function isWXAppInstalled(): Promise<boolean> {
  const sdk = await loadWechatSdk();
  return sdk ? await sdk.isWXAppInstalled() : false;
}

export async function shareWebpage(
  message: WechatSdk.ShareWebpageMetadata,
): Promise<{ errCode?: number; errStr?: string }> {
  const sdk = await loadWechatSdk();
  return sdk
    ? await sdk.shareWebpage(message)
    : { errCode: -1, errStr: 'wechat share not supported (native module missing)' };
}
