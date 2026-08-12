/**
 * 微信 SDK 平台入口（native）：透出 react-native-wechat-lib 的已用成员。
 * web 端由 wechat-sdk.web.ts 桩实现替换（Metro 按平台解析）。
 */
import { isWXAppInstalled, registerApp, shareWebpage } from 'react-native-wechat-lib';

export { isWXAppInstalled, registerApp, shareWebpage };
