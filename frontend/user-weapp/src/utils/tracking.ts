import Taro from '@tarojs/taro'
import { initTracking, type TrackingAdapter, type AuthContext } from '@aquarush/common/tracking'
import { networkManager } from './networkManager'
import { PAGE_TRACK_CONFIG } from '../config/tracking'

/**
 * 用户操作日志追踪器(weapp 端装配)
 *
 * 核心逻辑见 @aquarush/common/tracking(SDK),本文件仅注入 weapp 平台适配器:
 * - send:复用 networkManager(401 触发静默登录,与既有行为一致);
 * - 路由:Taro.getCurrentPages 页面栈顶;
 * - 登录上下文:本地存储 user_info;token:auth_token。
 */
const adapter: TrackingAdapter = {
  send: async (actions) => {
    await networkManager.post('/api/logs/user-actions/batch', { actions })
  },
  getCurrentRoute: () => {
    try {
      const pages = Taro.getCurrentPages()
      const current = pages[pages.length - 1]
      return current?.route ? `/${current.route}` : ''
    } catch {
      return ''
    }
  },
  getAuthContext: (): AuthContext => {
    try {
      const raw = Taro.getStorageSync('user_info')
      if (!raw) return {}
      const info = typeof raw === 'string' ? JSON.parse(raw) : raw
      return {
        userId: info?.id ? String(info.id) : undefined,
        username: info?.nickname || info?.username || undefined,
      }
    } catch {
      return {}
    }
  },
  hasToken: () => {
    try {
      return Boolean(Taro.getStorageSync('auth_token'))
    } catch {
      return false
    }
  },
}

/** 全局单例(页面路由映射见 src/config/tracking.ts) */
export const userActionTracker = initTracking(adapter, { pageTrackConfig: PAGE_TRACK_CONFIG })

export default userActionTracker
