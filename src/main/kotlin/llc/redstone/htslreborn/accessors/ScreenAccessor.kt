package llc.redstone.htslreborn.accessors

interface ScreenAccessor {
    fun isScreenInitialized(): Boolean
    fun setScreenInitialized(initialized: Boolean)
}