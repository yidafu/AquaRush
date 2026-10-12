/**
 * 前端架构约束(ArchUnitTS)。
 * 业务组件行数硬约束 300 行;组件库(@aquarush/admin-ui,已迁出为独立包)
 * 单独放宽至 1200 行——该规则仍由本套件执行,直接扫包内文件(archunit 的
 * 文件读取以 cwd 为基准,跨包相对路径读不到内容)。
 */
import { readdirSync, readFileSync } from 'node:fs';
import path from 'node:path';
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

/** @aquarush/admin-ui 包根(相对本文件 frontend/admin-client/src/)。 */
const ADMIN_UI_SRC = path.resolve(__dirname, '../../admin-ui/src');

/** 递归列出目录下后缀匹配的文件(相对路径,POSIX 分隔符)。 */
const walk = (dir: string, ext: string, base = dir): string[] =>
  readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const abs = path.join(dir, entry.name);
    if (entry.isDirectory()) return walk(abs, ext, base);
    return entry.name.endsWith(ext) ? [path.relative(base, abs).split(path.sep).join('/')] : [];
  });

describe('admin-client 架构约束(ArchUnitTS)', () => {
  it('业务组件(components/pages 下的 .tsx)不得超过 300 行', async () => {
    const rule = projectFiles()
      .inFolder(COMPONENT_DIRS)
      .withName('*.tsx')
      .should()
      .adhereTo(lineLimit(300), '组件文件超过 300 行,请拆分为子组件');
    expect(summarize(await rule.check())).toEqual([]);
  });

  it('组件库(@aquarush/admin-ui)文件不得超过 1200 行', () => {
    const violations = walk(ADMIN_UI_SRC, '.tsx')
      .map((rel) => ({ rel, lines: physicalLines(readFileSync(path.join(ADMIN_UI_SRC, rel), 'utf8')) }))
      .filter(({ lines }) => lines > 1200)
      .map(({ rel, lines }) => `${rel} (${lines} 行)`);
    expect(violations).toEqual([]);
  });
});
