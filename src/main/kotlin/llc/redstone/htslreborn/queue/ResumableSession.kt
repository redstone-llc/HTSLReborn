package llc.redstone.htslreborn.queue

import llc.redstone.htslreborn.data.ScriptContainer
import java.nio.file.Path

/** Something the queue can pause and later rebuild itself from. */
interface ResumableSession {
    val canResume: Boolean

    val container: ScriptContainer?
    val source: Path?

    fun begin(container: ScriptContainer? = null, source: Path? = null)

    /** Ops that navigate back to the last checkpoint and continue. Throws with a user-facing message. */
    fun buildResume(): List<Operation>

    fun end()

    fun describe(): String
}
