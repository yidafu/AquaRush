/**
 * 用户操作上报 SDK - 类型定义
 *
 * 与后端 `dev.yidafu.aqua.logging.dto` 的 UserActionLogRequest / BatchUserAction
 * 字段对齐(见 modules/aqua-foundation/.../logging/dto/UserActionLogRequest.kt)。
 * 上报接口:`POST /api/logs/user-actions/batch`(需认证)。
 */
export type TrackActionType =
  | 'PAGE_VIEW'
  | 'CLICK'
  | 'FORM_SUBMIT'
  | 'SEARCH'
  | 'SHARE'
  | 'SCROLL'
  | 'FILE_OPERATION'
  | (string & {})

/** 点击/触摸坐标(后端 CoordinatesDto,可选;当前各端默认不采集) */
export interface Coordinates {
  screenX: number
  screenY: number
  pageX: number
  pageY: number
}

/** 与后端 BatchUserAction 字段对齐的上报载荷 */
export interface TrackPayload {
  userId?: string
  username?: string
  actionType: string
  target: string
  pageUrl?: string
  elementId?: string
  elementType?: string
  elementText?: string
  coordinates?: Coordinates
  properties: Record<string, unknown>
  timestamp: number
}

/** 用户上下文(登录态),SDK 在 flush 时经 adapter 解析 */
export interface AuthContext {
  userId?: string
  username?: string
}

/**
 * 平台适配器:由各端注入,隔离所有平台 API(Taro / react-native / DOM)。
 *
 * 方法支持同步或异步返回值(异步返回值为 RN AsyncStorage 设计);
 * SDK 统一 `await` 处理。
 */
export interface TrackingAdapter {
  /** 批量上报(需自带认证头);失败必须 reject,由 SDK 静默重入队 */
  send(actions: TrackPayload[]): Promise<void>
  /** 当前页面路由;取不到时返回空串 */
  getCurrentRoute(): string
  /** 当前用户上下文(userId/username);取不到时返回空对象 */
  getAuthContext(): AuthContext | Promise<AuthContext>
  /** 是否有可用 token;无 token 时 SDK 仅入队不上报 */
  hasToken(): boolean | Promise<boolean>
}

/** 页面埋点配置:路由 → 页面名/附加属性(缺省回退原始路由) */
export interface PageTrackConfig {
  /** 页面中文名(作为 PAGE_VIEW 的 properties.pageTitle) */
  title: string
  /** 页面级附加属性(合并进 properties) */
  props?: Record<string, unknown>
}

export interface TrackingOptions {
  /** 队列上限,默认 20(超出丢最旧) */
  maxQueue?: number
  /** 定时 flush 间隔(毫秒),默认 5000 */
  flushIntervalMs?: number
  /** 同路由 PAGE_VIEW 去重窗口(毫秒),默认 500 */
  pageViewDedupeMs?: number
  /** 页面路由 → 页面名/附加属性映射(trackPageView 使用) */
  pageTrackConfig?: Record<string, PageTrackConfig>
}

/**
 * track-* props 契约(声明式埋点,由 babel-plugin-aquarush-track 编译为 <Track> 包裹)。
 *
 * 自研组件:props 接口 extends TrackAttributes;
 * 三方组件(antd / taro-ui 等):`{...( { trackTarget: 'x' } as TrackAttributes )}` 断言后 spread。
 */
export interface TrackAttributes {
  /** 操作对象标识(必填;缺失时插件跳过该元素并告警) */
  trackTarget?: string
  /** 动作类型,缺省 CLICK */
  trackAction?: string
  /** 附加属性:对象表达式 / JSON 字符串均可(字符串由插件编译期解析) */
  trackProps?: Record<string, unknown> | string
  trackElementId?: string
  trackElementType?: string
  trackElementText?: string
  /** 动态跳过上报(仍转发事件);与 trackIgnore 不同,支持任意表达式 */
  trackDisabled?: boolean
  /** 值为真(字面量/常量表达式)时插件剥离全部 track-* 属性、不包裹(不参与上报) */
  trackIgnore?: boolean | string
  /** 散列键:trackXxx → properties.xxx */
  [key: `track${string}`]: unknown
}
