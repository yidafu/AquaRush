/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
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

package dev.yidafu.aqua.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import java.io.File

/**
 * ArchUnit 测试共享 class 导入。
 * 范围 = java.class.path 中的项目构建输出(admin 以 build/classes/ 目录上 classpath,
 * 各 modules 以 build/libs 下的 plain jar 上 classpath) + system property 传入的
 * aqua-client 主类目录(不在 classpath 上,避免 Spring 组件扫描冲突);
 * 第三方 jar(位于 ~/.gradle/caches)一律不导入。graphql-gen/KSP 生成类编译进
 * 各模块 main 输出,当前不违反 D1–D6 任何规则,无需豁免。
 *
 * 注:ArchUnit importPaths 对 jar 文件路径不生效(静默导入 0 类),
 * 统一转为 URL 走 importUrls(File.toURI 对目录自动补尾部 /)。
 */
val backendClasses: JavaClasses by lazy {
  val clientDir =
    System.getProperty("archunit.clientClassesDir")
      ?: error("缺少 archunit.clientClassesDir 系统属性(由 services/aqua-admin 测试任务注入)")
  val classpathEntries = System.getProperty("java.class.path").split(File.pathSeparator)
  val projectOutputs = classpathEntries.filter { it.contains("/build/classes/") || it.contains("/build/libs/") }
  val locations = projectOutputs + clientDir
  ClassFileImporter()
    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
    .importUrls(locations.map { File(it).toURI().toURL() })
}
