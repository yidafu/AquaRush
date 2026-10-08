/**
 * 声明式埋点类型增强(weapp)
 *
 * 让 Taro 宿主组件(View/Text/Button/Input...)与 taro-ui 组件(AtButton...)
 * 直接接受 track-* props(编译期由 babel-plugin-aquarush-track 剥离并包裹 <Track>)。
 * 契约见 @aquarush/common/tracking 的 TrackAttributes。
 *
 * 注意:本文件从根入口 '@aquarush/common' 导入类型——weapp 的
 * tsconfig 是 moduleResolution: node,common 的 exports 子路径
 * './tracking' 在 Node10 下无法解析(既有问题,见 src/utils/tracking.ts)。
 */
import type { TrackAttributes } from '@aquarush/common'

declare module '@tarojs/components' {
  interface StandardProps<
    T = any,
    TouchEvent extends import('@tarojs/components/types/common').BaseTouchEvent<any> = import('@tarojs/components/types/common').ITouchEvent,
  > extends TrackAttributes {}
}

declare module 'taro-ui/types/base' {
  interface AtComponent extends TrackAttributes {}
}
