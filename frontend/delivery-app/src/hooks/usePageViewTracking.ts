import { useEffect } from 'react';
import { usePathname } from 'expo-router';

import { setCurrentRoute, userActionTracker } from '@/utils/tracking';

/**
 * 页面访问埋点 hook(delivery-app / RN 端)
 *
 * 在根布局(RootNavigator)内调用一次:
 *  - 路由变化时更新 tracker 当前路由(setCurrentRoute);
 *  - 上报 PAGE_VIEW(同路由 500ms 去重由 SDK 处理)。
 */
export function usePageViewTracking(): void {
  const pathname = usePathname();

  useEffect(() => {
    setCurrentRoute(pathname.startsWith('/') ? pathname : `/${pathname}`);
    userActionTracker.trackPageView();
  }, [pathname]);
}
