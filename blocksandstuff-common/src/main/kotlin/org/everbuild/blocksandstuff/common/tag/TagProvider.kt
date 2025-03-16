package org.everbuild.blocksandstuff.common.tag

interface TagProvider<T> {
    fun hasTag(element: T, tag: String): Boolean
    fun getTaggedWith(tag: String): Set<T>
}