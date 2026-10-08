/**
 * 同步 MD5 实现(纯 JS、零依赖),供 GitHub 风格默认头像等做确定性哈希使用;
 * 不用于任何安全/密码学用途。输入按 UTF-8 编码(RFC 1321 标准结果)。
 */

/** UTF-8 编码为字节序列(支持中文等多字节字符与代理对) */
function utf8Encode(input: string): number[] {
  const bytes: number[] = [];
  for (let i = 0; i < input.length; i++) {
    let c = input.charCodeAt(i);
    if (c < 0x80) {
      bytes.push(c);
    } else if (c < 0x800) {
      bytes.push(0xc0 | (c >> 6), 0x80 | (c & 0x3f));
    } else if (c < 0xd800 || c >= 0xe000) {
      bytes.push(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 0x3f), 0x80 | (c & 0x3f));
    } else {
      i++;
      const cp = 0x10000 + (((c & 0x3ff) << 10) | (input.charCodeAt(i) & 0x3ff));
      bytes.push(
        0xf0 | (cp >> 18),
        0x80 | ((cp >> 12) & 0x3f),
        0x80 | ((cp >> 6) & 0x3f),
        0x80 | (cp & 0x3f),
      );
    }
  }
  return bytes;
}

/** 64 字节消息块按小端序打包为 16 个 32 位字 */
function packWords(bytes: number[], offset: number): number[] {
  const words: number[] = new Array(16);
  for (let i = 0; i < 16; i++) {
    const j = offset + i * 4;
    words[i] =
      (bytes[j] & 0xff) |
      ((bytes[j + 1] & 0xff) << 8) |
      ((bytes[j + 2] & 0xff) << 16) |
      ((bytes[j + 3] & 0xff) << 24);
  }
  return words;
}

const SHIFTS = [
  7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
  5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
  4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
  6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
];

const CONSTANTS = [
  0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee, 0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
  0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be, 0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
  0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa, 0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
  0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed, 0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
  0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c, 0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
  0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05, 0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
  0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039, 0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
  0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1, 0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391,
];

function rotl(x: number, c: number): number {
  return (x << c) | (x >>> (32 - c));
}

/** 对单个 64 字节消息块做一轮 MD5 变换,就地更新 state */
function md5Transform(state: number[], block: number[]): void {
  let a = state[0];
  let b = state[1];
  let c = state[2];
  let d = state[3];
  for (let i = 0; i < 64; i++) {
    let f: number;
    let g: number;
    if (i < 16) {
      f = (b & c) | (~b & d);
      g = i;
    } else if (i < 32) {
      f = (d & b) | (~d & c);
      g = (5 * i + 1) % 16;
    } else if (i < 48) {
      f = b ^ c ^ d;
      g = (3 * i + 5) % 16;
    } else {
      f = c ^ (b | ~d);
      g = (7 * i) % 16;
    }
    f = (f + a + CONSTANTS[i] + block[g]) | 0;
    a = d;
    d = c;
    c = b;
    b = (b + rotl(f, SHIFTS[i])) | 0;
  }
  state[0] = (state[0] + a) | 0;
  state[1] = (state[1] + b) | 0;
  state[2] = (state[2] + c) | 0;
  state[3] = (state[3] + d) | 0;
}

/**
 * 计算输入字符串的 MD5(返回 32 位小写十六进制)。
 * 已知用例:md5('abc') === '900150983cd24fb0d6963f7d28e17f72'
 */
export function md5(input: string): string {
  const bytes = utf8Encode(input);
  const bitLen = bytes.length * 8;

  // RFC 1321 填充:追加 0x80 + 补零到 56 mod 64,再附 8 字节小端序长度(比特数)
  const padded = bytes.slice();
  padded.push(0x80);
  while (padded.length % 64 !== 56) padded.push(0);
  for (let i = 0; i < 4; i++) padded.push((bitLen >>> (i * 8)) & 0xff);
  for (let i = 0; i < 4; i++) padded.push(0); // 高位 32 位(输入长度远小于 2^32)

  const state = [0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476];
  for (let off = 0; off < padded.length; off += 64) {
    md5Transform(state, packWords(padded, off));
  }

  // MD5 摘要按小端序字节输出每个状态字
  let hex = '';
  for (const word of state) {
    for (let j = 0; j < 4; j++) {
      hex += ((word >>> (j * 8)) & 0xff).toString(16).padStart(2, '0');
    }
  }
  return hex;
}
