package llc.redstone.htslreborn.queue.exporter

import llc.redstone.htslreborn.queue.Operation
import llc.redstone.htslreborn.queue.ResumableSession
import llc.redstone.htslreborn.utils.MenuUtils

object ExportSession : ResumableSession {
    var checkpoint = -1
        private set

    override val canResume get() = checkpoint >= 0

    fun begin() {
        checkpoint = -1
    }

    fun record(index: Int) {
        checkpoint = index
    }

    override fun buildResume(): List<Operation> {
        val index = checkpoint.takeIf { it >= 0 } ?: error("Nothing to resume")
        if (!MenuUtils.isActionContainerOpen()) error("Open the action container you were exporting first")
        return Exporter.buildOps(startIndex = index)
    }

    override fun end() {
        checkpoint = -1
    }

    override fun describe() = "export at action $checkpoint (${Exporter.actions.size} compiled)"
}
