/**
 * 天地图(Tianditu)底图配置
 *
 * 天地图 WMTS REST 瓦片地址(Web Mercator, WGS-84/CGCS2000):
 *   https://t{s}.tianditu.gov.cn/{layer}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0
 *     &LAYER={layer}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles
 *     &TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk={key}
 *
 * layer:vec(矢量底图)+ cva(矢量注记)叠两层;img/cia 为影像底图(未启用,可扩展)。
 * Key 需在 https://console.tianditu.gov.cn/ 注册,生产域名需加入 referer 白名单。
 */
import type { StyleSpecification } from 'maplibre-gl';

/** 天地图 API Key(通过 Vite 环境变量注入) */
export const TIANDITU_KEY: string | undefined = import.meta.env
  .VITE_TIANDITU_KEY as string | undefined;

/** 默认地图中心(北京,兜底位置) */
export const DEFAULT_MAP_CENTER = { lng: 116.4074, lat: 39.9042 } as const;

/** 天地图瓦片子域(t0–t7) */
const TIANDITU_SUBDOMAINS = ['t0', 't1', 't2', 't3', 't4', 't5', 't6', 't7'] as const;

/** 生成某图层的 8 个子域瓦片 URL 列表(maplibre 不支持 {s} 时显式枚举,保证可用) */
function buildTileUrls(layer: string, key: string): string[] {
  return TIANDITU_SUBDOMAINS.map(
    (s) =>
      `https://${s}.tianditu.gov.cn/${layer}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${layer}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${key}`,
  );
}

/** 天地图矢量底图 + 注记的 MapLibre 样式(需有效 Key,否则瓦片 404) */
export function getTiandituStyle(): StyleSpecification {
  const key = TIANDITU_KEY ?? '';
  return {
    version: 8,
    sources: {
      'tianditu-vec': {
        type: 'raster',
        tiles: buildTileUrls('vec', key),
        tileSize: 256,
        minzoom: 3,
        maxzoom: 18,
        attribution: '天地图',
      },
      'tianditu-cva': {
        type: 'raster',
        tiles: buildTileUrls('cva', key),
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
