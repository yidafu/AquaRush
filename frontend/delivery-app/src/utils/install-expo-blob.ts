/**
 * 将 expo-blob 的规范 Blob 安装为 globalThis.Blob。
 *
 * 背景:Expo SDK 56+ 在 native 上默认把 expo/fetch 装为全局 fetch,其
 * `Response.blob()` 依赖全局 Blob;若全局仍是 React Native 自带 Blob,会走
 * 「写 native blob store + base64 读回」的慢路径并打印告警:
 *   "Response.blob() is using React Native's Blob ... Add the `expo-blob` package"
 * expo-blob 提供规范 Blob(native 直接持有二进制,arrayBuffer/bytes 零拷贝),
 * 但 SDK 57 的 winter runtime 不会自动替换全局 Blob,需要应用启动时安装一次
 * (参见 expo/expo#47468)。
 *
 * 仅替换 Blob,不替换 File:RN 的 File 仍基于 RN Blob,而我们上传统一传 Blob
 * (RN File 的 `name` 是只读 getter,expo/fetch 写部件 name 时会抛错)。
 */
import { Blob as ExpoBlob } from 'expo-blob';

if (typeof globalThis !== 'undefined' && globalThis.Blob !== ExpoBlob) {
  globalThis.Blob = ExpoBlob as unknown as typeof Blob;
}
