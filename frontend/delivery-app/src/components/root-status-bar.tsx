import { StatusBar } from 'expo-status-bar';

import { useColorScheme } from '@/hooks/use-color-scheme';

/** 全局状态栏：跟随亮暗主题。渐变 hero 屏在内部再用 <StatusBar style="light"/> 覆盖。 */
export function RootStatusBar() {
  const scheme = useColorScheme();
  return <StatusBar style={scheme === 'dark' ? 'light' : 'dark'} />;
}
