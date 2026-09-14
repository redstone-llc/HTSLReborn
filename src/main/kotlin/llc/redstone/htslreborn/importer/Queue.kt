package llc.redstone.htslreborn.importer

import kotlinx.coroutines.launch
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.SCOPE


@DslMarker
annotation class OperationDsl

@OperationDsl
class OperationBuilder {
    val ops = mutableListOf<Operation>()
    operator fun Operation.unaryPlus() {
        ops += this
    }
}

object Queue {
    val queue = ArrayDeque<Operation>()

    var current: Operation? = null
        private set

    var previous: Operation? = null
        private set

    @Volatile
    var executing = false
        private set

    var attempts: Int = 0
        private set

    var guiContext: String? = null
    var attempted: Int = 0
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

    fun onTick() {
        if (executing) return
        if (current == null) current = queue.removeFirstOrNull()
        val op = current ?: return

        executing = true
        SCOPE.launch {
            try {
                val done = try {
                    op.execute(MC)
                } catch (e: Exception) {
                    Status.Failure("Error executing $op: ${e.message}")
                }
                attempts++

                if (done == Status.Success) {
                    HTSLReborn.LOGGER.info("[${queue.size - 1}] Operation succeeded: {}", op)
                    if (op !is Operation.OpenMenu) {
                        previous = op
                    }
                    current = queue.removeFirstOrNull()
                    attempts = 0
                    return@launch
                }

                if (done is Status.Failure) {
                    HTSLReborn.LOGGER.warn("[${queue.size - 1}] Operation failed: {}. Reason: {}", op, done.reason)
                    if (op is Operation.OpenMenu) {
                        op.checkIfOpened = true
                    }
                    if (attempts >= 3) {
                        if (previous != null) {
                            HTSLReborn.LOGGER.warn("[${queue.size - 1}] Retrying previous operation: {}", previous)
                            previous?.execute(MC)
                            previous = null
                            attempted++
                        } else {
                            clear()
                            throw IllegalStateException("Operation failed after 3 attempts: ${done.reason}")
                        }
                    }
                }
            } finally {
                executing = false
            }
        }
    }

    fun clear() {
        queue.clear()
        current = null
        previous = null
        attempts = 0
        attempted = 0
        guiContext = null
        executing = false
    }

    fun size(): Int {
        return queue.size + if (current != null) 1 else 0
    }
}