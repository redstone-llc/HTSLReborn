package llc.redstone.htslreborn.queue

import llc.redstone.htslreborn.data.ScriptContainer
import java.nio.file.Path

interface BuildableContainer {
    fun build(container: ScriptContainer? = null, exportFrom: Int = 0, path: Path? = null): List<Operation>
}