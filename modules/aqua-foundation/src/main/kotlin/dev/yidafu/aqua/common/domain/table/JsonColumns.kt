/**
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
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

package dev.yidafu.aqua.common.domain.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json

/**
 * DDL 类型为 `json` 的列,以 [String] 直接映射(identity 序列化,结构化转换由仓储层负责)。
 *
 * 不能用 [text] 代替:PostgreSQL 不接受 varchar 绑定插入 json 列
 * (`column "x" is of type json but expression is of type character varying`),
 * text() 只在宽松的 H2 测试里侥幸通过。
 */
fun Table.jsonString(name: String): Column<String> = json(name, { it }, { it })
