package llc.redstone.htslreborn.importer

import llc.redstone.htslreborn.HTSLReborn.MC
import kotlin.collections.mutableListOf


object Queue {
    var queue = mutableListOf<Operation>()
    var current: Operation? = null

    suspend fun onTick() {
        if (current == null) {
            next()
        }

        if (current?.execute(MC) == true) {
            current = null
            next()
        }
    }

    fun next(): Operation? {
        if (queue.isNotEmpty()) {
            val operation = queue.removeAt(0)
            current = operation
            return operation
        }
        return null
    }

    fun addAll(operations: List<Operation>) {
        queue.addAll(operations)
    }

    fun clear() {
        queue.clear()
        current = null
    }
}
