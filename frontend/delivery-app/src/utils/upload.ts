import { Platform } from 'react-native';

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

async function toFile(uri: string, name: string, mime: string): Promise<File> {
  const blob = await (await fetch(uri)).blob(); // data:/blob: URI -> Blob (web)
  return new File([blob], name, { type: mime });
}

/**
 * Upload a delivery photo to `POST /api/v1/storage/files` (multipart `file`).
 * Auth header is injected by NetworkManager. Returns the resolved file URL.
 */
export async function uploadPhoto(uri: string, name: string, mime: string): Promise<string> {
  const file: any =
    Platform.OS === 'web'
      ? await toFile(uri, name, mime)
      : { uri, name, type: mime };

  const res = await networkManager.uploadFile<ApiResponse<FileMetadataResponse>>({
    url: UPLOAD_URL,
    filePath: file,
    name: 'file',
    headers: {}, // no Content-Type -> fetch sets the multipart boundary
  });

  if (!res?.success || !res.data?.fileUrl) {
    throw new Error(res?.message ?? '图片上传失败');
  }
  return res.data.fileUrl;
}
