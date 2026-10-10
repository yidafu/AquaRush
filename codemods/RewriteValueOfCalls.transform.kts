/**
 * Transform 2: valueOf 三阶段改写 + import/私有扩展追加。
 *
 * 阶段1 DEF: fun X.toGraphQL(): Y = Y.valueOf(name) → when(this) 穷举体 (数据: defWhens)
 * 阶段2 INLINE: Y.valueOf(expr.name) → expr.toGraphQL()/toDomain() (数据: inlineRepls)
 * 阶段3 RENAME: 其余命中集合的 X.valueOf(arg) → X.fromValue(arg) (数据: renames)
 * 尾部: 字符串级插入 import (importsToAdd) 与文件尾追加 private 扩展 (appends)
 *
 * 用法: ktcodeshift -t codemods/RewriteValueOfCalls.transform.kts <src/main dirs...>
 */
@file:Import("./rewrite-data.kts")

fun relPath(path: java.nio.file.Path): String =
    path.toString().substringAfterLast("enum-conversion/").trimStart('/')

fun norm(s: String): String = s.replace(Regex("\\s+"), " ").trim()

fun exprOf(src: String): Node.Expression =
    Ktcodeshift.parse("fun tmp(): Any = $src")
        .find<Node.Declaration.FunctionDeclaration>()
        .map { it.body!! }
        .first()

fun isDotValueOfCall(n: Node.Expression.BinaryExpression): Boolean {
    if (n.operator::class.simpleName != "Dot") return false
    val rhs = n.rhs
    return rhs is Node.Expression.CallExpression && rhs.calleeExpression.toSource() == "valueOf"
}

fun addImports(src: String, imps: List<String>): String {
    var out = src
    for (imp in imps) {
        if (out.contains(imp)) continue
        val lines = out.lines().toMutableList()
        val lastImportIdx = lines.indexOfLast { it.startsWith("import ") }
        if (lastImportIdx >= 0) {
            lines.add(lastImportIdx + 1, imp)
        } else {
            val pkgIdx = lines.indexOfFirst { it.startsWith("package ") }
            if (pkgIdx >= 0) {
                lines.add(pkgIdx + 1, "")
                lines.add(pkgIdx + 2, imp)
            } else {
                lines.add(0, imp)
                lines.add(1, "")
            }
        }
        out = lines.joinToString("\n")
    }
    return out
}

fun appendDecls(src: String, decls: List<String>): String =
    if (decls.isEmpty()) src else src.trimEnd() + "\n\n" + decls.joinToString("\n\n") + "\n"

transform { fileInfo ->
    val rel = relPath(fileInfo.path)
    val dtab = defWhens[rel]
    val itab = inlineRepls[rel]
    val rset = renames[rel]
    val imps = importsToAdd[rel]
    val decls = appends[rel]
    if (dtab == null && itab == null && rset == null && imps == null && decls == null) {
        return@transform null
    }

    var src = fileInfo.source

    // ---- 阶段1: DEF 函数体 → when 穷举 ----
    if (dtab != null) {
        src = Ktcodeshift.parse(src)
            .find<Node.Declaration.FunctionDeclaration>()
            .filter { n ->
                val fn = n.name?.text
                val body = n.body
                (fn == "toGraphQL" || fn == "toDomain") &&
                    body is Node.Expression.BinaryExpression &&
                    isDotValueOfCall(body) &&
                    body.rhs.let { (it as Node.Expression.CallExpression).arguments.size == 1 &&
                        norm(it.arguments[0].expression.toSource()) == "name" } &&
                    dtab.containsKey("${norm(n.receiverType?.toSource() ?: "")}|$fn")
            }
            .replaceWith { n ->
                val fn = n.name?.text
                val key = "${norm(n.receiverType?.toSource() ?: "")}|$fn"
                val body = n.body as Node.Expression.BinaryExpression
                val rhs = body.rhs as Node.Expression.CallExpression
                // 仅改写 body 形如 <Tgt>.valueOf(name) 的定义体
                if (norm(rhs.arguments[0].expression.toSource()) != "name") n
                else n.copy(body = exprOf(dtab.getValue(key)))
            }
            .toSource()
    }

    // ---- 阶段2: Y.valueOf(expr.name) → expr.toGraphQL()/toDomain() ----
    if (itab != null) {
        src = Ktcodeshift.parse(src)
            .find<Node.Expression.BinaryExpression>()
            .filter { n ->
                isDotValueOfCall(n) &&
                    (n.rhs as Node.Expression.CallExpression).arguments.size == 1 &&
                    norm((n.rhs as Node.Expression.CallExpression).arguments[0].expression.toSource()).endsWith(".name") &&
                    itab.containsKey("${norm(n.lhs.toSource())}|${norm((n.rhs as Node.Expression.CallExpression).arguments[0].expression.toSource())}")
            }
            .replaceWith { n ->
                val rhs = n.rhs as Node.Expression.CallExpression
                val key = "${norm(n.lhs.toSource())}|${norm(rhs.arguments[0].expression.toSource())}"
                exprOf(itab.getValue(key)) as Node.Expression.BinaryExpression
            }
            .toSource()
    }

    // ---- 阶段3: 剩余 DB/查询/事件字符串解析 valueOf → fromValue (改 callee, 保留实参格式) ----
    if (rset != null) {
        src = Ktcodeshift.parse(src)
            .find<Node.Expression.BinaryExpression>()
            .filter { n ->
                isDotValueOfCall(n) &&
                    rset.contains(norm(n.lhs.toSource())) &&
                    (n.rhs as Node.Expression.CallExpression).arguments.size == 1 &&
                    norm((n.rhs as Node.Expression.CallExpression).arguments[0].expression.toSource()).let {
                        it != "name" && !it.endsWith(".name")
                    }
            }
            .replaceWith { n ->
                val rhs = n.rhs as Node.Expression.CallExpression
                n.copy(rhs = rhs.copy(calleeExpression = nameExpression("fromValue")))
            }
            .toSource()
    }

    // ---- 尾部: import 插入 + 文件尾追加 private 扩展 ----
    if (imps != null) src = addImports(src, imps)
    if (decls != null) src = appendDecls(src, decls)

    src
}
