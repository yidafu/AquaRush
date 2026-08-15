import { useDidShow } from '@tarojs/taro'
import { userActionTracker } from '../utils/tracking'

/**
 * 页面访问埋点 hook
 *
 * 在每个页面组件顶层调用一次(useDidShow 触发):
 *   usePageViewTracking()
 *
 * 上报 PAGE_VIEW,页面名与附加属性取自 src/config/tracking.ts 的路由映射。
 */
export function usePageViewTracking(): void {
  useDidShow(() => {
    userActionTracker.trackPageView()
  })
}

export default usePageViewTracking
