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

    val isActive get() = current != null || queue.isNotEmpty() || executing

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
        if (executing) return
        if (current == null) current = queue.removeFirstOrNull()
        val op = current ?: return

        executing = true
        val done = try {
            op.execute(MC)
        } catch (e: Exception) {
            LOGGER.error("Error executing $op", e)
            false
        } finally {
            executing = false
        }

        if (done) current = null
    }

    fun clear() {
        queue.clear()
        current = null
        executing = false
    }

    fun size(): Int {
        return queue.size + if (current != null) 1 else 0
    }
}