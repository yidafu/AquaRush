/**
 * 天地图(Tianditu)WMTS 底图配置 —— delivery-app 与 admin-client 共用
 *
 * 收敛来源:
 * - delivery-app src/utils/tianditu-style.ts
 * - admin-client src/lib/map/tianditu.ts(两实现逐字近似,仅 env 名/attribution 差异)
 *
 * Key 由调用方注入(各端读各自环境变量:EXPO_PUBLIC_TIANDITU_KEY / VITE_TIANDITU_KEY),
 * 本模块不含任何平台/环境依赖。样式对象与 maplibre-gl / @maplibre/maplibre-react-native
 * 的 StyleSpecification 结构兼容,调用方按需断言。
 */

/** 天地图瓦片子域(t0–t7;maplibre 不支持 {s} 时显式枚举,保证可用) */
export const TIANDITU_SUBDOMAINS = ['t0', 't1', 't2', 't3', 't4', 't5', 't6', 't7'] as const

export interface TiandituRasterSource {
  type: 'raster'
  tiles: string[]
  tileSize: number
  minzoom: number
  maxzoom: number
  attribution?: string
}

export interface TiandituMapStyle {
  version: 8
  sources: Record<string, TiandituRasterSource>
  layers: Array<{ id: string; type: 'raster'; source: string }>
}

/**
 * 生成某图层的 8 个子域瓦片 URL 列表(WMTS REST,Web Mercator,WGS-84/CGCS2000):
 * https://t{s}.tianditu.gov.cn/{layer}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0
 *   &LAYER={layer}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles
 *   &TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk={key}
 */
export function buildTiandituTileUrls(layer: string, key: string): string[] {
  return TIANDITU_SUBDOMAINS.map(
    (s) =>
      `https://${s}.tianditu.gov.cn/${layer}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${layer}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${key}`
  )
}

/** 天地图矢量底图 + 注记的 MapLibre 样式(Key 无效时瓦片 404) */
export function buildTiandituStyle(
  key: string,
  options: { attribution?: string } = {}
): TiandituMapStyle {
  const { attribution } = options
  return {
    version: 8,
    sources: {
      'tianditu-vec': {
        type: 'raster',
        tiles: buildTiandituTileUrls('vec', key),
        tileSize: 256,
        minzoom: 3,
        maxzoom: 18,
        ...(attribution ? { attribution } : {}),
      },
      'tianditu-cva': {
        type: 'raster',
        tiles: buildTiandituTileUrls('cva', key),
        tileSize: 256,
        minzoom: 3,
        maxzoom: 18,
      },
    },
    layers: [
      { id: 'tianditu-vec-layer', type: 'raster', source: 'tianditu-vec' },
      { id: 'tianditu-cva-layer', type: 'raster', source: 'tianditu-cva' },
    ],
  }
}
