/**
 * image 工具单测:buildUrl 与 GitHub identicon 风格默认头像生成
 */
import { describe, expect, it } from '@jest/globals'
import { avatarFromName, buildUrl } from './image'

describe('buildUrl', () => {
  it('以 / 开头的相对路径拼接后端地址', () => {
    expect(buildUrl('/static/avatar/1.png')).toBe('http://127.0.0.1:8080/static/avatar/1.png')
  })

  it('完整 URL 原样返回', () => {
    expect(buildUrl('https://cdn.example.com/a.png')).toBe('https://cdn.example.com/a.png')
    expect(buildUrl('http://localhost:9090/x')).toBe('http://localhost:9090/x')
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
