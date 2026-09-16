package llc.redstone.htslreborn.queue.differ

import llc.redstone.htslreborn.data.ImportContext
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.queue.Operation
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.queue.ResumableSession
import llc.redstone.htslreborn.queue.exporter.ExportSession
import llc.redstone.htslreborn.utils.MenuUtils

object DiffSession : ResumableSession {
    enum class Phase { EXPORT, EDIT }

    var containers: List<ScriptContainer>? = null
        private set

    var container = -1
        private set

    var phase = Phase.EXPORT
        private set

    override val canResume get() = containers != null && container >= 0

    fun begin(containers: List<ScriptContainer>) {
        Queue.session = this
        this.containers = containers
        container = -1
        phase = Phase.EXPORT
    }

    fun record(container: Int, phase: Phase) {
        this.container = container
        this.phase = phase
    }

    override fun buildResume(): List<Operation> {
        val containers = containers ?: error("Nothing to resume")
        val index = container.takeIf { it >= 0 } ?: error("Nothing to resume")
        if (containers[index].context == ImportContext.DEFAULT && !MenuUtils.isActionContainerOpen()) {
            error("Open the action container you were diffing first")
        }

        val exportFrom = when (phase) {
            Phase.EXPORT -> ExportSession.checkpoint.coerceAtLeast(0)
            Phase.EDIT -> 0
        }
        return Differ.build(containers, from = index, exportFrom = exportFrom)
    }

    override fun end() {
        containers = null
        container = -1
        phase = Phase.EXPORT
    }

    override fun describe() = "diff container $container, $phase phase" +
            if (phase == Phase.EXPORT) " at action ${ExportSession.checkpoint}" else ""
}
