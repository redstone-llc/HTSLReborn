package llc.redstone.htslreborn.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import llc.redstone.htslreborn.HTSLReborn.MC
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

object ClientThread {

    @Volatile
    private var lastPacketAt = 0L

    suspend fun <T> run(block: () -> T): T {
        if (MC.isSameThread) return block()
        return suspendCancellableCoroutine { cont ->
            MC.execute {
                if (!cont.isActive) return@execute
                runCatching(block).fold({ cont.resume(it) }, { cont.resumeWithException(it) })
            }
        }
    }

    suspend fun <T> send(block: () -> T): T {
        val wait = lastPacketAt + 50L - System.currentTimeMillis()
        if (wait > 0) delay(wait.milliseconds)
        return try {
            run(block)
        } finally {
            lastPacketAt = System.currentTimeMillis()
        }
    }

    suspend fun settleMenu() {
        delay((50 + InputUtils.getClientPing().coerceAtLeast(0)).milliseconds)
    }
}
