transform { fileInfo ->
    val name = fileInfo.path.fileName.toString()
    if (name !in setOf("PromotionActivityStatus.kt", "OrderMappers.kt", "PaymentRepositoryImpl.kt")) {
        return@transform null
    }
    println("===== ${fileInfo.path} =====")
    val tree = Ktcodeshift.parse(fileInfo.source)
    // 只 dump 我们关心的节点类型，避免刷屏
    tree.find<Node.Declaration>().forEach { d ->
        val text = d.text
        if (text.contains("valueOf") || d is Node.Declaration.EnumClass || d.name?.text?.contains("toGraphQL") == true) {
            println("--- Declaration (${d::class.simpleName}) ---")
            println(Dumper.dump(d).take(4000))
        }
    }
    null
}
