/**
 * 微信 SDK Web 端桩实现。
 *
 * react-native-wechat-lib 模块顶层会读取 NativeModules.WeChat（web 上为 undefined，
 * 直接抛 TypeError），无法在 web 打包中静态导入。此文件由 Metro 在 web 平台替代
 * wechat-sdk.ts（平台文件解析优先），所有能力返回「不支持」。
 */
export async function registerApp(): Promise<boolean> {
  return false;
}

export async function isWXAppInstalled(): Promise<boolean> {
  return false;
}

export async function isWXAppSupportApi(): Promise<boolean> {
  return false;
}

export async function shareWebpage(): Promise<{ errCode: number; errStr: string }> {
  return { errCode: -1, errStr: 'wechat share not supported on web' };
}

export async function shareLocalImage(): Promise<{ errCode: number; errStr: string }> {
  return { errCode: -1, errStr: 'wechat share not supported on web' };
}
