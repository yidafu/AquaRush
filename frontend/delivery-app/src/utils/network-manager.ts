import { NetworkManager, RequestHandler } from '@aquarush/common';

import { GRAPHQL_URL } from '@/constants/api';
import { storage } from '@/services/storage';

/** Invoked when the token is cleared (session expired / 401). AuthProvider registers it. */
let sessionExpiredHandler: (() => void) | null = null;
export const registerSessionExpiredHandler = (fn: (() => void) | null) => {
  sessionExpiredHandler = fn;
};

/**
 * fetch-based adapter for @aquarush/common NetworkManager.
 * Keeps `config.headers` empty so multipart uploads get the correct boundary.
 */
const fetchRequestHandler: RequestHandler = {
  request: async ({ url, method = 'GET', header = {}, data, timeout = 10000 }) => {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeout);
    try {
      const isJson = /application\/json/i.test(header['Content-Type'] ?? '');
      const body =
        data !== undefined
          ? isJson && typeof data === 'object'
            ? JSON.stringify(data)
            : (data as BodyInit)
          : undefined;

      const res = await fetch(url, {
        method,
        headers: header as Record<string, string>,
        body,
        signal: controller.signal,
      });

      const json = await res.json().catch(() => null);
      return { statusCode: res.status, data: json };
    } finally {
      clearTimeout(timer);
    }
  },

  uploadFile: async ({ url, filePath, name, filename, formData, header = {}, timeout = 30000 }) => {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeout);
    try {
      const body = new FormData();
      // Expo SDK 53+ replaces RN's fetch with expo/fetch, which only accepts
      // string/Blob/File FormData parts. React Native's legacy `{ uri, name, type }`
      // part is rejected with "Unsupported FormDataPart implementation", so convert
      // local file URIs to a Blob first (fetch() reads file:// natively).
      const rawFile = filePath;
      const part =
        typeof rawFile === 'object' && rawFile !== null && !(rawFile instanceof Blob)
          ? await (await fetch(rawFile.uri, { signal: controller.signal })).blob()
          : rawFile;
      // 关键:FormData 部件必须传 Blob 而非 RN 的 File。expo/fetch 的 FormData 补丁
      // (expo/src/winter/FormData.ts normalizeArgs)会写部件的 `name` 属性,而 RN File
      // 把 `name` 定义为原型上只读 getter,导致
      //   TypeError: Cannot assign to property 'name' which has only a getter
      // 统一传 Blob + 显式 filename(Blob 无 name 属性,不会被 getter 拦截)。
      const finalFilename =
        filename ??
        (typeof rawFile === 'object' && rawFile !== null && 'name' in rawFile ? rawFile.name : undefined) ??
        name;
      if (typeof part === 'string') {
        body.append(name, part);
      } else {
        // expo/fetch 从部件的 `name` 生成 Content-Disposition filename:RN Blob 与
        // expo-blob Blob 都没有该属性,先写 own 属性保证任意 Blob 实现都带上文件名;
        // append 的第三个参数作为兜底。部分实现(如浏览器原生 Blob)拒绝写属性,忽略即可。
        if (Object.isExtensible(part)) {
          try {
            // TS 视角下 Blob 没有 name 属性、File 的 name 只读,但这里是在给可扩展对象
            // 增加 own 属性(运行时行为),故显式收窄类型。
            (part as { name?: string }).name = finalFilename;
          } catch {
            // ignore:append(name, blob, filename) 会兜底
          }
        }
        body.append(name, part, finalFilename);
      }
      if (formData) {
        for (const [key, value] of Object.entries(formData)) {
          body.append(key, value);
        }
      }
      // Deliberately no Content-Type here: fetch sets the multipart boundary.
      const res = await fetch(url, {
        method: 'POST',
        headers: header as Record<string, string>,
        body,
        signal: controller.signal,
      });
      const text = await res.text();
      return { statusCode: res.status, data: text };
    } finally {
      clearTimeout(timer);
    }
  },
};

/** Initialized at module load (never inside a React effect) to avoid init-order races. */
export const networkManager = NetworkManager.getInstance(
  { baseURL: GRAPHQL_URL, timeout: 15000, headers: {} },
  {
    // getToken is sync and AsyncStorage can't be read synchronously; NetworkManager always uses getTokenAsync.
    getToken: () => null,
    getTokenAsync: async () => storage.getToken(),
    clearToken: async () => {
      await storage.clearToken();
      sessionExpiredHandler?.();
    },
    silentLogin: undefined,
  },
  fetchRequestHandler,
);
