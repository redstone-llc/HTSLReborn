package llc.redstone.htslreborn.utils

object IteratorUtils {
    fun <T> MutableListIterator<T>.peek(): T? {
        if (!this.hasNext()) return null
        val next = this.next()
        this.previous()
        return next
    }
}