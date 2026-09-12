package llc.redstone.htslreborn.importer

import llc.redstone.htslreborn.HTSLReborn.LOGGER
import llc.redstone.htslreborn.HTSLReborn.MC


@DslMarker
annotation class OperationDsl

@OperationDsl
class OperationBuilder {
    val ops = mutableListOf<Operation>()
    operator fun Operation.unaryPlus() { ops += this }
}

object Queue {
    private val queue = ArrayDeque<Operation>()

    var current: Operation? = null
        private set

    var executing = false
        private set

    val isIdle get() = current == null && queue.isEmpty()

    fun enqueue(block: OperationBuilder.() -> Unit) {
        queue.addAll(OperationBuilder().apply(block).ops)
    }

    operator fun plusAssign(operation: Operation) {
        queue += operation
    }

    fun addAll(operations: List<Operation>) {
        queue.addAll(operations)
    }

    suspend fun onTick() {
        if (current == null) current = queue.removeFirstOrNull()
        val op = current ?: return
        if (executing) return

        val done = try {
            executing = true
            op.execute(MC)
        } catch (e: Exception) {
            LOGGER.error("Error executing menu navigation: ${e.message}")
            clear() // or error correction or summin
            return
        }

        if (done) {
            executing = false
            current = null
        }
    }

    fun clear() {
        queue.clear()
        current = null
        executing = false
    }
}