#!/usr/bin/env python3
"""
add-copyright.py - 为项目源代码文件添加 AGPL-3.0 版权声明头部（按目录白名单处理）。

覆盖的文件类型（按注释风格分组）:
  * 块注释  /* ... */   : kt/kts/gradle/gradle.kts/ts/tsx/js/jsx/mjs/cjs/css/scss/sass/less/wxss/sql
  * 井号注释  # ...      : sh/py/yml/yaml/properties/toml/ini/graphql/graphqls
  * XML 注释  <!-- ... -->: html/xml/wxml/svg

特性:
  * 目录白名单：通过 --dirs 明确指定要处理的目录，脚本不会默认扫描整个仓库。
  * 幂等：已包含 AquaRush/AGPL 版权声明的文件会被自动跳过。
  * 自动跳过构建产物、依赖目录与生成代码（build/.gradle/node_modules/dist/
    graphql-gen/generated 等，以及 "DO NOT EDIT" 标记文件）。
  * 正确处理 shebang（#!/bin/sh）与 XML 声明（<?xml ...?>），版权头插入其后。
  * 保持文件原有行尾风格（LF/CRLF）。
  * --update-year 可刷新已有头部的年份。

用法:
  python3 scripts/add-copyright.py --dirs modules shared-config   # 仅处理白名单目录
  python3 scripts/add-copyright.py --dirs modules --dry-run       # 预览，不修改任何文件
  python3 scripts/add-copyright.py --dirs . --check               # CI 模式：存在缺失头部时非零退出
  python3 scripts/add-copyright.py --dirs frontend --year 2026    # 覆盖版权年份
  python3 scripts/add-copyright.py --dirs modules --update-year   # 更新已有头部中的年份

目录白名单（--dirs）为必填：脚本刻意不做全仓默认扫描，避免误加版权头。
确需处理整个仓库时显式传入 --dirs .。
"""

import argparse
import os
import re
import sys
from datetime import datetime
from pathlib import Path

# ---------------------------------------------------------------------------
# 配置常量
# ---------------------------------------------------------------------------

# 版权声明文本（块注释风格，与现有 Kotlin 头部保持一致，ktlint 兼容）
HEADER_BLOCK_TEMPLATE = """/**
 * AquaRush
 *
 * Copyright (C) {year} AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
"""

# 井号注释风格（sh/py/yml 等）
HEADER_HASH_TEMPLATE = """# AquaRush
#
# Copyright (C) {year} AquaRush Team
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published
# by the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
"""

# XML/HTML 注释风格（html/xml/wxml/svg）
HEADER_XML_TEMPLATE = """<!--
    AquaRush

    Copyright (C) {year} AquaRush Team

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
-->
"""

# 扩展名 -> 注释风格分组
BLOCK_STYLE_EXTS = frozenset({
    ".kt", ".kts", ".gradle", ".gradle.kts",
    ".java",
    ".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs",
    ".css", ".scss", ".sass", ".less", ".wxss",
    ".sql",
})
HASH_STYLE_EXTS = frozenset({
    ".sh", ".py", ".rb",
    ".yml", ".yaml", ".properties", ".toml", ".ini",
    ".graphql", ".graphqls",
})
XML_STYLE_EXTS = frozenset({".html", ".xml", ".wxml", ".svg"})

STYLE_BY_EXT = {}
for _ext in BLOCK_STYLE_EXTS:
    STYLE_BY_EXT[_ext] = "block"
for _ext in HASH_STYLE_EXTS:
    STYLE_BY_EXT[_ext] = "hash"
for _ext in XML_STYLE_EXTS:
    STYLE_BY_EXT[_ext] = "xml"

HEADER_BY_STYLE = {
    "block": HEADER_BLOCK_TEMPLATE,
    "hash": HEADER_HASH_TEMPLATE,
    "xml": HEADER_XML_TEMPLATE,
}

# 扫描时跳过的目录名（任意层级命中即跳过）
SKIP_DIRS = frozenset({
    ".git", ".gradle", ".kotlin", ".idea", ".expo", ".next",
    "node_modules", "build", "dist", "coverage", "out", "target",
    "venv", ".venv", "__pycache__", "Pods", "vendor",
    "graphql-gen", "generated",
})

# 直接跳过的后缀（资源/二进制/锁文件等）
SKIP_SUFFIXES = (
    ".json", ".md", ".markdown", ".txt", ".log",
    ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".icns",
    ".jar", ".war", ".zip", ".gz", ".tgz", ".lock", ".map",
    ".pyc", ".pyo", ".class", ".woff", ".woff2", ".ttf", ".otf",
)

# 已有版权头部检测（前 30 行内出现 Copyright + (AquaRush|AGPL|GNU Affero)）
HAS_HEADER_RE = re.compile(r"Copyright.*(?:AquaRush|AGPL|GNU Affero)", re.IGNORECASE)

# 生成代码标记检测
GENERATED_RE = re.compile(r"(DO NOT EDIT|generated by)", re.IGNORECASE)

# 版权年份（支持 2025、2025-2026、2025, 2026 等写法）
YEAR_RE = re.compile(r"Copyright \(C\) (?:[0-9]{4}(?:\s*[,.\-]\s*[0-9]{4})*)")


# ---------------------------------------------------------------------------
# 工具函数
# ---------------------------------------------------------------------------

def detect_eol(text):
    """检测文件主要行尾风格，默认 LF。"""
    if "\r\n" in text:
        return "\r\n"
    return "\n"


def render_header(style, year, eol):
    """按注释风格与行尾风格渲染版权头部（保证以换行结尾）。"""
    header = HEADER_BY_STYLE[style].format(year=year)
    header = header.replace("\n", eol)
    if not header.endswith(eol):
        header += eol
    return header


def has_header(text):
    """判断文件是否已包含 AquaRush/AGPL 版权头部。"""
    head = eol_join(text.splitlines()[:30])
    return bool(HAS_HEADER_RE.search(head))


def looks_generated(text):
    """判断文件是否为生成代码（头部前 3 行含 DO NOT EDIT / generated by）。"""
    head = eol_join(text.splitlines()[:3])
    return bool(GENERATED_RE.search(head))


def eol_join(lines):
    """将行列表拼成字符串（还原行尾，保持 keepends 语义）。"""
    return "".join(lines)


def is_binary(raw):
    """通过 NUL 字节检测二进制文件。"""
    return b"\x00" in raw[:8192]


def read_text(path):
    """读取文件文本，二进制文件返回 None。"""
    with open(path, "rb") as f:
        raw = f.read()
    if is_binary(raw):
        return None
    return raw.decode("utf-8", errors="replace")


def insert_index(lines, style):
    """计算版权头部应插入的行下标（0 = 文件最顶部）。

    规则：
      * XML 声明 <?xml ...?> 必须位于文件第一行，头部插到其后。
      * shebang（#!/bin/bash）及 Python 编码声明行需要保留在头部之前。
    """
    idx = 0
    if not lines:
        return idx
    first = lines[0]
    if style == "xml":
        if first.lstrip().startswith("<?xml"):
            idx = 1
    elif first.startswith("#!"):
        idx = 1
        if len(lines) > 1 and lines[1].startswith("#") and "coding" in lines[1].lower():
            idx = 2
    return idx


def update_header_year(text, year):
    """将已有头部中的版权年份替换为目标年份（保留首次出现的 Copyright (C)）。"""
    new_text, count = YEAR_RE.subn("Copyright (C) %d" % year, text, count=1)
    return new_text, count


def iter_source_files(scan_roots, skip_dirs):
    """递归遍历白名单目录，产出待处理的源代码文件路径。

    scan_roots 为 --dirs 解析后的绝对路径列表。跳过：跳过目录
    （build/node_modules/generated 等）、非目标扩展名、资源/二进制后缀、
    minified 文件；白名单目录重叠时自动去重。
    """
    seen = set()
    for root in scan_roots:
        for dirpath, dirnames, filenames in os.walk(root):
            # 原地剪枝，避免进入跳过的目录
            dirnames[:] = [d for d in dirnames if d not in skip_dirs]
            for name in filenames:
                lower = name.lower()
                if ".min." in lower:
                    continue
                ext = Path(name).suffix.lower()
                # 目标扩展名已在 STYLE_BY_EXT 中声明；SKIP_SUFFIXES 用于排除
                # 理论上带目标后缀但实为资源/锁文件的边缘情况（如 *.map）
                if any(lower.endswith(s) for s in SKIP_SUFFIXES):
                    continue
                if ext not in STYLE_BY_EXT:
                    continue
                path = os.path.join(dirpath, name)
                if path in seen:
                    continue
                seen.add(path)
                yield path


# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------

def process_file(path, year, update_year, dry_run):
    """处理单个文件，返回 (status, detail)。

    status 取值: ADD / UPDATE / SKIP (已存在) / SKIP (生成代码) /
                 SKIP (二进制/不可读) / ERROR
    """
    text = read_text(path)
    if text is None:
        return "SKIP", "binary/undecodable"

    if looks_generated(text):
        return "SKIP", "generated code"

    ext = Path(path).suffix.lower()
    style = STYLE_BY_EXT.get(ext)
    if style is None:
        return "SKIP", "unsupported extension"

    eol = detect_eol(text)
    lines = text.splitlines(keepends=True)

    if has_header(text):
        if update_year:
            new_text, count = update_header_year(text, year)
            if count > 0 and new_text != text:
                if not dry_run:
                    write_text(path, new_text)
                return "UPDATE", "year refreshed to %d" % year
        return "SKIP", "already has header"

    header = render_header(style, year, eol)
    idx = insert_index(lines, style)
    new_lines = lines[:idx] + [header] + lines[idx:]
    new_text = "".join(new_lines)

    if not dry_run:
        write_text(path, new_text)
    return "ADD", header.splitlines()[0].strip()


def write_text(path, text):
    """原子写回文件（先写临时文件再替换，避免中断导致文件损坏）。"""
    tmp = "%s.%d.tmp" % (path, os.getpid())
    with open(tmp, "w", encoding="utf-8", newline="") as f:
        f.write(text)
    os.replace(tmp, path)


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="为项目所有源代码文件添加 AGPL-3.0 版权声明头部",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="示例:\n  python3 scripts/add-copyright.py --dirs modules\n"
               "  python3 scripts/add-copyright.py --dirs modules --dry-run\n"
               "  python3 scripts/add-copyright.py --dirs . --check --year 2026",
    )
    parser.add_argument("--root", default=".", help="扫描根目录（默认当前目录）")
    parser.add_argument("--dirs", nargs="+", metavar="DIR",
                        help="目录白名单（相对 --root）：仅处理这些目录。必填，"
                             "脚本不会默认扫描整个仓库。")
    parser.add_argument("--dry-run", action="store_true",
                        help="仅报告将发生的变更，不修改任何文件")
    parser.add_argument("--check", action="store_true",
                        help="CI 模式：存在缺失头部的文件时返回退出码 1")
    parser.add_argument("--year", type=int, default=None,
                        help="版权年份（默认使用当前年份）")
    parser.add_argument("--update-year", action="store_true",
                        help="刷新已有头部中的年份")
    parser.add_argument("-v", "--verbose", action="store_true",
                        help="输出每个文件的状态详情")
    args = parser.parse_args(argv)

    year = args.year if args.year else datetime.now().year

    root = os.path.abspath(args.root)
    if not os.path.isdir(root):
        parser.error("root 目录不存在: %s" % root)

    if not args.dirs:
        parser.error(
            "未指定目录白名单。请用 --dirs 列出需要处理的目录，例如：\n"
            "  python3 scripts/add-copyright.py --dirs modules services frontend shared-config\n"
            "确需处理整个仓库时，显式传入 --dirs .\n"
            "（脚本刻意不做全仓默认扫描；可先加 --dry-run 预览。）"
        )

    scan_roots = []
    for d in args.dirs:
        abs_dir = os.path.join(root, d)
        if not os.path.isdir(abs_dir):
            parser.error("白名单目录不存在: %s（相对 root: %s）" % (abs_dir, d))
        scan_roots.append(abs_dir)

    counts = {"ADD": 0, "UPDATE": 0, "SKIP": 0, "ERROR": 0}
    missing = []  # 缺头部但未添加（仅 check 模式统计）

    files = sorted(iter_source_files(scan_roots, SKIP_DIRS))
    for path in files:
        status, detail = process_file(
            path, year, args.update_year, dry_run=args.dry_run or args.check,
        )
        rel = os.path.relpath(path, os.getcwd())
        if status in ("ADD", "UPDATE"):
            counts[status] += 1
            if status == "ADD":
                missing.append(rel)
            if args.verbose or args.dry_run:
                print("[%s] %s (%s)" % (status, rel, detail))
        elif status == "SKIP":
            counts["SKIP"] += 1
            if args.verbose:
                print("[SKIP] %s (%s)" % (rel, detail))
        else:
            counts["ERROR"] += 1
            print("[ERROR] %s (%s)" % (rel, detail), file=sys.stderr)

    print("")
    print("=" * 60)
    mode = "DRY RUN（未修改任何文件）" if args.dry_run else "执行完成"
    if args.check:
        mode = "CHECK（未修改任何文件）"
    print("版权声明扫描: %s" % mode)
    print("  根目录:      %s" % root)
    print("  白名单目录:  %s" % " ".join(args.dirs))
    print("  年份:        %d" % year)
    print("  扫描文件数:  %d" % len(files))
    print("  新增头部:    %d" % counts["ADD"])
    print("  刷新年份:    %d" % counts["UPDATE"])
    print("  跳过(已有):  %d" % counts["SKIP"])
    print("  错误:        %d" % counts["ERROR"])
    print("=" * 60)

    # check 模式：存在缺失头部的文件时非零退出
    if args.check and (missing or counts["ERROR"]):
        print("")
        print("以下 %d 个文件缺少版权声明:" % len(missing))
        for rel in missing:
            print("  - %s" % rel)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
