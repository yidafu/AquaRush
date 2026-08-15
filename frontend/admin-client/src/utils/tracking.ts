import { initTracking, type TrackingAdapter, type AuthContext } from '@aquarush/common/tracking'
import { PAGE_TRACK_CONFIG } from '../config/tracking'

/**
 * 用户操作日志追踪器(admin-client / PC 端装配)
 *
 * 核心逻辑见 @aquarush/common/tracking(SDK),本文件仅注入 web 平台适配器:
 * - send:静默 fetch 上报(不走 utils/http.ts,避免其 antd message.error 拦截器
 *   在后台 flush 失败时弹出错误提示;上报失败绝不影响业务);
 * - 路由:window.location.pathname(事件发生时读取,SPA 下即当前页);
 * - 登录上下文:localStorage.userInfo(token:localStorage.token);
 * - 切后台(visibilitychange)立即 flush,避免事件滞留丢失。
 */

const adapter: TrackingAdapter = {
  send: async (actions) => {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/logs/user-actions/batch', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ actions }),
    })
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`)
    }
  },
  getCurrentRoute: () => {
    try {
      return window.location.pathname || ''
    } catch {
      return ''
    }
  },
  getAuthContext: (): AuthContext => {
    try {
      const raw = localStorage.getItem('userInfo')
      if (!raw) return {}
      const info = JSON.parse(raw)
      return {
        userId: info?.id != null ? String(info.id) : undefined,
        username: info?.username || info?.nickname || undefined,
      }
    } catch {
      return {}
    }
  },
  hasToken: () => {
    try {
      return Boolean(localStorage.getItem('token'))
    } catch {
      return false
    }
  },
}

/** 全局单例(页面路由映射见 src/config/tracking.ts) */
export const userActionTracker = initTracking(adapter, { pageTrackConfig: PAGE_TRACK_CONFIG })

// 浏览器切后台/隐藏标签页时立即上报缓冲队列
if (typeof document !== 'undefined' && typeof document.addEventListener === 'function') {
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
      void userActionTracker.flush()
    }
  })
}
