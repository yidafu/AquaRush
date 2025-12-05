#!/usr/bin/env python3
"""
从SVG文件生成所有需要的图标（包括子目录）
使用 cairosvg 库转换SVG为PNG
"""

import re
from pathlib import Path
try:
    from cairosvg import svg2png
except ImportError:
    print("❌ 需要安装 cairosvg: source venv/bin/activate && pip install cairosvg")
    exit(1)

def read_svg_file(svg_path):
    """从文件读取SVG内容"""
    if svg_path.exists():
        with open(svg_path, 'r', encoding='utf-8') as f:
            return f.read()
    return None

def modify_svg_color(svg_content, color="#1890ff"):
    """修改SVG中的fill颜色和stroke颜色"""
    import re

    # 处理填充图标（有fill属性的）
    if 'fill=' in svg_content and 'fill="none"' not in svg_content:
        # 替换现有的fill属性
        svg_content = re.sub(r'fill="[^"]*"', 'fill="' + color + '"', svg_content)
    # 处理描边图标（有stroke属性的，如导航图标）
    elif 'stroke=' in svg_content and 'stroke="none"' not in svg_content:
        # 替换现有的stroke属性
        svg_content = re.sub(r'stroke="[^"]*"', 'stroke="' + color + '"', svg_content)
    # 如果SVG内容中既没有fill也没有stroke，则添加fill属性
    elif 'fill=' not in svg_content and 'stroke=' not in svg_content:
        # 在path标签的末尾添加fill属性
        svg_content = re.sub(r'(<path[^>]*)(/?>)', r'\1 fill="' + color + r'"\2', svg_content)

    return svg_content

def convert_svg_to_png(svg_content, output_path, size=(24, 24)):
    """使用cairosvg转换SVG为PNG"""
    try:
        # 确保输出目录存在
        output_path.parent.mkdir(parents=True, exist_ok=True)

        # 使用cairosvg直接转换SVG内容为PNG
        png_data = svg2png(
            bytestring=svg_content.encode('utf-8'),
            output_width=size[0],
            output_height=size[1]
        )

        # 写入PNG文件
        with open(output_path, 'wb') as f:
            f.write(png_data)

        return True

    except Exception as e:
        print(f"❌ 转换失败 {output_path}: {e}")
        return False

def find_all_svg_files(current_dir):
    """递归查找所有SVG文件，返回相对路径列表"""
    svg_files = []

    for svg_file in current_dir.rglob("*.svg"):
        # 跳过特定的SVG文件
        if svg_file.name in ["icons.svg", "simple-icons.svg"]:
            continue
        svg_files.append(svg_file)

    return svg_files

def get_output_path(svg_file, current_dir, suffix=""):
    """根据SVG文件路径生成对应的PNG输出路径"""
    # 计算相对于current_dir的相对路径
    relative_path = svg_file.relative_to(current_dir)
    # 将.svg扩展名改为.png，然后添加后缀
    png_path = relative_path.with_suffix('.png')
    if suffix:
        # 在.png之前插入后缀
        output_path = current_dir / png_path.with_name(f"{png_path.stem}{suffix}{png_path.suffix}")
    else:
        output_path = current_dir / png_path
    return output_path

def generate_all_icons():
    """从SVG文件生成所有需要的图标（包括子目录）"""
    current_dir = Path(__file__).parent

    # 递归查找所有SVG文件
    svg_files = find_all_svg_files(current_dir)

    # 需要特别处理的图标（用于生成橙色版本）
    special_icons = ["map-pin", "feedback", "truck"]

    print(f"🎨 发现 {len(svg_files)} 个可用SVG图标:")
    for svg_file in svg_files:
        relative_path = svg_file.relative_to(current_dir)
        print(f"   📁 {relative_path}")

    special_found = [icon for icon in special_icons if any(icon in str(f) for f in svg_files)]
    print(f"🔧 特殊处理图标 (生成橙色版本): {', '.join(special_found)}")
    print()

    print("🚀 开始从SVG文件生成 AquaRush 图标...")

    generated_count = 0

    for svg_file in svg_files:
        # 读取SVG文件
        svg_content = read_svg_file(svg_file)
        if not svg_content:
            print(f"⚠️  跳过无法读取的文件: {svg_file}")
            continue

        icon_name = svg_file.stem
        relative_path = svg_file.relative_to(current_dir)

        # 判断是否为导航图标
        is_nav_icon = icon_name in ["home", "home-active", "my", "my-active", "order", "order-active"]

        if is_nav_icon:
            # 导航图标处理：active状态用主题色，非active状态用灰色
            if "active" in icon_name:
                # active状态的导航图标使用主题色
                if "stroke=" in svg_content:
                    theme_svg = re.sub(r'stroke="[^"]*"', 'stroke="#1890ff"', svg_content)
                else:
                    theme_svg = re.sub(r'fill="[^"]*"', 'fill="#1890ff"', svg_content)
            else:
                # 非active状态的导航图标保持原色（灰色）
                theme_svg = svg_content
        else:
            # 普通图标使用主题色
            theme_svg = modify_svg_color(svg_content, "#1890ff")

        # 生成默认色的图标
        for suffix, size in [("", (36, 36)), ("@2x", (72, 72)), ("@3x", (108, 108))]:
            output_file = get_output_path(svg_file, current_dir, suffix)
            # 确保输出目录存在
            output_file.parent.mkdir(parents=True, exist_ok=True)

            if convert_svg_to_png(theme_svg, output_file, size):
                print(f"✅ 生成: {output_file.relative_to(current_dir)}")
                generated_count += 1

        # 为特殊图标生成橙色版本
        if icon_name in special_icons and not is_nav_icon:
            orange_svg = modify_svg_color(svg_content, "#ff6b35")
            for suffix, size in [("", (24, 24)), ("@2x", (48, 48))]:
                output_file = get_output_path(svg_file, current_dir, f"-orange{suffix}")
                # 确保输出目录存在
                output_file.parent.mkdir(parents=True, exist_ok=True)

                if convert_svg_to_png(orange_svg, output_file, size):
                    print(f"🔶 生成: {output_file.relative_to(current_dir)}")
                    generated_count += 1

    print(f"\n🎉 完成! 总共生成了 {generated_count} 个图标文件")

if __name__ == "__main__":
    generate_all_icons()
