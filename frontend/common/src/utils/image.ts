export function buildUrl(url: string) {
  if (url.startsWith('/')) {
    return 'http://127.0.0.1:8080' + url;
  }
  return url;
}
