import { StyleSheet, Text, View } from 'react-native';
import { Radius } from '@/constants/theme';

interface DestinationMapProps {
  longitude?: number | null;
  latitude?: number | null;
  height?: number;
}

/**
 * Web 端桩实现:@maplibre/maplibre-react-native 仅支持原生(iOS/Android),
 * web 端渲染地址摘要占位(与 trajectory-sdk.web.ts 平台桩模式一致)。
 */
export function DestinationMap({
  longitude,
  latitude,
  height = 200,
}: DestinationMapProps) {
  const hasCoords = longitude != null && latitude != null;
  return (
    <View style={[styles.placeholder, { height }]}>
      <Text style={styles.placeholderText}>
        {hasCoords ? `配送坐标:${latitude!.toFixed(6)}, ${longitude!.toFixed(6)}(地图仅 App 端展示)` : '该地址暂无坐标'}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  placeholder: {
    width: '100%',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f5f5f5',
    borderRadius: Radius.md,
  },
  placeholderText: {
    fontSize: 13,
    color: '#999999',
  },
});
