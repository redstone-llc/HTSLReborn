package llc.redstone.htslreborn.queue

sealed interface Status {
    data object Success : Status
    data class Failure(val reason: String) : Status
}