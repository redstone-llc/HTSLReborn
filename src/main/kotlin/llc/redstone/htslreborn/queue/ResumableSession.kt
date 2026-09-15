package llc.redstone.htslreborn.queue

/** Something the queue can pause and later rebuild itself from. */
interface ResumableSession {
    val canResume: Boolean

    /** Ops that navigate back to the last checkpoint and continue. Throws with a user-facing message. */
    fun buildResume(): List<Operation>

    fun end()

    fun describe(): String
}
