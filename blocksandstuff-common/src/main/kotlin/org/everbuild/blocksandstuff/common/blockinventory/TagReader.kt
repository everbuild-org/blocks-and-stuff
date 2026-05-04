package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.tag.Tag
import net.minestom.server.tag.TagReadable

interface TagReader {
    fun <T> getTag(tag: Tag<T>): T

    class Readable(val readable: TagReadable) : TagReader {
        override fun <T> getTag(tag: Tag<T>): T = readable.getTag(tag)
    }
}