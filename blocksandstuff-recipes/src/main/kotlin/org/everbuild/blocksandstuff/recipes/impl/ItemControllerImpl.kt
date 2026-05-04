package org.everbuild.blocksandstuff.recipes.impl

import net.kyori.adventure.key.Key
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.recipes.api.ItemController
import kotlin.reflect.KClass

object ItemControllerImpl : ItemController {
    override fun createCustomItem(
        key: Key,
        amount: Int
    ): ItemStack? {
        return null
    }

    override fun byKey(key: Key): ItemStack? {
        return null
    }

    override val typeTag: Tag<String>
        get() = Tag.String("")

    override fun isCustomItem(itemStack: ItemStack): Boolean {
        return false
    }

    override fun getComponentClassByKey(key: Key): KClass<*>? {
        return null
    }

    override fun keyOfComponent(clazz: KClass<*>): Key? {
        return null
    }

    override fun ItemStack.withAnonymous(component: Any): ItemStack {
        return this
    }
}