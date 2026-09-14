package llc.redstone.htslreborn.importer

import kotlinx.coroutines.launch
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.SCOPE
import llc.redstone.htslreborn.utils.ToastUtils


@DslMarker
annotation class OperationDsl

@OperationDsl
class OperationBuilder {
    val ops = mutableListOf<Operation>()
    var container = 0
    val path = mutableListOf<Int>()

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

    @Volatile
    var paused = false
        private set


    val isIdle get() = current == null && queue.isEmpty()

    val isActive get() = current != null || queue.isNotEmpty() || executing

    fun pause(): Boolean {
        if (paused || !isActive) return false
        paused = true
        ImportProgress.onPaused()
        HTSLReborn.LOGGER.info("Import paused at {}", ImportSession.checkpoint)
        return true
    }

    /** Rebuilds the queue from the last checkpoint. Throws with a user-facing message if it can't. */
    fun resume() {
        if (executing) error("An operation is still finishing, try again in a moment")
        val checkpoint = ImportSession.restore()
        val containers = ImportSession.containers ?: error("Nothing to resume")
        val ops = ParserToQueue.buildResume(containers, checkpoint, ImportSession.checkpointBase)

        clear(discardSession = false)
        addAll(ops)
        paused = false
        HTSLReborn.LOGGER.info("Import resumed from {} ({} ops)", checkpoint, ops.size)
    }

    fun enqueue(block: OperationBuilder.() -> Unit) {
        addAll(OperationBuilder().apply(block).ops)
    }

    operator fun plusAssign(operation: Operation) {
        addAll(listOf(operation))
    }

    fun addAll(operations: List<Operation>) {
        queue.addAll(operations)
        ImportProgress.onEnqueued(operations.size)
    }

    fun onTick() {
        if (executing || paused) return
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
                    ImportProgress.onSucceeded(op, cleanRun = attempts == 1)
                    if (op !is Operation.OpenMenu) {
                        previous = op
                    }
                    current = queue.removeFirstOrNull()
                    attempts = 0
                    if (current == null) {
                        ImportProgress.reset()
                        ImportSession.end()
                    } else {
                        ImportProgress.recompute()
                    }
                    return@launch
                }

                if (done is Status.Failure) {
                    HTSLReborn.LOGGER.warn("[${queue.size - 1}] Operation failed: {}. Reason: {}", op, done.reason)
                    ImportProgress.onFailed()
                    if (paused) {
                        attempts = 0
                        return@launch
                    }
                    if (op is Operation.OpenMenu) {
                        op.checkIfOpened = true
                    }
                    if (attempts >= 3) {
                        if (previous != null) {
                            HTSLReborn.LOGGER.warn("[${queue.size - 1}] Retrying previous operation: {}", previous)
                            previous?.execute(MC)
                            previous = null
                        } else {
                            pause()
                            ToastUtils.send("§cImport paused", "§7${done.reason}\n§7Run /htsl resume to retry from the last action.")
                        }
                    }
                }
            } finally {
                executing = false
            }
        }
    }

    fun clear(discardSession: Boolean = true) {
        queue.clear()
        current = null
        previous = null
        attempts = 0
        guiContext = null
        executing = false
        paused = false
        ImportProgress.reset()
        if (discardSession) ImportSession.end()
    }

    fun size(): Int {
        return queue.size + if (current != null) 1 else 0
    }
}