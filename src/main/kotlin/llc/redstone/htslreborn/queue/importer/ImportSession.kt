package llc.redstone.htslreborn.queue.importer

import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.queue.Operation
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.queue.ResumableSession
import java.nio.file.Path

object ImportSession : ResumableSession {
    override var source: Path? = null
        private set
    var sourceHash: String? = null
        private set
    override var container: ScriptContainer? = null
        private set

    var baseCount = 0

    var checkpoint: Operation.Checkpoint? = null
        private set
    var checkpointBase = 0
        private set

    override val canResume get() = checkpoint != null

    override fun buildResume(): List<Operation> {
        val checkpoint = restore()
        val container = container ?: error("Nothing to resume")
        return Importer.buildResume(container, checkpoint, checkpointBase)
    }

    override fun describe() = "import ${source?.fileName} at ${checkpoint?.path} (base $checkpointBase)"

    override fun begin(container: ScriptContainer?, source: Path?) {
        Queue.session = this
        this.source = source
        this.container = container
        baseCount = 0
        checkpoint = null
        checkpointBase = 0
    }

    fun record(checkpoint: Operation.Checkpoint) {
        this.checkpoint = checkpoint
        checkpointBase = baseCount
    }

    override fun end() {
        source = null
        sourceHash = null
        container = null
        baseCount = 0
        checkpoint = null
        checkpointBase = 0
    }

    fun restore(): Operation.Checkpoint {
        checkpoint?.let { if (container != null) return it }
        error("No resume data available")
    }
}
