package org.everbuild.blocksandstuff.recipes.util

import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.click.Click

/**
 * Removes [resultSlots] from a drag click so a result slot can never be filled.
 *
 * Drags bypass the regular per-slot click handling, so inventories with a
 * take-only result slot must sanitize the slot list here. Non-drag clicks are
 * left untouched and stay handled by the inventory's own PreClick logic.
 */
fun InventoryPreClickEvent.excludeResultSlotsFromDrag(vararg resultSlots: Int) {
    val click = this.click
    if (click !is Click.Drag) return

    val excluded = resultSlots.toSet()
    if (click.slots().none { it in excluded }) return

    val remaining = click.slots().filter { it !in excluded }
    this.click =
        when (click) {
            is Click.LeftDrag -> Click.LeftDrag(remaining)
            is Click.RightDrag -> Click.RightDrag(remaining)
            is Click.MiddleDrag -> Click.MiddleDrag(remaining)
        }
}
