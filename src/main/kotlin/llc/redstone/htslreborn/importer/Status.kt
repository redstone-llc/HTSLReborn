package llc.redstone.htslreborn.importer

sealed interface Status {
    data object Success : Status
    data class Failure(val reason: String) : Status
}