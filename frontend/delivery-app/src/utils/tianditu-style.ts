/**
 * 天地图(Tianditu)底图配置(配送端 App)
 *
 * 与管理后台(frontend/admin-client/src/lib/map/tianditu.ts)保持同一套
 * WMTS REST 瓦片模板;Key 通过 EXPO_PUBLIC_TIANDITU_KEY 构建期内联。
 */
import type { StyleSpecification } from '@maplibre/maplibre-react-native';

/** 天地图 API Key(Expo 环境变量) */
export const TIANDITU_KEY: string | undefined = process.env.EXPO_PUBLIC_TIANDITU_KEY;

/** 天地图瓦片子域(t0–t7) */
const TIANDITU_SUBDOMAINS = ['t0', 't1', 't2', 't3', 't4', 't5', 't6', 't7'] as const;

/** 生成某图层的 8 个子域瓦片 URL 列表 */
export function buildTiandituTileUrls(layer: string): string[] {
  const key = TIANDITU_KEY ?? '';
  return TIANDITU_SUBDOMAINS.map(
    (s) =>
      `https://${s}.tianditu.gov.cn/${layer}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${layer}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${key}`,
  );
}

/** 天地图矢量底图 + 注记的 MapLibre 样式(Key 无效时瓦片 404) */
export function buildTiandituStyle(): StyleSpecification {
  return {
    version: 8,
    sources: {
      'tianditu-vec': {
        type: 'raster',
        tiles: buildTiandituTileUrls('vec'),
        tileSize: 256,
        minzoom: 3,
        maxzoom: 18,
      },
      'tianditu-cva': {
        type: 'raster',
        tiles: buildTiandituTileUrls('cva'),
        tileSize: 256,
        minzoom: 3,
        maxzoom: 18,
      },
    },
    layers: [
      { id: 'tianditu-vec-layer', type: 'raster', source: 'tianditu-vec' },
      { id: 'tianditu-cva-layer', type: 'raster', source: 'tianditu-cva' },
    ],
  };
}
