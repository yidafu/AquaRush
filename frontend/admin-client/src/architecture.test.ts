/**
 * 前端架构约束(ArchUnitTS)。
 * 组件行数硬约束 300 行;ui/ 组件库(Radix 封装)单独放宽至 1200 行。
 */
import { describe, expect, it } from '@jest/globals';
import { projectFiles, type FileInfo } from 'archunit';

type FileViolation = { message: string; rule: string; fileInfo: FileInfo };

/** 物理行数,与 wc -l 一致:按 \n 计数,末尾换行不额外计一行。 */
const physicalLines = (content: string): number => {
  const lines = content.split('\n');
  if (lines[lines.length - 1] === '') lines.pop();
  return lines.length;
};

const lineLimit =
  (limit: number) =>
  (file: FileInfo): boolean =>
    physicalLines(file.content) <= limit;

/** 违规摘要:路径 + 实际行数(比完整 violation 对象更可读)。 */
const summarize = (violations: readonly unknown[]): string[] =>
  violations.map((v) => {
    const { fileInfo, message } = v as Partial<FileViolation>;
    return fileInfo ? `${fileInfo.path} (${physicalLines(fileInfo.content)} 行)` : String(message ?? v);
  });

/** src/components 与 src/pages:含根目录平铺与任意深度子目录。 */
const COMPONENT_DIRS = /^src\/(components|pages)(\/|$)/;
const UI_DIR = /^src\/ui(\/|$)/;

describe('admin-client 架构约束(ArchUnitTS)', () => {
  it('业务组件(components/pages 下的 .tsx)不得超过 300 行', async () => {
    const rule = projectFiles()
      .inFolder(COMPONENT_DIRS)
      .withName('*.tsx')
      .should()
      .adhereTo(lineLimit(300), '组件文件超过 300 行,请拆分为子组件');
    expect(summarize(await rule.check())).toEqual([]);
  });

  it('ui/ 组件库文件不得超过 1200 行', async () => {
    const rule = projectFiles()
      .inFolder(UI_DIR)
      .withName('*.tsx')
      .should()
      .adhereTo(lineLimit(1200), 'ui/ 组件文件超过 1200 行,请拆分');
    expect(summarize(await rule.check())).toEqual([]);
  });
});
