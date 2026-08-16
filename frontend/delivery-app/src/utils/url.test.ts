/**
 * resolveAssetUrl 单测(纯函数,依赖 @/constants/api 的 API_BASE_URL)
 * jest-expo 默认 iOS 平台环境,API_BASE_URL 回退为 http://localhost:9090。
 */
import { resolveAssetUrl } from '@/utils/url';

describe('resolveAssetUrl', () => {
  it('空值返回 undefined', () => {
    expect(resolveAssetUrl()).toBeUndefined();
    expect(resolveAssetUrl(null)).toBeUndefined();
    expect(resolveAssetUrl('')).toBeUndefined();
  });

  it('相对路径拼接 API_BASE_URL', () => {
    expect(resolveAssetUrl('/files/1.png')).toBe('http://localhost:9090/files/1.png');
  });

  it('localhost 绝对 URL 重写为 API_BASE_URL 的源', () => {
    expect(resolveAssetUrl('http://localhost:9090/api/v1/storage/files/1.png')).toBe(
      'http://localhost:9090/api/v1/storage/files/1.png',
    );
    // 端口不同也以 API_BASE_URL 为准
    expect(resolveAssetUrl('http://localhost:9999/a.png?x=1')).toBe(
      'http://localhost:9090/a.png?x=1',
    );
  });

  it('127.0.0.1 绝对 URL 同样重写(模拟器场景)', () => {
    expect(resolveAssetUrl('http://127.0.0.1:9090/api/v1/storage/files/1.png')).toBe(
      'http://localhost:9090/api/v1/storage/files/1.png',
    );
  });

  it('其他绝对 URL 原样返回', () => {
    expect(resolveAssetUrl('https://cdn.example.com/a.png')).toBe('https://cdn.example.com/a.png');
  });

  it('非法 URL 原样返回', () => {
    expect(resolveAssetUrl('not a url')).toBe('not a url');
  });
});
