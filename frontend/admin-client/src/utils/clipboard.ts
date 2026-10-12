import { message } from '@/ui';

/** 复制文本到剪贴板(带成功/失败提示),供详情弹窗复制按钮等场景复用 */
export async function copyText(text?: string | null): Promise<void> {
  try {
    await navigator.clipboard.writeText(text ?? '');
    message.success('已复制');
  } catch {
    message.error('复制失败');
  }
}
