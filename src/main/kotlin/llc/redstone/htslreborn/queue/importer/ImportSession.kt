package llc.redstone.htslreborn.queue.importer

import com.google.gson.GsonBuilder
import llc.redstone.htslreborn.HTSLReborn.LOGGER
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.parser.ast.HtslAstBuilder
import llc.redstone.htslreborn.queue.Operation
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.queue.ResumableSession
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

object ImportSession : ResumableSession {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val resumeFile: Path get() = MC.gameDirectory.toPath().resolve("htsl/.resume.json")

    var source: Path? = null
        private set
    var sourceHash: String? = null
        private set
    var containers: List<ScriptContainer>? = null
        private set

    var baseCount = 0

    var checkpoint: Operation.Checkpoint? = null
        private set
    var checkpointBase = 0
        private set

    override val canResume get() = checkpoint != null || Files.exists(resumeFile)

    override fun buildResume(): List<Operation> {
        val checkpoint = restore()
        val containers = containers ?: error("Nothing to resume")
        return Importer.buildResume(containers, checkpoint, checkpointBase)
    }

    override fun describe() = "import ${source?.fileName} at ${checkpoint?.path} (base $checkpointBase)"

    fun begin(source: Path, containers: List<ScriptContainer>) {
        Queue.session = this
        this.source = source
        this.sourceHash = hash(source)
        this.containers = containers
        baseCount = 0
        checkpoint = null
        checkpointBase = 0
    }

    fun record(checkpoint: Operation.Checkpoint) {
        this.checkpoint = checkpoint
        checkpointBase = baseCount
        save()
    }

    override fun end() {
        source = null
        sourceHash = null
        containers = null
        baseCount = 0
        checkpoint = null
        checkpointBase = 0
        runCatching { Files.deleteIfExists(resumeFile) }
    }

    /** Loads in-memory state from disk if needed. Throws with a user-facing message on failure. */
    fun restore(): Operation.Checkpoint {
        checkpoint?.let { if (containers != null) return it }

        val saved = load() ?: error("Nothing to resume")
        val path = Paths.get(saved.source)
        if (!Files.exists(path)) error("Source file no longer exists: ${saved.source}")
        if (hash(path) != saved.hash) {
            end()
            error("${path.fileName} was edited since the import was interrupted")
        }

        source = path
        sourceHash = saved.hash
        containers = HtslAstBuilder.parseFile(path)
        checkpoint = Operation.Checkpoint(saved.container, saved.path)
        checkpointBase = saved.baseCount
        baseCount = saved.baseCount
        return checkpoint!!
    }

    fun load(): Saved? = runCatching {
        if (!Files.exists(resumeFile)) return null
        gson.fromJson(Files.readString(resumeFile), Saved::class.java)
    }.onFailure { LOGGER.warn("Failed to read resume file", it) }.getOrNull()

    private fun save() {
        val cp = checkpoint ?: return
        val saved = Saved(source?.toString() ?: return, sourceHash ?: return, cp.container, cp.path, checkpointBase)
        runCatching {
            Files.createDirectories(resumeFile.parent)
            Files.writeString(resumeFile, gson.toJson(saved))
        }.onFailure { LOGGER.warn("Failed to write resume file", it) }
    }

    private fun hash(path: Path): String =
        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))
            .joinToString("") { "%02x".format(it) }

    data class Saved(
        val source: String,
        val hash: String,
        val container: Int,
        val path: List<Int>,
        val baseCount: Int,
    )
}
