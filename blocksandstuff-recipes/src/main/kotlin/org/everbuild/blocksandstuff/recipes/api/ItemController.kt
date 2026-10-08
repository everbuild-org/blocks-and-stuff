package org.everbuild.blocksandstuff.recipes.api

import net.kyori.adventure.key.Key
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag
import kotlin.reflect.KClass

interface ItemController {
    val typeTag: Tag<String>
    fun createCustomItem(key: Key, amount: Int = 1) : ItemStack?
    fun byKey(key: Key) : ItemStack?
    fun isCustomItem(itemStack: ItemStack) : Boolean
    fun getComponentClassByKey(key: Key) : KClass<*>?
    fun keyOfComponent(clazz: KClass<*>) : Key?

    fun ItemStack.withAnonymous(component: Any) : ItemStack
}