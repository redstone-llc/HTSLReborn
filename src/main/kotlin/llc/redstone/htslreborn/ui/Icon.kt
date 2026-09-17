package llc.redstone.htslreborn.ui

data class Icon(val x: Int, val y: Int, val disabled: Boolean = false, val run: () -> Unit) {
    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in x..(x + 14) && mouseY in y..(y + 14)
    }
}