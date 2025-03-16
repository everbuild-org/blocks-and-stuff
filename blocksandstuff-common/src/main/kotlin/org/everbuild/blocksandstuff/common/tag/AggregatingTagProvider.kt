package org.everbuild.blocksandstuff.common.tag

abstract class AggregatingTagProvider<T> : TagProvider<T> {
    private val children = mutableListOf<TagProvider<T>>()

    fun addChild(child: TagProvider<T>) = children.add(child)
    fun removeChild(child: TagProvider<T>) = children.remove(child)
    fun clearChildren() = children.clear()

    override fun hasTag(element: T, tag: String) = children.any { it.hasTag(element, tag) }
    override fun getTaggedWith(tag: String) = children.flatMap { it.getTaggedWith(tag) }.toSet()
}