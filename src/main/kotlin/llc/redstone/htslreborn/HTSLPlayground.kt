package llc.redstone.htslreborn

import llc.redstone.htslreborn.parser.ast.HtslAstBuilder
import org.javers.core.JaversBuilder.javers
import org.javers.core.diff.ListCompareAlgorithm
import java.nio.file.Path
import java.nio.file.Paths


private fun parseHtslFile(path: Path) {
    val ast = HtslAstBuilder.parseFile(path)
    for (container in ast) {
        val target = buildString {
            container.target.name?.let { append(" name=\"").append(it).append('"') }
            container.target.trigger?.let { append(" trigger=\"").append(it).append('"') }
        }
        println("Container ${container.context}$target (${container.actions.size} actions)")
        container.actions.forEach { println("  $it") }
    }
}

fun main() {
//    val home = Paths.get(System.getProperty("user.home"))
//    val htslFolder = home.resolve("Desktop/htsl/")
//    for (file in htslFolder.toFile().listFiles() ?: emptyArray()) {
//        if (file.isFile && file.extension == "htsl") {
//            println(" ==================================== Parsing file: ${file.name} ==================================== ")
//            try {
//                parseHtslFile(file.toPath())
//            } catch (e: Exception) {
//                println("Error parsing file ${file.name}: ${e.message}")
//            }
//        }
//    }


    val obj1 = HtslAstBuilder.parseFile(Paths.get(System.getProperty("user.home")).resolve("Desktop/htsl/test.htsl")).firstOrNull()?.actions
    val obj2 = HtslAstBuilder.parseFile(Paths.get(System.getProperty("user.home")).resolve("Desktop/htsl/test2.htsl")).firstOrNull()?.actions

    val javers = javers()
        .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
        .build()

    println(javers.compare(obj1, obj2))
}
