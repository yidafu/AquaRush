/**
 * Transform 1: 给 DB/查询/事件字符串解析目标的领域枚举添加 companion `fromValue`。
 *
 * 数据来源: fromvalue-data.kts (由 generate.py 生成, file -> [enum names])
 * 机制: 已有 companion 则追加函数, 否则新建 companion (ktast writer 会自动补 ';' 分号)
 * when 分支从 AST enumEntries 现场读取, 保证与枚举定义强一致。
 *
 * 用法: ktcodeshift -t codemods/AddFromValue.transform.kts <enum files...>
 */
@file:Import("./fromvalue-data.kts")

fun relPath(path: java.nio.file.Path): String =
    path.toString().substringAfterLast("enum-conversion/").trimStart('/')

fun exprDeclOf(src: String): Node.Declaration =
    Ktcodeshift.parse("class __Wrapper__ {\n$src\n}")
        .find<Node.Declaration.FunctionDeclaration>()
        .map { it }
        .first()

transform { fileInfo ->
    val targets = enumTargets[relPath(fileInfo.path)] ?: return@transform null

    Ktcodeshift.parse(fileInfo.source)
        .find<Node.Declaration.ClassDeclaration>()
        .filter { cls ->
            val n = cls.name?.text
            n != null && n in targets && cls.body?.enumEntries?.isNotEmpty() == true
        }
        .replaceWith { cls ->
            val body = cls.body!!
            val enumName = cls.name!!.text

            // 现场从 AST 读枚举值, 保证 when 分支与枚举定义一致
            val branches = body.enumEntries.joinToString("\n") { entry ->
                val v = entry.name.text
                """        "$v" -> $v"""
            }
            val funSrc = """
                |fun fromValue(value: String): $enumName =
                |    when (value) {
                |$branches
                |        else -> throw IllegalArgumentException("Unknown $enumName value: '" + value + "'")
                |    }
            """.trimMargin()
            val newFun = exprDeclOf(funSrc)

            val existingCompanion = body.declarations
                .filterIsInstance<Node.Declaration.ObjectDeclaration>()
                .firstOrNull { obj -> obj.modifiers.any { it.toSource().contains("companion") } }

            if (existingCompanion != null) {
                val cb = existingCompanion.body!!
                cls.copy(
                    body = body.copy(
                        declarations = body.declarations.map {
                            if (it === existingCompanion) {
                                existingCompanion.copy(
                                    body = cb.copy(declarations = cb.declarations + newFun),
                                )
                            } else it
                        },
                    ),
                )
            } else {
                val companion = Ktcodeshift.parse(
                    "class __Wrapper__ {\ncompanion object {\n$funSrc\n}\n}",
                ).find<Node.Declaration.ObjectDeclaration>().map { it }.first()
                cls.copy(body = body.copy(declarations = body.declarations + companion))
            }
        }
        .toSource()
}
