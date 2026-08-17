import { StyleSheet, View } from 'react-native';
import { ThemedText } from '@/components/themed-text';
import { Radius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface DestinationMapProps {
  longitude?: number | null;
  latitude?: number | null;
  height?: number;
}

/**
 * Web 端桩实现:@maplibre/maplibre-react-native 仅支持原生(iOS/Android),
 * web 端渲染地址摘要占位(平台桩模式,与已移除的 trajectory-sdk.web.ts 一致)。
 */
export function DestinationMap({
  longitude,
  latitude,
  height = 200,
}: DestinationMapProps) {
  const theme = useTheme();
  const hasCoords = longitude != null && latitude != null;
  return (
    <View style={[styles.placeholder, { height, backgroundColor: theme.pageBackground }]}>
      <ThemedText type="caption" themeColor="textTertiary">
        {hasCoords ? `配送坐标:${latitude!.toFixed(6)}, ${longitude!.toFixed(6)}(地图仅 App 端展示)` : '该地址暂无坐标'}
      </ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  placeholder: {
    width: '100%',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: Radius.md,
  },
});
