package llc.redstone.htslreborn.queue

import llc.redstone.htslreborn.utils.InputUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import kotlin.math.abs

object Progress {
    private const val MIN_SAMPLES = 3
    private const val DELAY_WINDOW = 20
    private const val DELAY_MIN_MS = 8.0
    private const val DELAY_MAX_MS = 8_000.0

    /** The shown total sticks until the estimate drifts past these bounds. */
    private const val LATCH_HOLD_MS = 5_000L
    private const val LATCH_DRIFT_FRACTION = 0.15
    private const val LATCH_MIN_DRIFT_MS = 5_000L
    private const val LATCH_BIG_DRIFT_FRACTION = 0.5

    var startedAt = 0L
        private set

    var totalOps = 0
        private set

    var completedOps = 0
        private set

    private var lastSuccessAt = 0L
    private var pausedAt = 0L
    private val delays = HashMap<String, DelayAvg>()

    // Latched by whichever thread renders first; both only ever store a fresh snapshot.
    @Volatile
    private var latchedTotalMs: Long? = null

    @Volatile
    private var latchedAt = 0L

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
        remainingMs = when {
            unknown -> null
            pending.isEmpty() -> 0L
            else -> pending.sumOf { cost(it) }.toLong()
        }
    }

    /** Live estimate, including time already spent on the op in flight. */
    val rawTotalMs: Long?
        get() {
            val remaining = remainingMs ?: return null
            val now = if (pausedAt != 0L) pausedAt else System.currentTimeMillis()
            val spentOnCurrent = if (lastSuccessAt == 0L) 0L else (now - lastSuccessAt).coerceAtLeast(0L)
            return elapsedMs + (remaining - spentOnCurrent).coerceAtLeast(0L)
        }

    /**
     * Holds a single total and only re-latches once the live estimate drifts well
     * clear of it, so the HUD counts down instead of twitching every operation.
     */
    val displayTotalMs: Long?
        get() {
            val raw = rawTotalMs ?: return null
            val now = System.currentTimeMillis()
            val shown = latchedTotalMs ?: run {
                latchedTotalMs = raw
                latchedAt = now
                return raw
            }

            val drift = abs(raw - shown)
            val threshold = maxOf(LATCH_MIN_DRIFT_MS, (shown * LATCH_DRIFT_FRACTION).toLong())
            val overrun = elapsedMs >= shown && raw > shown
            val bigDrift = drift > maxOf(threshold, (shown * LATCH_BIG_DRIFT_FRACTION).toLong())
            val held = now - latchedAt < LATCH_HOLD_MS

            if (overrun || bigDrift || (drift > threshold && !held)) {
                latchedTotalMs = raw
                latchedAt = now
                return raw
            }
            return shown
        }

    val displayRemainingMs: Long?
        get() {
            val total = displayTotalMs ?: return null
            return (total - elapsedMs).coerceAtLeast(0L)
        }

    val averages: Map<String, Double> get() = delays.mapValues { it.value.mean }

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
            observe(op.costKey, (now - lastSuccessAt).toDouble())
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
        latchedTotalMs = null
        latchedAt = 0L
        remainingMs = null
        indeterminate = false
    }

    private fun pendingOps(): List<Operation> =
        listOfNotNull(Queue.current) + Queue.queue

    private fun cost(op: Operation): Double {
        op.fixedCost()?.let { return it.toDouble() }
        return delays[op.costKey]?.mean ?: prior(op.costKey)
    }

    private fun observe(key: String, observed: Double) {
        delays.getOrPut(key) { DelayAvg(prior(key)) }.add(observed)
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

    fun getComponent(): MutableComponent {
        val elapsed = format(elapsedMs)
        val total = format(displayTotalMs, indeterminate)
        return Component.literal("$elapsed / $total")
    }

    fun format(ms: Long?, indeterminate: Boolean = false): String {
        if (ms == null) return "--"
        val total = (ms + 999) / 1000
        val prefix = if (indeterminate) "~" else ""
        val m = total / 60
        val s = total % 60
        return if (m > 0) "$prefix${m}:%02d".format(s) else "${prefix}0:%02d".format(s)
    }

    private class DelayAvg(prior: Double) {
        private val samples = ArrayDeque<Double>().apply {
            repeat(4) { addLast(prior) }
        }
        var mean = prior
            private set

        fun add(observed: Double) {
            val lo = (mean * 0.4).coerceAtLeast(DELAY_MIN_MS)
            val hi = (mean * 2.5).coerceAtMost(DELAY_MAX_MS).coerceAtLeast(lo)
            samples.addLast(observed.coerceIn(lo, hi))
            while (samples.size > DELAY_WINDOW) samples.removeFirst()
            mean = samples.average()
        }
    }
}
