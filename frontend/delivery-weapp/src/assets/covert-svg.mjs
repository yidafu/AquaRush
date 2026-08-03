const sharp = require("sharp");
const fs = require("fs").promises;

async function svgToPngWithSharp(svgPath, outputPath, options = {}) {
  const { width, height, backgroundColor } = options;

  try {
    // 读取 SVG 文件
    const svgBuffer = await fs.readFile(svgPath);

    // 配置 sharp
    const image = sharp(svgBuffer, { density: 300 }); // 设置 DPI

    // 可选：设置背景色（透明 SVG 转不透明 PNG 时需要）
    if (backgroundColor) {
      image.flatten({ background: backgroundColor });
    }

    // 可选：调整尺寸
    if (width || height) {
      image.resize(width, height, {
        fit: "contain",
        background: backgroundColor || "#ffffff",
      });
    }

    // 转换为 PNG 并保存
    await image.png().toFile(outputPath);
    console.log(`SVG 已转换为 PNG: ${outputPath}`);
  } catch (error) {
    console.error("转换失败:", error);
  }
}

// 使用示例
// svgToPngWithSharp("input.svg", "output.png", {
//   width: 800,
//   backgroundColor: "#ffffff", // 可选，设置白色背景
// });

const svgList = await glob('*.svg');

for (const svg of svgList) {
  svgToPngWithSharp(svg, svg.replace("svg", 'png'), {
    width: 64,
  });
}
