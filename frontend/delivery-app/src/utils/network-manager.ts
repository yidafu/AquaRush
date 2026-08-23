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

  uploadFile: async ({ url, filePath, name, formData, header = {}, timeout = 30000 }) => {
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
      const filename = typeof rawFile === 'object' && rawFile !== null ? rawFile.name ?? name : name;
      if (typeof part === 'string') {
        body.append(name, part);
      } else {
        body.append(name, part, filename);
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
