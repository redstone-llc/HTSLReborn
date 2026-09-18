package llc.redstone.htslreborn.queue.exporter

import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.queue.Operation
import llc.redstone.htslreborn.queue.ResumableSession
import llc.redstone.htslreborn.utils.MenuUtils
import java.nio.file.Path

object ExportSession : ResumableSession {
    override var container: ScriptContainer? = null
        private set

    override var source: Path? = null
        private set

    var checkpoint = -1
        private set

    override val canResume get() = checkpoint >= 0

    override fun begin(container: ScriptContainer?, source: Path?) {
        this.container = container
        this.source = source
        checkpoint = -1
    }

    fun record(index: Int) {
        checkpoint = index
    }

    override fun buildResume(): List<Operation> {
        val index = checkpoint.takeIf { it >= 0 } ?: error("Nothing to resume")
        if (!MenuUtils.isActionContainerOpen()) error("Open the action container you were exporting first")
        return Exporter.build(container, exportFrom = index)
    }

    override fun end() {
        checkpoint = -1
        container = null
        source = null
    }

    override fun describe() = "export at action $checkpoint (${Exporter.actions.size} compiled)"
}
