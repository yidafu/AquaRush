import { md5 } from './md5';

export function buildUrl(url: string) {
  if (url.startsWith('/')) {
    return 'http://127.0.0.1:8080' + url;
  }
  return url;
}

/** HSL → hex(#rrggbb),供 GitHub 风格头像颜色使用 */
function hslToHex(h: number, s: number, l: number): string {
  s /= 100;
  l /= 100;
  const k = (n: number) => (n + h / 30) % 12;
  const a = s * Math.min(l, 1 - l);
  const f = (n: number) => l - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1)));
  const toHex = (v: number) => Math.round(v * 255).toString(16).padStart(2, '0');
  return `#${toHex(f(0))}${toHex(f(8))}${toHex(f(4))}`;
}

/**
 * 根据用户名/姓名生成默认头像(GitHub identicon 算法,data URI SVG,自包含、离线可用):
 * - 对名字做 MD5 确定性哈希,同一名字始终得到同一头像;
 * - 前 5 字节 → 5 行前景色,第 6 字节 → 浅色背景(HSL);
 * - 其余字节 → 5×5 网格(左右对称)的填充位;
 * 未上传头像时作兜底展示。
 */
export function avatarFromName(name?: string | null): string {
  const digest = md5(name ?? '');
  const bytes: number[] = [];
  for (let i = 0; i < digest.length; i += 2) {
    bytes.push(parseInt(digest.slice(i, i + 2), 16));
  }

  const fgColors = bytes.slice(0, 5).map((b) => hslToHex((b / 255) * 360, 65, 50));
  const bg = hslToHex((bytes[5] / 255) * 360, 35, 80);

  // 5×5 网格:仅左半(含中列)3 列由哈希位决定,右半镜像
  const bits: number[] = [];
  for (let i = 6; i < bytes.length && bits.length < 15; i++) {
    for (let bit = 7; bit >= 0 && bits.length < 15; bit--) {
      bits.push((bytes[i] >> bit) & 1);
    }
  }
  const grid: string[][] = Array.from({ length: 5 }, () => Array<string>(5).fill(bg));
  let idx = 0;
  for (let y = 0; y < 5; y++) {
    for (let x = 0; x < 3; x++) {
      if (bits[idx] === 1) {
        grid[y][x] = fgColors[y];
        grid[y][4 - x] = fgColors[y];
      }
      idx++;
    }
  }

  const cell = 20;
  const size = 5 * cell;
  let cells = '';
  for (let y = 0; y < 5; y++) {
    for (let x = 0; x < 5; x++) {
      if (grid[y][x] !== bg) {
        cells +=
          `<rect x="${x * cell}" y="${y * cell}" width="${cell}" height="${cell}" fill="${grid[y][x]}"/>`;
      }
    }
  }
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" shape-rendering="crispEdges">` +
    `<rect width="${size}" height="${size}" fill="${bg}"/>` +
    cells +
    `</svg>`;
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}
