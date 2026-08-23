import { UPLOAD_URL } from '@/constants/api';
import { networkManager } from '@/utils/network-manager';

interface FileMetadataResponse {
  id: number;
  fileUrl?: string | null;
}

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string | null;
}

async function toBlob(uri: string, mime: string): Promise<Blob> {
  // file:/blob:/data: URI -> Blob(web 走浏览器 fetch;native 走 expo/fetch,
  // 原生读取 file://,安装 expo-blob 后直接返回原生 Blob,避免 base64 往返)。
  const blob = await (await fetch(uri)).blob();
  // Response.blob() 的 type 来自响应头,file:// 场景可能为空;按 ImagePicker 提供的
  // mime 重建 Blob,保证 multipart 部件携带正确的 Content-Type(类型一致时零拷贝)。
  return blob.type === mime ? blob : new Blob([blob], { type: mime });
}

/**
 * Upload a delivery photo to `POST /api/v1/storage/files` (multipart `file`).
 * Auth header is injected by NetworkManager. Returns the resolved file URL.
 *
 * Expo SDK 53+ runs `expo/fetch` on native, which rejects React Native's legacy
 * `{ uri, name, type }` FormData parts ("Unsupported FormDataPart implementation") —
 * always pass a real `Blob`, on every platform.
 *
 * 注意:部件必须传 `Blob` 而非 RN 的 `File`。expo/fetch 的 FormData 补丁会写部件的
 * `name` 属性,而 RN File 的 `name` 是原型上只读 getter,传 File 会抛
 * "Cannot assign to property 'name' which has only a getter"。
 * 文件名通过 `filename` 显式传给 multipart Content-Disposition。
 */
export async function uploadPhoto(uri: string, name: string, mime: string): Promise<string> {
  const blob = await toBlob(uri, mime);

  const res = await networkManager.uploadFile<ApiResponse<FileMetadataResponse>>({
    url: UPLOAD_URL,
    filePath: blob,
    name: 'file',
    filename: name,
    headers: {}, // no Content-Type -> fetch sets the multipart boundary
  });

  if (!res?.success || !res.data?.fileUrl) {
    throw new Error(res?.message ?? '图片上传失败');
  }
  return res.data.fileUrl;
}
