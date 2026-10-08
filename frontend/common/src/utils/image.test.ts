/**
 * image 工具单测:resolveAssetUrl 与 GitHub identicon 风格默认头像生成
 */
import { describe, expect, it } from '@jest/globals'
import { avatarFromName, resolveAssetUrl } from './image'

describe('resolveAssetUrl', () => {
  it('以 / 开头的相对路径拼接 baseUrl', () => {
    expect(resolveAssetUrl('/files/1.png', 'http://localhost:9090')).toBe(
      'http://localhost:9090/files/1.png'
    )
  })

  it('localhost/127.0.0.1 主机重写到 baseUrl 的 origin', () => {
    expect(resolveAssetUrl('http://localhost:9090/api/v1/storage/files/1.png', 'http://10.0.2.2:9090')).toBe(
      'http://10.0.2.2:9090/api/v1/storage/files/1.png'
    )
    expect(resolveAssetUrl('http://127.0.0.1:8080/a.png?x=1', 'https://api.example.com/graphql')).toBe(
      'https://api.example.com/a.png?x=1'
    )
  })

  it('完整 URL(非 localhost)原样返回', () => {
    expect(resolveAssetUrl('https://cdn.example.com/a.png', 'http://localhost:9090')).toBe(
      'https://cdn.example.com/a.png'
    )
  })

  it('null / undefined / 空串返回 undefined', () => {
    expect(resolveAssetUrl(null, 'http://localhost:9090')).toBeUndefined()
    expect(resolveAssetUrl(undefined, 'http://localhost:9090')).toBeUndefined()
    expect(resolveAssetUrl('', 'http://localhost:9090')).toBeUndefined()
  })

  it('非法 URL 原样返回', () => {
    expect(resolveAssetUrl('not a url', 'http://localhost:9090')).toBe('not a url')
  })
})

describe('avatarFromName', () => {
  const decode = (dataUri: string): string =>
    decodeURIComponent(dataUri.replace(/^data:image\/svg\+xml;charset=utf-8,/, ''))

  it('返回自包含 SVG data URI', () => {
    const avatar = avatarFromName('张三')
    expect(avatar.startsWith('data:image/svg+xml;charset=utf-8,')).toBe(true)
    const svg = decode(avatar)
    expect(svg).toContain('<svg')
    expect(svg).toContain('shape-rendering="crispEdges"')
    // 含背景色矩形(5x5 网格底)
    expect(svg).toContain('<rect width="100" height="100"')
  })

  it('同一名字生成的头像确定性一致', () => {
    expect(avatarFromName('张三')).toBe(avatarFromName('张三'))
  })

  it('null / undefined / 空串均得到同一兜底头像', () => {
    const a = avatarFromName(null)
    const b = avatarFromName(undefined)
    const c = avatarFromName('')
    expect(a).toBe(b)
    expect(b).toBe(c)
  })

  it('不同名字生成不同头像', () => {
    expect(avatarFromName('张三')).not.toBe(avatarFromName('李四'))
  })

  it('头像含前景色块(identicon 非空)', () => {
    const svg = decode(avatarFromName('王小明'))
    // 至少出现一个前景色 rect(fill 非背景浅色)
    expect(svg).toMatch(/<rect [^>]*fill="#[0-9a-f]{6}"\/>/)
  })
})
