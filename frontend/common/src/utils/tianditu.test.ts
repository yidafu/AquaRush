/**
 * tianditu 单测:瓦片 URL 模板、key 注入、style 结构与图层。
 */
import { describe, expect, it } from '@jest/globals'
import { TIANDITU_SUBDOMAINS, buildTiandituStyle, buildTiandituTileUrls } from './tianditu'

describe('buildTiandituTileUrls', () => {
  it('生成 8 个子域 URL,模板占位与 tk 注入正确', () => {
    const urls = buildTiandituTileUrls('vec', 'MY_KEY')
    expect(urls).toHaveLength(8)
    expect(TIANDITU_SUBDOMAINS).toHaveLength(8)
    urls.forEach((url, i) => {
      expect(url).toContain(`https://t${i}.tianditu.gov.cn/vec_w/wmts`)
      expect(url).toContain('LAYER=vec')
      expect(url).toContain('TILEMATRIX={z}&TILEROW={y}&TILECOL={x}')
      expect(url).toContain('tk=MY_KEY')
    })
  })

  it('key 为空时仍生成模板(tk= 空)', () => {
    const [url] = buildTiandituTileUrls('cva', '')
    expect(url).toContain('tk=')
  })
})

describe('buildTiandituStyle', () => {
  it('矢量 + 注记双源双图层', () => {
    const style = buildTiandituStyle('K')
    expect(style.version).toBe(8)
    expect(Object.keys(style.sources)).toEqual(['tianditu-vec', 'tianditu-cva'])
    expect(style.sources['tianditu-vec'].tiles[0]).toContain('LAYER=vec')
    expect(style.sources['tianditu-cva'].tiles[0]).toContain('LAYER=cva')
    expect(style.layers.map((l) => l.id)).toEqual(['tianditu-vec-layer', 'tianditu-cva-layer'])
  })

  it('attribution 可选注入', () => {
    const style = buildTiandituStyle('K', { attribution: '天地图' })
    expect(style.sources['tianditu-vec'].attribution).toBe('天地图')
    expect(style.sources['tianditu-cva'].attribution).toBeUndefined()
  })
})
