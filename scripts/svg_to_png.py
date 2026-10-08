#!/usr/bin/env python3
"""
SVG 转 PNG 工具
将 SVG 文件转换为 PNG 格式的图片
"""

import os
import sys
from pathlib import Path
import xml.etree.ElementTree as ET
try:
    from cairosvg import svg2png
    CAIRO_AVAILABLE = True
except ImportError:
    CAIRO_AVAILABLE = False
    print("警告: 未安装 cairosvg，将使用备用方法")

try:
    from PIL import Image, ImageDraw
    from svglib.svglib import renderSVG
    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False

class SVGToPNGConverter:
    def __init__(self, width=None, height=None, scale=1.0):
        """
        初始化转换器

        Args:
            width: 输出PNG的宽度（像素）
            height: 输出PNG的高度（像素）
            scale: 缩放比例
        """
        self.width = width
        self.height = height
        self.scale = scale

    def convert_with_cairo(self, input_file, output_file):
        """
        使用 cairosvg 转换 SVG 到 PNG

        Args:
            input_file: 输入SVG文件路径
            output_file: 输出PNG文件路径

        Returns:
            bool: 转换是否成功
        """
        if not CAIRO_AVAILABLE:
            return False

        try:
            # 获取SVG的原始尺寸
            tree = ET.parse(input_file)
            root = tree.getroot()

            # 尝试获取视图框
            viewBox = root.get('viewBox')
            if viewBox:
                _, _, orig_width, orig_height = map(float, viewBox.split())
            else:
                # 尝试获取width和height属性
                orig_width = float(root.get('width', 300))
                orig_height = float(root.get('height', 150))

            # 计算输出尺寸
            if self.width and self.height:
                output_width, output_height = self.width, self.height
            elif self.width:
                output_width = self.width
                output_height = int(orig_height * (self.width / orig_width))
            elif self.height:
                output_height = self.height
                output_width = int(orig_width * (self.height / orig_height))
            else:
                output_width = int(orig_width * self.scale)
                output_height = int(orig_height * self.scale)

            # 转换
            with open(input_file, 'rb') as svg_file:
                svg2png(
                    file_obj=svg_file,
                    write_to=output_file,
                    output_width=output_width,
                    output_height=output_height
                )

            return True
        except Exception as e:
            print(f"cairosvg 转换失败: {e}")
            return False

    def convert_with_svglib(self, input_file, output_file):
        """
        使用 svglib + PIL 转换 SVG 到 PNG

        Args:
            input_file: 输入SVG文件路径
            output_file: 输出PNG文件路径

        Returns:
            bool: 转换是否成功
        """
        if not PIL_AVAILABLE:
            return False

        try:
            # 渲染SVG
            drawing = renderSVG.renderSVG(input_file)

            # 创建图像
            if self.width and self.height:
                img = drawing.asPILImage().resize((self.width, self.height))
            elif self.width:
                # 按宽度比例缩放
                orig_width, orig_height = drawing.asPILImage().size
                new_height = int(orig_height * (self.width / orig_width))
                img = drawing.asPILImage().resize((self.width, new_height))
            elif self.height:
                # 按高度比例缩放
                orig_width, orig_height = drawing.asPILImage().size
                new_width = int(orig_width * (self.height / orig_height))
                img = drawing.asPILImage().resize((new_width, self.height))
            elif self.scale != 1.0:
                # 按比例缩放
                orig_width, orig_height = drawing.asPILImage().size
                new_width = int(orig_width * self.scale)
                new_height = int(orig_height * self.scale)
                img = drawing.asPILImage().resize((new_width, new_height))
            else:
                img = drawing.asPILImage()

            # 保存为PNG
            img.save(output_file, 'PNG')
            return True
        except Exception as e:
            print(f"svglib 转换失败: {e}")
            return False

    def convert(self, input_file, output_file=None):
        """
        转换 SVG 到 PNG

        Args:
            input_file: 输入SVG文件路径
            output_file: 输出PNG文件路径（可选）

        Returns:
            str: 输出文件路径
        """
        input_path = Path(input_file)

        # 生成默认输出文件名
        if output_file is None:
            if input_path.suffix.lower() == '.svg':
                output_file = str(input_path.with_suffix('.png'))
            else:
                output_file = f"{input_path}_converted.png"

        # 确保输出目录存在
        output_path = Path(output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        # 尝试不同的转换方法
        success = False

        # 方法1: 使用 cairosvg
        if CAIRO_AVAILABLE:
            print("正在使用 cairosvg 转换...")
            success = self.convert_with_cairo(input_file, output_file)

        # 方法2: 使用 svglib + PIL
        if not success and PIL_AVAILABLE:
            print("正在使用 svglib + PIL 转换...")
            success = self.convert_with_svglib(input_file, output_file)

        if not success:
            raise RuntimeError("无法转换SVG到PNG，请确保已安装 cairosvg 或 svglib + Pillow")

        return output_file

def print_usage():
    """打印使用说明"""
    print("SVG 转 PNG 工具")
    print("=" * 30)
    print("用法:")
    print("  python svg_to_png.py <输入SVG文件> [输出PNG文件]")
    print("")
    print("选项:")
    print("  -w, --width <像素>     设置输出宽度")
    print("  -h, --height <像素>    设置输出高度")
    print("  -s, --scale <比例>     设置缩放比例 (默认: 1.0)")
    print("")
    print("示例:")
    print("  python svg_to_png.py input.svg")
    print("  python svg_to_png.py input.svg output.png")
    print("  python svg_to_png.py -w 800 input.svg")
    print("  python svg_to_png.py -s 2.0 input.svg output.png")
    print("")
    print("依赖库:")
    print("  推荐安装: pip install cairosvg")
    print("  备用方案: pip install svglib pillow")

def main():
    """主函数"""
    if len(sys.argv) < 2:
        print_usage()
        return

    # 解析命令行参数
    args = sys.argv[1:]
    input_file = None
    output_file = None
    width = None
    height = None
    scale = 1.0

    i = 0
    while i < len(args):
        arg = args[i]
        if arg in ['-w', '--width'] and i + 1 < len(args):
            width = int(args[i + 1])
            i += 2
        elif arg in ['-h', '--height'] and i + 1 < len(args):
            height = int(args[i + 1])
            i += 2
        elif arg in ['-s', '--scale'] and i + 1 < len(args):
            scale = float(args[i + 1])
            i += 2
        elif arg.startswith('-'):
            print(f"未知参数: {arg}")
            print_usage()
            return
        else:
            if input_file is None:
                input_file = arg
            elif output_file is None:
                output_file = arg
            else:
                print("参数过多")
                print_usage()
                return
            i += 1

    if input_file is None:
        print("请指定输入SVG文件")
        print_usage()
        return

    # 检查输入文件是否存在
    if not os.path.exists(input_file):
        print(f"错误: 输入文件 '{input_file}' 不存在")
        return

    # 创建转换器
    converter = SVGToPNGConverter(width=width, height=height, scale=scale)

    try:
        # 执行转换
        output_path = converter.convert(input_file, output_file)

        # 显示结果
        input_size = os.path.getsize(input_file)
        output_size = os.path.getsize(output_path)

        print("✅ 转换成功！")
        print(f"📁 输入文件: {input_file} ({input_size:,} 字节)")
        print(f"📁 输出文件: {output_path} ({output_size:,} 字节)")

        # 显示图像信息
        try:
            from PIL import Image
            img = Image.open(output_path)
            print(f"📐 图像尺寸: {img.size[0]} x {img.size[1]} 像素")
        except:
            pass

    except Exception as e:
        print(f"❌ 转换失败: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
