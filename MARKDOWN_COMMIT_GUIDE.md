# Markdown 格式 Commit 使用指南

## 概述

AquaRush 项目已更新 commit 生成工具，现在支持生成完整的 Markdown 格式 commit 消息，提供更清晰的变更记录和更好的项目历史可读性。

## 🚀 快速开始

### 方法一：使用脚本

```bash
# 1. 暂存你的更改
git add .

# 2. 运行 commit 生成脚本
./scripts/generate-commit.sh

# 3. 按照提示输入 commit 信息
```

### 方法二：使用斜杠命令

```bash
/commit
```

## 📋 Commit 消息结构

### 标准 Angular 格式标题

```
type(scope): description
```

### 完整 Markdown 正文

```markdown
## 📊 Changes Summary

- **User Module**: 3 files modified
- **Order Module**: 2 files modified
- **Payment Module**: 1 file modified

## 📈 Impact

- **Files Changed**: 6
- **Lines Added**: 120
- **Lines Removed**: 15

## 🧪 Testing Requirements

- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
```

## 🎯 支持的 Commit 类型

| 类型 | 描述 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(user): add user authentication` |
| `fix` | 修复 Bug | `fix(order): resolve payment calculation issue` |
| `docs` | 文档更新 | `docs(readme): update installation guide` |
| `refactor` | 代码重构 | `refactor(services): split monolithic application` |
| `test` | 测试相关 | `test(payment): add unit tests for payment service` |
| `chore` | 构建/配置 | `chore(deps): update Spring Boot dependencies` |

## 🏗️ 支持的范围

### 后端模块
- `user` - 用户管理和认证
- `order` - 订单处理和管理
- `delivery` - 配送员和任务管理
- `product` - 产品目录和库存
- `payment` - 支付处理和交易
- `common` - 共享工具和基础设施

### 服务层
- `services` - 服务层配置和结构

### 前端
- `frontend` - 客户端应用和界面

### 其他
- `schema` - 数据库模式和迁移
- `docs` - 文档和 README

## ✨ 新功能特性

### 🔍 智能分析

脚本会自动分析暂存文件：
- 识别涉及的模块和功能
- 建议合适的 commit 类型
- 生成变更统计摘要

### 📊 自动生成的摘要

如果没有提供详细的 commit 正文，脚本会自动生成：

```markdown
## 📊 Changes Summary

- **User Module**: 3 files modified
- **Order Module**: 2 files modified
- **Common Module**: 1 file modified
```

### 📈 影响评估

```markdown
## 📈 Impact

- **Files Changed**: 6
- **Lines Added**: 120
- **Lines Removed**: 15
```

### 🧪 测试清单

对于功能类型的 commit，自动生成测试要求：

```markdown
## 🧪 Testing Requirements

- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
```

## 💡 最佳实践

### 1. 良好的 Commit 标题

```bash
# ✅ 好的示例
feat(user): implement JWT authentication with WeChat integration
fix(payment): resolve WeChat Pay signature verification issue
refactor(services): split monolithic application into microservices

# ❌ 避免的示例
fix bug
update code
feat: some changes
```

### 2. 有意义的范围

```bash
# ✅ 使用具体的范围
feat(user,auth): add JWT token refresh mechanism
fix(payment,wechat): resolve WeChat Pay callback handling

# ❌ 避免过宽的范围
feat(all): update everything
fix(backend): fix multiple issues
```

### 3. 详细的描述

```bash
# ✅ 包含详细的变更说明
feat(order): implement real-time order tracking

Add WebSocket support for real-time order status updates:
- Connect delivery workers to order updates
- Push notifications to customers
- Track delivery progress in real-time
```

## 🔧 配置和自定义

### 脚本位置
```
/scripts/generate-commit.sh
```

### 自定义规则
你可以修改脚本来：
- 添加新的模块分类
- 自定义 commit 类型建议
- 调整 Markdown 格式

## 📖 示例

### 示例 1：新功能开发

```bash
# 暂存用户认证相关文件
git add modules/aqua-user/src/main/kotlin/dev/yidafu/aqua/user/service/AuthService.kt

# 运行脚本
./scripts/generate-commit.sh

# 输入标题
feat(auth): implement JWT authentication with WeChat integration

# 跳过详细描述（使用自动生成）
```

生成的 commit：

```markdown
feat(auth): implement JWT authentication with WeChat integration

## 📊 Changes Summary

- **User Module**: 2 files modified

## 📈 Impact

- **Files Changed**: 2
- **Lines Added**: 85
- **Lines Removed**: 12

## 🧪 Testing Requirements

- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
```

### 示例 2：重构项目

```bash
# 暂存所有重构文件
git add services/ modules/

# 运行脚本
./scripts/generate-commit.sh

# 输入标题
refactor(services): split monolithic application into admin and client services

# 添加详细描述
```

## 🎉 总结

新的 Markdown 格式 commit 功能让 AquaRush 项目的变更历史更加：
- **清晰**：结构化的变更摘要
- **详细**：完整的模块和影响统计
- **标准**：遵循 Angular commit 规范
- **协作友好**：便于代码审查和项目理解

开始使用新的 commit 生成工具，让项目历史更加专业和易读！