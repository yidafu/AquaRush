/**
 * coordinate 坐标转换工具单测
 * 参考样例来自 eviltransform 经典用例(上海,GCJ-02 偏移 ~500m)。
 */
import { describe, expect, it } from '@jest/globals'
import { gcj02ToWgs84, isOutOfChina, wgs84ToGcj02 } from './coordinate'

// eviltransform 经典对照:wgs(31.1774276, 121.5272106) → gcj(31.17530398364597, 121.531541859215)
const WGS_LNG = 121.5272106
const WGS_LAT = 31.1774276
const GCJ_LNG = 121.531541859215
const GCJ_LAT = 31.17530398364597

describe('wgs84ToGcj02', () => {
  it('上海样例:WGS-84 转 GCJ-02 与参考值一致(容差 1e-4)', () => {
    const { longitude, latitude } = wgs84ToGcj02(WGS_LNG, WGS_LAT)
    expect(longitude).toBeCloseTo(GCJ_LNG, 4)
    expect(latitude).toBeCloseTo(GCJ_LAT, 4)
  })

  it('境外坐标原样返回(柏林)', () => {
    expect(wgs84ToGcj02(13.405, 52.52)).toEqual({ longitude: 13.405, latitude: 52.52 })
  })
})

describe('gcj02ToWgs84', () => {
  it('上海样例:GCJ-02 转 WGS-84 与参考值一致(容差 1e-4)', () => {
    const { longitude, latitude } = gcj02ToWgs84(GCJ_LNG, GCJ_LAT)
    expect(longitude).toBeCloseTo(WGS_LNG, 4)
    expect(latitude).toBeCloseTo(WGS_LAT, 4)
  })

  it('境外坐标原样返回(纽约)', () => {
    expect(gcj02ToWgs84(-74.006, 40.7128)).toEqual({ longitude: -74.006, latitude: 40.7128 })
  })
})

describe('往返一致性', () => {
  // 单步逆变换(直接减去偏移量)残差约 0.5–2m(≈1e-5~2e-5 度),容差取 1e-4(≈11m):
  // 目的仅为排除 GCJ-02 的 ~500m 级偏移,亚米级精度由 GCJ-02 偏移本身决定。
  it('wgs84 → gcj02 → wgs84 回到原点(容差 1e-4)', () => {
    const gcj = wgs84ToGcj02(WGS_LNG, WGS_LAT)
    const back = gcj02ToWgs84(gcj.longitude, gcj.latitude)
    expect(back.longitude).toBeCloseTo(WGS_LNG, 4)
    expect(back.latitude).toBeCloseTo(WGS_LAT, 4)
  })

  it('北京天安门往返一致', () => {
    const gcj = wgs84ToGcj02(116.3912757, 39.906217)
    const back = gcj02ToWgs84(gcj.longitude, gcj.latitude)
    expect(back.longitude).toBeCloseTo(116.3912757, 4)
    expect(back.latitude).toBeCloseTo(39.906217, 4)
  })
})

describe('isOutOfChina', () => {
  it('中国境内返回 false', () => {
    expect(isOutOfChina(116.4074, 39.9042)).toBe(false)
    expect(isOutOfChina(121.5272106, 31.1774276)).toBe(false)
  })

  it('境外返回 true', () => {
    expect(isOutOfChina(13.405, 52.52)).toBe(true)
    expect(isOutOfChina(-122.4194, 37.7749)).toBe(true)
  })
})
