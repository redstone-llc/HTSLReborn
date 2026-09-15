package llc.redstone.htslreborn.queue.differ

import llc.redstone.htslreborn.data.ImportContext
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.queue.Container.enterContext
import llc.redstone.htslreborn.queue.OperationBuilder
import llc.redstone.htslreborn.utils.MenuUtils
import llc.redstone.htslreborn.utils.ToastUtils

object Differ {
    fun process(containers: List<ScriptContainer>) {
        val builder = OperationBuilder()
        for ((index, container) in containers.withIndex()) {
            if (container.context == ImportContext.DEFAULT && !MenuUtils.isActionContainerOpen()) {
                ToastUtils.send("§cSkipping ${container.context.name}", "§7No action container is open.")
                continue
            }
            builder.container = index
            builder.apply {
                enterContext(container)

            }
        }
    }
}