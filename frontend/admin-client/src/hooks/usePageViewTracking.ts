import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { userActionTracker } from '../utils/tracking'

/**
 * 页面访问埋点 hook(admin-client)
 *
 * 在布局组件顶层调用一次,路由变化时上报 PAGE_VIEW:
 *   usePageViewTracking()
 */
export function usePageViewTracking(): void {
  const location = useLocation()
  useEffect(() => {
    userActionTracker.trackPageView()
  }, [location.pathname])
}

export default usePageViewTracking
