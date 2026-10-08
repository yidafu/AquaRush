/**
 * md5 单测
 * 使用 RFC 1321 标准测试向量 + 中文/emoji(代理对)用例验证 UTF-8 编码正确性。
 */
import { describe, expect, it } from '@jest/globals'
import { md5 } from './md5'

describe('md5', () => {
  it('RFC 1321 标准测试向量', () => {
    expect(md5('')).toBe('d41d8cd98f00b204e9800998ecf8427e')
    expect(md5('a')).toBe('0cc175b9c0f1b6a831c399e269772661')
    expect(md5('abc')).toBe('900150983cd24fb0d6963f7d28e17f72')
    expect(md5('message digest')).toBe('f96b697d7cb7938d525a2f31aaf161d0')
    expect(md5('abcdefghijklmnopqrstuvwxyz')).toBe('c3fcd3d76192e4007dfb496cca67e13b')
    expect(md5('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789')).toBe(
      'd174ab98d277d9f5a5611c2c9f419d9f'
    )
    expect(
      md5('12345678901234567890123456789012345678901234567890123456789012345678901234567890')
    ).toBe('57edf4a22be3c955ac49da2e2107b67a')
  })

  it('中文等多字节 UTF-8 字符', () => {
    expect(md5('中文')).toBe('a7bac2239fcdcb3a067903d8077c4a07')
    expect(md5('hello 世界')).toBe('1aaa8e8010645fe4e3d44ad9745bb94e')
  })

  it('emoji 代理对(4 字节 UTF-8)', () => {
    expect(md5('😀')).toBe('2a02eac39d716a70ecf37579185927b6')
  })

  it('输出恒为 32 位小写十六进制', () => {
    expect(md5('anything')).toMatch(/^[0-9a-f]{32}$/)
  })

  it('同一输入结果确定性一致', () => {
    expect(md5('张伟')).toBe(md5('张伟'))
  })
})
