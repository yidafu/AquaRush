import { StyleSheet, Text, View } from 'react-native';
import {
  Camera,
  Layer,
  Map,
  Marker,
  RasterSource,
  UserLocation,
} from '@maplibre/maplibre-react-native';
import { Radius } from '@/constants/theme';
import {
  TIANDITU_KEY,
  buildTiandituStyle,
  buildTiandituTileUrls,
} from '@/utils/tianditu-style';

interface DestinationMapProps {
  /** 收货地址经度(存储/接口统一 WGS-84,与天地图底图一致,直接上图) */
  longitude?: number | null;
  /** 收货地址纬度(存储/接口统一 WGS-84) */
  latitude?: number | null;
  height?: number;
}

/**
 * 订单配送地图(F-ADR-012 扩展):MapLibre + 天地图底图,
 * 展示收货地址标记与送水工当前位置(GPS WGS-84,与底图一致)。
 * 仅原生端使用;web 端由 destination-map.web.tsx 提供桩实现。
 */
export function DestinationMap({
  longitude,
  latitude,
  height = 200,
}: DestinationMapProps) {
  if (!TIANDITU_KEY) {
    return (
      <View style={[styles.placeholder, { height }]}>
        <Text style={styles.placeholderText}>未配置 EXPO_PUBLIC_TIANDITU_KEY，地图不可用</Text>
      </View>
    );
  }

  if (longitude == null || latitude == null) {
    return (
      <View style={[styles.placeholder, { height }]}>
        <Text style={styles.placeholderText}>该地址暂无坐标，无法上图</Text>
      </View>
    );
  }

  // 地址坐标为 WGS-84(后端坐标体系统一),天地图底图同为 WGS-84,直接展示
  return (
    <View style={[styles.container, { height }]}>
      <Map style={styles.map} mapStyle={buildTiandituStyle()}>
        <RasterSource
          id="tianditu-vec"
          tiles={buildTiandituTileUrls('vec')}
          tileSize={256}
          minzoom={3}
          maxzoom={18}
          attribution="天地图">
          <Layer id="tianditu-vec-layer" type="raster" />
        </RasterSource>
        <RasterSource
          id="tianditu-cva"
          tiles={buildTiandituTileUrls('cva')}
          tileSize={256}
          minzoom={3}
          maxzoom={18}>
          <Layer id="tianditu-cva-layer" type="raster" />
        </RasterSource>
        <Camera center={[longitude, latitude]} zoom={15} duration={0} />
        <Marker id="destination" lngLat={[longitude, latitude]} anchor="bottom">
          <View style={styles.pin}>
            <View style={styles.pinInner} />
          </View>
        </Marker>
        {/* 送水工当前位置(GPS WGS-84,与底图一致,直接展示) */}
        <UserLocation animated accuracy />
      </Map>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    width: '100%',
    overflow: 'hidden',
    borderRadius: Radius.md,
  },
  map: {
    flex: 1,
  },
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
  pin: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#ffffff',
    borderWidth: 4,
    borderColor: '#F5222D',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000000',
    shadowOpacity: 0.3,
    shadowRadius: 3,
    shadowOffset: { width: 0, height: 1 },
    elevation: 3,
  },
  pinInner: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#F5222D',
  },
});
