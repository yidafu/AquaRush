#!/bin/bash

# H5版本商详页构建脚本
# 用于构建商详页的H5版本并部署到admin-client

# 设置UTF-8编码
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8

set -e  # 遇到错误时退出

echo "🚀 开始构建H5版本商详页..."

# 定义路径
WEAPP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ADMIN_CLIENT_DIR="$(cd "$WEAPP_DIR/../admin-client" && pwd)"
TEMP_CONFIG_FILE="$WEAPP_DIR/src/app.config.ts.backup"

echo "📁 工作目录: $WEAPP_DIR"
echo "📁 Admin客户端目录: $ADMIN_CLIENT_DIR"

# 1. 备份原始的app.config.ts文件
echo "📦 备份原始配置文件..."
if [ -f "$WEAPP_DIR/src/app.config.ts" ]; then
    cp "$WEAPP_DIR/src/app.config.ts" "$TEMP_CONFIG_FILE"
    echo "✅ 已备份 app.config.ts -> app.config.ts.backup"
else
    echo "❌ 错误: 找不到原始 app.config.ts 文件"
    exit 1
fi

# 2. 将app.config.admin.ts重命名为app.config.ts
echo "🔄 切换到H5管理员配置..."
if [ -f "$WEAPP_DIR/src/app.config.admin.ts" ]; then
    cp "$WEAPP_DIR/src/app.config.admin.ts" "$WEAPP_DIR/src/app.config.ts"
    echo "✅ 已切换到 app.config.admin.ts"
else
    echo "❌ 错误: 找不到 app.config.admin.ts 文件"
    # 恢复原始配置
    if [ -f "$TEMP_CONFIG_FILE" ]; then
        mv "$TEMP_CONFIG_FILE" "$WEAPP_DIR/src/app.config.ts"
    fi
    exit 1
fi

# 3. 创建admin-client的public目录（如果不存在）
echo "📂 创建目标目录..."
TARGET_DIR="$ADMIN_CLIENT_DIR/public/weapp"
mkdir -p "$TARGET_DIR"
echo "✅ 目标目录已准备: $TARGET_DIR"

# 4. 执行构建
echo "🔨 开始执行H5构建..."
cd "$WEAPP_DIR"

# 设置环境变量并构建
export OUTPUT_DIR="../admin-client/public/weapp"
export BUILD_PAGES="product-detail"

if command -v pnpm &> /dev/null; then
    echo "📦 使用 pnpm 构建..."
    pnpm run build:h5
elif command -v npm &> /dev/null; then
    echo "📦 使用 npm 构建..."
    npm run build:h5
else
    echo "❌ 错误: 未找到 npm 或 pnpm"
    # 恢复原始配置
    mv "$TEMP_CONFIG_FILE" "$WEAPP_DIR/src/app.config.ts"
    exit 1
fi

# 5. 检查构建结果
echo "🔍 检查构建结果..."
if [ -d "$TARGET_DIR" ] && [ "$(ls -A "$TARGET_DIR")" ]; then
    echo "✅ 构建成功！文件已生成到: $TARGET_DIR"
    echo "📋 生成的文件:"
    ls -la "$TARGET_DIR"
else
    echo "❌ 构建失败: 目标目录为空或不存在"
    # 恢复原始配置
    mv "$TEMP_CONFIG_FILE" "$WEAPP_DIR/src/app.config.ts"
    exit 1
fi

# 6. 恢复原始配置文件
echo "🔄 恢复原始配置文件..."
mv "$TEMP_CONFIG_FILE" "$WEAPP_DIR/src/app.config.ts"
echo "✅ 已恢复原始 app.config.ts"

# 7. 清理临时文件
echo "🧹 清理临时文件..."
unset OUTPUT_DIR
unset BUILD_PAGES

echo "🎉 H5版本商详页构建完成！"
echo "📂 构建文件位置: $TARGET_DIR"
echo "🌐 可在admin-client中通过 /weapp 路径访问"
