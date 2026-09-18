package llc.redstone.htslreborn.queue.differ

import llc.redstone.htslreborn.data.ImportContext
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.queue.Operation
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.queue.ResumableSession
import llc.redstone.htslreborn.queue.exporter.ExportSession
import llc.redstone.htslreborn.utils.MenuUtils
import java.nio.file.Path

object DiffSession : ResumableSession {
    enum class Phase { EXPORT, EDIT }

    override var container: ScriptContainer? = null
        private set

    var phase = Phase.EXPORT
        private set

    override var source: Path? = null
        private set

    override val canResume get() = container != null

    override fun begin(container: ScriptContainer?, source: Path?) {
        this.source = source
        Queue.session = this
        this.container = container
        phase = Phase.EXPORT
    }

    fun record(phase: Phase) {
        this.phase = phase
    }

    override fun buildResume(): List<Operation> {
        val container = container ?: error("Nothing to resume")
        if (container.context == ImportContext.DEFAULT && !MenuUtils.isActionContainerOpen()) {
            error("Open the action container you were diffing first")
        }

        val exportFrom = when (phase) {
            Phase.EXPORT -> ExportSession.checkpoint.coerceAtLeast(0)
            Phase.EDIT -> 0
        }
        return Differ.build(container, exportFrom = exportFrom)
    }

    override fun end() {
        container = null
        phase = Phase.EXPORT
    }

    override fun describe() = "diff container $container, $phase phase" +
            if (phase == Phase.EXPORT) " at action ${ExportSession.checkpoint}" else ""
}
