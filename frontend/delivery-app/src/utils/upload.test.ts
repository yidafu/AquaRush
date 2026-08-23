/**
 * uploadPhoto 回归测试:
 * 曾把 multipart 部件构造成 RN 的 `File`,而 expo/fetch 的 FormData 补丁会写部件的
 * `name` 属性,RN File 的 `name` 是原型上只读 getter,导致
 *   TypeError: Cannot assign to property 'name' which has only a getter
 * 修复后部件必须是 Blob,文件名经 `filename` 显式传递。
 */
import { uploadPhoto } from '@/utils/upload';
import { networkManager } from '@/utils/network-manager';

jest.mock('@/utils/network-manager', () => ({
  networkManager: { uploadFile: jest.fn() },
}));

const mockedUploadFile = networkManager.uploadFile as jest.Mock;

describe('uploadPhoto', () => {
  const fakeBlob = { type: 'image/jpeg' };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedUploadFile.mockResolvedValue({
      success: true,
      data: { id: 1, fileUrl: 'http://localhost:9090/api/v1/storage/files/1' },
    });
    globalThis.fetch = jest.fn(
      async () => ({ blob: async () => fakeBlob }) as unknown as Response,
    ) as unknown as typeof fetch;
  });

  it('以 Blob(而非 RN File)作为 multipart 部件,并显式携带文件名', async () => {
    const url = await uploadPhoto('file:///tmp/photo.jpg', 'photo.jpg', 'image/jpeg');

    expect(url).toBe('http://localhost:9090/api/v1/storage/files/1');
    expect(mockedUploadFile).toHaveBeenCalledTimes(1);

    const options = mockedUploadFile.mock.calls[0][0];
    expect(options.name).toBe('file');
    expect(options.filename).toBe('photo.jpg');
    expect(options.filePath).toBe(fakeBlob);

    // 回归防护:部件不能是 File —— expo/fetch 的 FormData 补丁会写部件的 name 属性,
    // RN File 的 name 是只读 getter,append 会抛 "Cannot assign to property 'name' which has only a getter"。
    expect((options.filePath as { constructor?: { name?: string } }).constructor?.name).not.toBe('File');
  });

  it('blob 类型与 mime 不一致时用 mime 重建 Blob(保证 multipart Content-Type)', async () => {
    globalThis.fetch = jest.fn(
      async () => ({ blob: async () => ({ type: '' }) }) as unknown as Response,
    ) as unknown as typeof fetch;

    await uploadPhoto('file:///tmp/photo.jpg', 'photo.jpg', 'image/jpeg');

    const options = mockedUploadFile.mock.calls[0][0];
    expect(options.filePath).not.toBeNull();
    expect(options.filename).toBe('photo.jpg');
  });

  it('上传失败(非 success 或无 fileUrl)时抛出错误', async () => {
    mockedUploadFile.mockResolvedValue({ success: false, data: null, message: '文件过大' });

    await expect(uploadPhoto('file:///tmp/photo.jpg', 'photo.jpg', 'image/jpeg')).rejects.toThrow(
      '文件过大',
    );
  });
});
