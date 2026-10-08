#!/usr/bin/env python3
"""
从SVG文件生成所有需要的图标（包括子目录）
使用 cairosvg 库转换SVG为PNG（或直接用 rsvg-convert / scripts/svg_to_png.py）

V1.0 设计系统：品牌主色 #3A9BD9（湖蓝），SVG 源文件已内嵌最终颜色，
本脚本仅按 1x/2x/3x（36/72/108px）栅格化，不再做颜色替换。
"""

from pathlib import Path

try:
    from cairosvg import svg2png
except ImportError:
    print("❌ 需要安装 cairosvg: source venv/bin/activate && pip install cairosvg")
    print("   或改用系统 rsvg-convert 逐个转换。")
    raise SystemExit(1)

# V1.0 品牌主色（SVG 源已内嵌，此处仅作说明与兜底）
BRAND_PRIMARY = "#3A9BD9"

# 生成尺寸: 1x / @2x / @3x（微信按 DPR 自动选择）
SIZES = [("", 36), ("@2x", 72), ("@3x", 108)]


def convert_svg_to_png(svg_content, output_path, size):
    """使用cairosvg转换SVG为PNG"""
    try:
        output_path.parent.mkdir(parents=True, exist_ok=True)
        png_data = svg2png(
            bytestring=svg_content.encode("utf-8"),
            output_width=size,
            output_height=size,
        )
        with open(output_path, "wb") as f:
            f.write(png_data)
        return True
    except Exception as e:
        print(f"❌ 转换失败 {output_path}: {e}")
        return False


def find_all_svg_files(current_dir):
    """递归查找所有SVG文件，返回相对路径列表"""
    svg_files = []
    for svg_file in current_dir.rglob("*.svg"):
        # 跳过组合图标源文件
        if svg_file.name in ["icons.svg", "simple-icons.svg"]:
            continue
        svg_files.append(svg_file)
    return svg_files


def get_output_path(svg_file, current_dir, suffix=""):
    """根据SVG文件路径生成对应的PNG输出路径"""
    relative_path = svg_file.relative_to(current_dir)
    png_path = relative_path.with_suffix(".png")
    if suffix:
        output_path = current_dir / png_path.with_name(f"{png_path.stem}{suffix}{png_path.suffix}")
    else:
        output_path = current_dir / png_path
    return output_path


def generate_all_icons():
    """从SVG文件生成所有需要的图标（包括子目录）"""
    current_dir = Path(__file__).parent

    svg_files = find_all_svg_files(current_dir)
    print(f"🎨 发现 {len(svg_files)} 个可用SVG图标:")
    for svg_file in svg_files:
        print(f"   📁 {svg_file.relative_to(current_dir)}")

    print(f"🚀 开始从SVG文件生成 AquaRush 图标（品牌主色 {BRAND_PRIMARY}）...")

    generated_count = 0
    for svg_file in svg_files:
        svg_content = svg_file.read_text(encoding="utf-8")
        for suffix, size in SIZES:
            output_file = get_output_path(svg_file, current_dir, suffix)
            if convert_svg_to_png(svg_content, output_file, size):
                print(f"✅ 生成: {output_file.relative_to(current_dir)}")
                generated_count += 1

    print(f"\n🎉 完成! 总共生成了 {generated_count} 个图标文件")


if __name__ == "__main__":
    generate_all_icons()
