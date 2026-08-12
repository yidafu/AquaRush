import { API_BASE_URL } from '@/constants/api';

/**
 * The backend returns absolute file URLs built from its own base
 * (`http://localhost:9090/api/v1/storage/files/...`). On Android emulators and
 * physical devices `localhost` points at the wrong host, so rewrite it to the
 * base URL this app is actually using.
 *
 * Note: do NOT use `@aquarush/common`'s `buildUrl` — it hardcodes 127.0.0.1:8080.
 */
export function resolveAssetUrl(url?: string | null): string | undefined {
  if (!url) return undefined;
  if (url.startsWith('/')) return `${API_BASE_URL}${url}`;
  try {
    const parsed = new URL(url);
    if (parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1') {
      const base = new URL(API_BASE_URL);
      return `${base.origin}${parsed.pathname}${parsed.search}`;
    }
  } catch {
    // not a URL — return as-is
  }
  return url;
}
