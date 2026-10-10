# 软删表查询约定

> 配套根 `AGENTS.md` 关键规则 13。基础设施在 `modules/aqua-foundation/.../common/domain/table/`
> (`SoftDeleteFlagTable.kt` / `SoftDeletableTable.kt` / `SoftDeleteOps.kt`),本指南只讲怎么用。

## 机制概览

Exposed 没有隐式软删过滤,项目用**基类成员遮蔽扩展函数**把过滤固定到查询入口:
`SoftDeleteFlagTable` 重写了 `selectAll()` / `select()` 成员,自动追加 `is_deleted = false`;
Kotlin 解析中成员函数优先于扩展函数,所以只要静态类型是软删基类,单表查询漏过滤在语法上不可能。

两个基类:

| 基类 | 列 | 适用 |
| --- | --- | --- |
| `SoftDeleteFlagTable` | `is_deleted` | 只需软删标记、无留痕列的表 |
| `SoftDeletableTable` | `is_deleted` + `deleted_at` + `deleted_by` | 需要业务侧留痕的表 |

## 查询规则

### 1. 单表读:直接 `selectAll()` / `select()`,追加条件只用 `andWhere {}`

```kotlin
// ✅ 自动带 is_deleted = false
val rows = UsersTable.selectAll().andWhere { UsersTable.status eq "ACTIVE" }
val names = UsersTable.select(UsersTable.nickname).andWhere { … }

// ❌ 再调 .where {} 会抛 IllegalStateException: WHERE clause is specified twice
UsersTable.selectAll().where { … }
```

`selectAll()` / `select()` 入口已消费掉唯一的 `where` 名额,后续条件**必须**走
`andWhere {}` / `orWhere {}`。

### 2. 计数:`countActive {}`

```kotlin
val activeCount = UsersTable.countActive { UsersTable.role eq "USER" }
```

### 3. join / 聚合 / 子查询:用 `activeOnly {}` 逐表取条件

Exposed 无法自动枚举参与 join 的表,每个软删表都要各自过滤:

```kotlin
OrdersTable
  .join(UsersTable, JoinType.INNER, additionalConstraint = {
    UsersTable.activeOnly { UsersTable.id eq OrdersTable.userId }
  })
  .selectAll()
  .where { OrdersTable.status eq "PAID" }
```

聚合表达式、groupBy、子查询里手写 where 时同理,用 `activeOnly {}` 取条件再 `and`。

### 4. 软删:`softDelete {}`

```kotlin
val affected = UsersTable.softDelete { UsersTable.id eq userId }
// 等价 UPDATE ... SET is_deleted = true WHERE (is_deleted = false) AND (条件)
```

`SoftDeletableTable` 的 `deleted_at` / `deleted_by` 由调用方在同一 `update {}` 里一并写入。

### 5. 读已删行:**只能**走 `selectAllIncludingDeleted()`

这是唯一的显式逃逸口,合法场景仅限:审计、恢复,以及 `save()` 里「主键是否已存在」的判定
(按活跃行判定会把重建的已删主键落到 INSERT 上撞唯一约束)。

```kotlin
val deleted = UsersTable.selectAllIncludingDeleted().andWhere { UsersTable.isDeleted eq true }
```

## 守卫

- `services/aqua-admin/src/test/kotlin/dev/yidafu/aqua/architecture/SoftDeleteQueryConventionTest.kt` — 约定守卫测试
- `modules/aqua-foundation/src/test/kotlin/dev/yidafu/aqua/common/domain/model/UserModelSoftDeleteTest.kt` — 基类行为测试

新增软删表继承两个基类之一即可,不要手写 `is_deleted = false` 过滤;join/聚合漏过滤会被守卫测试拦下。
