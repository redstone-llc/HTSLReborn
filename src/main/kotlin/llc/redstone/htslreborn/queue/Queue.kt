package llc.redstone.htslreborn.queue

import kotlinx.coroutines.launch
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.SCOPE
import llc.redstone.htslreborn.queue.differ.DiffSession
import llc.redstone.htslreborn.queue.differ.Differ
import llc.redstone.htslreborn.queue.exporter.ExportSession
import llc.redstone.htslreborn.queue.exporter.Exporter
import llc.redstone.htslreborn.queue.importer.ImportSession
import llc.redstone.htslreborn.queue.importer.Importer
import llc.redstone.htslreborn.ui.working.ContainerQueueEntry
import llc.redstone.htslreborn.ui.working.WorkingWidget
import llc.redstone.htslreborn.utils.TextUtils
import llc.redstone.htslreborn.utils.ToastUtils


@DslMarker
annotation class OperationDsl

@OperationDsl
class OperationBuilder {
    val ops = mutableListOf<Operation>()
    val path = mutableListOf<Int>()

    operator fun Operation.unaryPlus() {
        ops += this
    }
}

object Queue {
    val containers = mutableListOf<ContainerQueueEntry>()
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

    var tasksStarted: Int = 0

    var guiContext: String? = null

    @Volatile
    var paused = false
        private set

    var session: ResumableSession? = null

    val isActive get() = !paused && (current != null || queue.isNotEmpty() || executing)

    fun pause(): Boolean {
        if (paused || !isActive) return false
        paused = true
        Progress.onPaused()
        HTSLReborn.LOGGER.info("Queue paused: {}", session?.describe())
        return true
    }

    fun resume() {
        if (executing) error(TextUtils.translate("htslreborn.error.resume_busy"))
        val session = session ?: error(TextUtils.translate("htslreborn.error.nothing_to_resume"))
        val ops = session.buildResume()

        clear(discardSession = false)
        addAll(ops)
        paused = false
        this.session = session
        HTSLReborn.LOGGER.info("Queue resumed: {} ({} ops)", session.describe(), ops.size)
    }

    fun enqueue(block: OperationBuilder.() -> Unit) {
        addAll(OperationBuilder().apply(block).ops)
    }

    operator fun plusAssign(operation: Operation) {
        addAll(listOf(operation))
    }

    fun add(operation: Operation, index: Int = -1) = addAll(listOf(operation), index)

    fun addAll(operations: List<Operation>, index: Int = -1) {
        if (index == -1) {
            queue.addAll(operations)
        } else {
            queue.addAll(index, operations)
        }
        Progress.onEnqueued(operations.size)
    }

    fun onTick() {
        if (executing || paused) return

        if (session == null) {
            if (tasksStarted >= containers.size) {
                tasksStarted = 0
                containers.clear()
                WorkingWidget.scrollHeight = 0.0 //SPAGHETTI CODEEEEEE
                return
            }
            containers.getOrNull(tasksStarted)?.let { entry ->
                session = when (entry.context) {
                    is Importer -> ImportSession
                    is Exporter -> ExportSession
                    is Differ -> DiffSession
                    else -> error("Unknown context: ${entry.context}")
                }
                tasksStarted++
                session?.begin(entry.container, entry.source)
                Progress.reset()
                addAll(entry.context.build(entry.container, path = entry.source))
            }
        }

        if (current == null) current = queue.removeFirstOrNull()
        val op = current ?: return

        executing = true
        SCOPE.launch {
            try {
                val done = try {
                    op.execute(MC)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Status.Failure(TextUtils.translate("htslreborn.error.execute", op, e.message ?: ""))
                }
                attempts++

                if (done == Status.Success) {
                    HTSLReborn.LOGGER.info("[${queue.size - 1}] Operation succeeded: {}", op)
                    Progress.onSucceeded(op, cleanRun = attempts == 1)
                    if (op !is Operation.OpenMenu) {
                        previous = op
                    }
                    current = queue.removeFirstOrNull()
                    attempts = 0
                    if (current == null) {
                        Progress.reset()
                        session?.end()
                        session = null
                    } else {
                        Progress.recompute()
                    }
                    return@launch
                }

                if (done is Status.Failure) {
                    HTSLReborn.LOGGER.warn("[${queue.size - 1}] Operation failed: {}. Reason: {}", op, done.reason)
                    Progress.onFailed()
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
                            ToastUtils.paused(done.reason)
                        }
                    }
                }
            } finally {
                executing = false
            }
        }
    }

    fun clear(discardSession: Boolean = true, discardContainers: Boolean = true) {
        queue.clear()
        current = null
        previous = null
        attempts = 0
        guiContext = null
        executing = false
        paused = false
        Progress.reset()
        if (discardSession) {
            session?.end()
            session = null
        }
        if (discardContainers) {
            containers.clear()
            tasksStarted = 0
        }
    }

    fun size(): Int {
        return queue.size + if (current != null) 1 else 0
    }


}