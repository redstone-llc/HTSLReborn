package llc.redstone.htslreborn.ui.working

import llc.redstone.htslreborn.queue.Queue
import net.minecraft.client.gui.layouts.AbstractLayout
import net.minecraft.client.gui.layouts.LayoutElement
import java.util.function.Consumer

class QueueLayout(
    x: Int,
    y: Int,
    width: Int,
    height: Int
) : AbstractLayout(x, y, width, height) {
    override fun visitChildren(consumer: Consumer<LayoutElement>) {
        for (entry in Queue.containers) {
            consumer.accept(entry)
        }
    }

    override fun arrangeElements() {
        super.arrangeElements()
        var childY = y
        for (entry in Queue.containers) {
            entry.setPosition(x, childY)
            childY += entry.height + 1
        }
    }

    //? if >=26.2 {
    // override fun removeChildren(): Unit { }
    //? }

    override fun getWidth(): Int = 197

    override fun getHeight(): Int = Queue.containers.size * 17
}
