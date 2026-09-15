package llc.redstone.htslreborn.queue

import llc.redstone.htslreborn.utils.InputUtils

object Progress {
    private const val ALPHA = 0.3
    private const val MIN_SAMPLES = 3
    private const val DISPLAY_INTERVAL_MS = 1000L
    private const val DISPLAY_GROW_THRESHOLD = 1.2

    var startedAt = 0L
        private set

    var totalOps = 0
        private set

    var completedOps = 0
        private set

    private var lastSuccessAt = 0L
    private var pausedAt = 0L
    private val ema = HashMap<String, Double>()

    private var displayedRemainingMs: Long? = null
    private var displayUpdatedAt = 0L

    val active get() = totalOps > 0

    val paused get() = pausedAt != 0L

    val fraction: Float get() = if (totalOps == 0) 0f else completedOps.toFloat() / totalOps

    val elapsedMs: Long
        get() {
            if (startedAt == 0L) return 0L
            val end = if (pausedAt != 0L) pausedAt else System.currentTimeMillis()
            return end - startedAt
        }

    // Only ever written from the queue's coroutine; the HUD reads them from the render thread.
    @Volatile
    var remainingMs: Long? = null
        private set

    @Volatile
    var indeterminate = false
        private set

    /** Call from whoever owns the queue, after it has changed. */
    fun recompute() {
        val pending = pendingOps()
        val unknown = pending.any { !it.estimable() }
        indeterminate = completedOps < MIN_SAMPLES || unknown
        remainingMs = if (pending.isEmpty() || unknown) null else pending.sumOf { cost(it) }.toLong()
    }

    /** Rate-limited and dampened so the number doesn't jitter every tick. */
    val displayRemainingMs: Long?
        get() {
            val raw = remainingMs ?: return null
            val now = System.currentTimeMillis()
            val shown = displayedRemainingMs
            val stale = now - displayUpdatedAt >= DISPLAY_INTERVAL_MS
            val grewALot = shown != null && raw > shown * DISPLAY_GROW_THRESHOLD
            if (shown == null || grewALot || (stale && raw <= shown)) {
                displayedRemainingMs = raw
                displayUpdatedAt = now
            } else if (stale) {
                displayedRemainingMs = shown - (now - displayUpdatedAt)
                displayUpdatedAt = now
            }
            return displayedRemainingMs?.coerceAtLeast(0)
        }

    val averages: Map<String, Double> get() = ema

    fun onEnqueued(count: Int) {
        if (count <= 0) return
        if (totalOps == 0) {
            startedAt = System.currentTimeMillis()
            lastSuccessAt = startedAt
        }
        totalOps += count
        recompute()
    }

    fun onSucceeded(op: Operation, cleanRun: Boolean) {
        val now = System.currentTimeMillis()
        completedOps++
        if (cleanRun && op.fixedCost() == null) {
            val observed = (now - lastSuccessAt).toDouble()
            ema[op.costKey] = ema[op.costKey]?.let { ALPHA * observed + (1 - ALPHA) * it } ?: observed
        }
        lastSuccessAt = now
    }

    fun onFailed() {
        lastSuccessAt = System.currentTimeMillis()
    }

    fun onPaused() {
        if (pausedAt == 0L) pausedAt = System.currentTimeMillis()
    }

    fun reset() {
        startedAt = 0L
        totalOps = 0
        completedOps = 0
        lastSuccessAt = 0L
        pausedAt = 0L
        displayedRemainingMs = null
        displayUpdatedAt = 0L
        remainingMs = null
        indeterminate = false
    }

    private fun pendingOps(): List<Operation> =
        listOfNotNull(Queue.current) + Queue.queue

    private fun cost(op: Operation): Double {
        op.fixedCost()?.let { return it.toDouble() }
        return ema[op.costKey] ?: prior(op.costKey)
    }

    private fun prior(key: String): Double {
        val ping = InputUtils.getClientPing().coerceAtLeast(0)
        return when (key) {
            "OpenMenu" -> 150.0 + ping
            "OpenMenu.check" -> 30.0
            "Click" -> 10.0
            "ClickItem", "Option" -> 50.0 + ping
            "Input" -> 300.0 + 2 * ping
            "Chat" -> 100.0 + ping
            "Item" -> 100.0 + ping
            else -> 100.0 + ping
        }
    }

    fun format(ms: Long?, indeterminate: Boolean = false): String {
        if (ms == null) return "--"
        val total = (ms + 999) / 1000
        val prefix = if (indeterminate) "~" else ""
        val m = total / 60
        val s = total % 60
        return if (m > 0) "$prefix${m}m %02ds".format(s) else "$prefix${s}s"
    }
}
