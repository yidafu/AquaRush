#!/bin/bash

# 目标目录
target_dir="/Users/yidafu/github/AquaRush/modules/aqua-common/src/main/kotlin/dev/yidafu/aqua/common/domain/model"

# 遍历目标目录中的所有.kt文件
for file in "$target_dir"/*.kt; do
    if [ -f "$file" ]; then
        # 获取当前文件名（不带路径和扩展名）
        current_filename=$(basename "$file" .kt)

        # 查找文件中的类名（class, interface, enum class等）
        # 使用正则表达式匹配类定义
        class_name=$(grep -E "^(class|interface|enum class|data class|sealed class)\s+\w+" "$file" | head -1 | sed -E "s/^(class|interface|enum class|data class|sealed class)\s+([\w]+).*/\2/")

        if [ -n "$class_name" ]; then
            echo "File: $current_filename.kt"
            echo "Class: $class_name"

            # 检查文件名是否与类名一致
            if [ "$current_filename" != "$class_name" ]; then
                echo "Renaming $current_filename.kt to $class_name.kt"
                mv "$file" "$target_dir/$class_name.kt"
            else
                echo "Filename already matches class name"
            fi

            echo "---"
        else
            echo "No class definition found in $current_filename.kt"
            echo "---"
        fi
    fi
done

echo "All files processed."
