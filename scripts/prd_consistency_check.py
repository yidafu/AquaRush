#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""AquaRush PRD 一致性校验 —— PRD 单一事实源守卫

背景:`PRD/` 是需求单一事实源,但一次「缺口解决」需要人工在 6 处回写
(域清单表 / 域正文现状 / 验收回归行 / 14-misc 登记行 / 00-PRD 目录导航 / 修订记录),
漏一处即产生跨文件矛盾。本脚本把这些「漏一处」变成可自动发现的断言。

检查项:
  [1] G 编号闭合性   : 被引用的缺口编号必须在 14-misc.md 有登记行(否则查无此号)
  [2] G 编号连续性   : G01..Gmax 无空洞
  [3] 域编号归属     : 域文件「#### F-域-序号」定义的编号必须落在该域「本域范围」声明内
  [4] 目录导航覆盖   : 00-PRD 目录导航的编号区间必须覆盖各域实际定义的编号
  [5] 状态标记一致   : 域文件 §1.1 功能清单表与 §3 条目标题的状态标记一致
  [6] 修订记录时序   : 00-PRD 修订记录版本号严格递增

用法:
  python3 scripts/prd_consistency_check.py
  python3 scripts/prd_consistency_check.py --prd-dir PRD

退出码: 0 = 无 FAIL; 1 = 存在 FAIL(可接入 CI / pre-commit)
"""

import argparse
import os
import re
import sys

FUNC_RE = re.compile(r"\bF-([A-Z]{3})-(\d{3})\b")
GAP_RE = re.compile(r"\bG(\d{1,3})\b")
DEF_HEADING_RE = re.compile(r"^####\s+F-([A-Z]{3})-(\d{3})\b(.*)$", re.M)
STATE_RE = re.compile(r"\[(已实现|部分实现|规划中|已移除)\]")
STATE_ANY_RE = re.compile(r"\[?(已实现|部分实现|规划中|已移除)\]?")
TABLE_ROW_RE = re.compile(r"^\|\s*(F-[A-Z]{3}-\d{3})\s*\|([^\n]*)", re.M)
REV_ROW_RE = re.compile(r"^\|\s*([0-9]{4}(?:-[0-9]{2}-[0-9]{2})?)\s*\|\s*v(\d+)\.(\d+)\s*\|")
SCOPE_RE = re.compile(r"本域范围[:：]\s*(.+?)(?:[;；]|$)")
NAV_ROW_RE = re.compile(r"^\|\s*\[([0-9]{2}-[a-z-]+\.md)\]\([^)]*\)\s*\|\s*([^|]*?)\s*\|\s*([^|]*?)\s*\|")

OK, FAIL = "PASS", "FAIL"
STATE_NORM = {"已实现": "已实现", "部分实现": "部分实现", "规划中": "规划中", "已移除": "已移除"}


def read_docs(prd_dir):
    docs = {}
    for name in sorted(os.listdir(prd_dir)):
        if name.endswith(".md"):
            with open(os.path.join(prd_dir, name), encoding="utf-8") as fh:
                docs[name] = fh.read()
    return docs


def parse_number_set(segment):
    """解析形如 'F-ORD-001~014、020~023' / 'F-COL-001/002/003' 的编号集合。"""
    segment = re.sub(r"（[^）]*）|\([^)]*\)", "", segment)  # 去括号注释(含 预留 说明)
    nums = set()
    for part in re.split(r"[、,，/]", segment):
        part = part.strip()
        if not part:
            continue
        rng = re.search(r"(?:F-[A-Z]{3}-)?(\d{1,3})\s*[~～\-]\s*(?:F-[A-Z]{3}-)?(\d{1,3})", part)
        if rng:
            a, b = int(rng.group(1)), int(rng.group(2))
            if a <= b:
                nums.update(range(a, b + 1))
            continue
        single = re.search(r"(?:F-[A-Z]{3}-)?(\d{1,3})", part)
        if single:
            nums.add(int(single.group(1)))
    return nums


def parse_reserved(segment):
    """解析 '预留 010+' → 10;无则返回 None。"""
    m = re.search(r"预留\s*(\d{1,3})\s*\+", segment)
    return int(m.group(1)) if m else None


def status_of_line(line):
    m = STATE_RE.search(line)
    return STATE_NORM[m.group(1)] if m else None


def check_gap_registry(docs, results):
    """[1] G 编号闭合性 + [2] 连续性"""
    misc = docs.get("14-misc.md", "")
    registered = {int(m) for m in re.findall(r"^\|\s*G(\d{1,3})\s*\|", misc, re.M)}

    missing, refs = set(), {}
    for name, text in docs.items():
        if name == "00-PRD.md":  # 修订记录引用历史编号,不作为「登记」依据
            continue
        for line_no, line in enumerate(text.splitlines(), 1):
            for g in GAP_RE.findall(line):
                num = int(g)
                if num not in registered:
                    missing.add(num)
                    refs.setdefault(num, []).append(f"{name}:{line_no}")

    detail = []
    if missing:
        detail.append("被引用但未在 14-misc.md 登记的缺口编号:")
        for num in sorted(missing):
            detail.append(f"  - G{num:02d} → 引用方 {', '.join(refs[num][:4])}"
                          + ("  …" if len(refs[num]) > 4 else ""))
    results.append(("[1] G 编号闭合性", FAIL if missing else OK, detail))

    detail = []
    if registered:
        top = max(registered)
        holes = [n for n in range(1, top + 1) if n not in registered]
        if holes:
            detail.append(f"G01~G{top:02d} 存在空洞({len(holes)} 个): {fmt_ranges(holes)}")
    results.append(("[2] G 编号连续性", FAIL if detail else OK, detail))
    return registered


def fmt_ranges(nums):
    nums = sorted(nums)
    out, start, prev = [], nums[0], nums[0]
    for n in nums[1:]:
        if n == prev + 1:
            prev = n
            continue
        out.append(f"G{start:02d}" if start == prev else f"G{start:02d}~G{prev:02d}")
        start = prev = n
    out.append(f"G{start:02d}" if start == prev else f"G{start:02d}~G{prev:02d}")
    return ", ".join(out)


def defined_codes(text):
    """域文件「#### F-xxx-nnn」定义的编号 → {prefix: set(int)}"""
    out = {}
    for m in DEF_HEADING_RE.finditer(text):
        out.setdefault(m.group(1), set()).add(int(m.group(2)))
    return out


def check_domain_scope(docs, results):
    """[3] 域编号归属:域文件定义的编号必须在「本域范围」声明内"""
    detail = []
    for name, text in sorted(docs.items()):
        scope_m = SCOPE_RE.search(text)
        if not scope_m:
            continue
        segment = scope_m.group(1)
        allowed = parse_number_set(segment)
        reserved = parse_reserved(segment)
        for prefix, nums in sorted(defined_codes(text).items()):
            if prefix not in segment:
                out_of = sorted(nums)
            else:
                out_of = sorted(n for n in nums
                                if n not in allowed and not (reserved and n >= reserved))
            if out_of:
                codes = ", ".join(f"F-{prefix}-{n:03d}" for n in out_of)
                detail.append(f"{name}:定义 {codes} 超出「本域范围:{segment.strip()}」")
    results.append(("[3] 域编号归属", FAIL if detail else OK, detail))


def check_nav_coverage(docs, results):
    """[4] 00-PRD 目录导航编号区间 ⊇ 各域实际定义编号"""
    nav = docs.get("00-PRD.md", "")
    declared, reserved = {}, {}
    for line in nav.splitlines():
        m = NAV_ROW_RE.search(line)
        if not m:
            continue
        cell = m.group(3) if "F-" in m.group(3) else m.group(2)
        if "F-" not in cell or "缺口" in cell:
            continue
        nums = parse_number_set(cell)
        if not nums:
            continue
        prefixes = set(re.findall(r"F-([A-Z]{3})-", cell))
        rsrv = parse_reserved(cell)
        for p in prefixes:
            declared.setdefault(p, set()).update(nums)
            if rsrv:
                reserved[p] = rsrv

    detail = []
    for name, text in sorted(docs.items()):
        for prefix, nums in sorted(defined_codes(text).items()):
            if prefix not in declared:
                detail.append(f"{name}:域 {prefix} 未出现在 00-PRD 目录导航(定义 {len(nums)} 个编号)")
                continue
            out = sorted(n for n in nums
                         if n not in declared[prefix]
                         and not (prefix in reserved and n >= reserved[prefix]))
            if out:
                codes = ", ".join(f"F-{prefix}-{n:03d}" for n in out)
                detail.append(f"{name}:{codes} 未纳入 00-PRD 目录导航区间")

    # 14-misc 行承诺的缺口区间 vs 实际登记上限
    registered = {int(m) for m in re.findall(r"^\|\s*G(\d{1,3})\s*\|", docs.get("14-misc.md", ""), re.M)}
    gm = re.search(r"缺口登记\(G(\d+)\s*[-~]\s*G?(\d+)\)", nav)
    if gm and registered:
        claimed_to = int(gm.group(2))
        actual_top = max(registered)
        if actual_top > claimed_to:
            detail.append(
                f"00-PRD 目录导航称缺口登记至 G{claimed_to:02d},实际已登记至 G{actual_top:02d}"
                f"(G{claimed_to + 1:02d}~G{actual_top:02d} 未纳入导航描述)")
    results.append(("[4] 目录导航覆盖", FAIL if detail else OK, detail))


def check_state_consistency(docs, results):
    """[5] 域文件 §1.1 表 vs §3 标题 状态标记一致性"""
    detail = []
    for name, text in sorted(docs.items()):
        table, heading = {}, {}
        for m in TABLE_ROW_RE.finditer(text):
            code, rest = m.group(1), m.group(2)
            cells = [c.strip() for c in rest.split("|")]
            if len(cells) < 2:
                continue
            mark = STATE_ANY_RE.fullmatch(cells[1].strip())
            if mark:
                table.setdefault(code, mark.group(1))
        for m in DEF_HEADING_RE.finditer(text):
            code = f"F-{m.group(1)}-{m.group(2)}"
            mark = status_of_line(m.group(0))
            if mark:
                heading.setdefault(code, mark)
        for code in sorted(set(table) & set(heading)):
            if table[code] != heading[code]:
                detail.append(f"{name}:{code} 清单表标 [{table[code]}] vs 条目标题标 [{heading[code]}]")
    results.append(("[5] 状态标记一致", FAIL if detail else OK, detail))


def check_revision_order(docs, results):
    """[6] 00-PRD 修订记录:无重复版本号,且整体按同一方向(升序或降序)单调"""
    detail, seq = [], []
    for line_no, line in enumerate(docs.get("00-PRD.md", "").splitlines(), 1):
        m = REV_ROW_RE.match(line)
        if m:
            seq.append((int(m.group(2)), int(m.group(3)), line_no))

    seen = {}
    for major, minor, line_no in seq:
        key = (major, minor)
        if key in seen:
            detail.append(f"v{major}.{minor} 重复出现(第 {seen[key]} 行、第 {line_no} 行)")
        seen[key] = line_no

    pairs = [(a, b, ln) for a, b, ln in seq]
    asc = sum(1 for (a, b, _), (c, d, _) in zip(pairs, pairs[1:]) if (c, d) <= (a, b))
    desc = sum(1 for (a, b, _), (c, d, _) in zip(pairs, pairs[1:]) if (c, d) >= (a, b))
    if asc <= desc:  # 期望升序
        bad = [(p, q) for p, q in zip(pairs, pairs[1:]) if q[:2] <= p[:2]]
        wrong = "升序"
    else:  # 期望降序
        bad = [(p, q) for p, q in zip(pairs, pairs[1:]) if q[:2] >= p[:2]]
        wrong = "降序"
    for (a, b, ln), (c, d, ln2) in bad[:5]:
        detail.append(f"期望{wrong}但此处逆序:v{a}.{b}(第 {ln} 行) → v{c}.{d}(第 {ln2} 行)")
    if len(bad) > 5:
        detail.append(f"…另有 {len(bad) - 5} 处逆序")
    results.append(("[6] 修订记录时序", FAIL if detail else OK, detail))


def main():
    ap = argparse.ArgumentParser(description="AquaRush PRD 一致性校验")
    ap.add_argument("--prd-dir", default=None, help="PRD 目录(默认仓库根下 PRD/)")
    args = ap.parse_args()

    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    prd_dir = args.prd_dir or os.path.join(root, "PRD")
    if not os.path.isdir(prd_dir):
        print(f"❌ 未找到 PRD 目录: {prd_dir}", file=sys.stderr)
        return 2

    docs = read_docs(prd_dir)
    results = []
    check_gap_registry(docs, results)
    check_domain_scope(docs, results)
    check_nav_coverage(docs, results)
    check_state_consistency(docs, results)
    check_revision_order(docs, results)

    print("=" * 64)
    print("AquaRush PRD 一致性校验")
    print("=" * 64)
    print(f"PRD 目录: {prd_dir}")
    print(f"文档数  : {len(docs)}")
    print()
    for title, status, detail in results:
        dots = "." * max(2, 30 - len(title))
        print(f"{title} {dots} {status}")
        for line in detail:
            print(f"    {line}")
    print("-" * 64)
    failed = sum(1 for _, s, _ in results if s == FAIL)
    print(f"结果: {len(results)} 项检查,{len(results) - failed} PASS / {failed} FAIL")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
