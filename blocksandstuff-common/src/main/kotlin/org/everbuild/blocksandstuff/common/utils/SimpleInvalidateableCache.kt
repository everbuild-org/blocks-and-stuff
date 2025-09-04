package org.everbuild.blocksandstuff.common.utils

class SimpleInvalidatableCache<K, V>(val builder: (K) -> V) {
    private val cache = mutableMapOf<K, V>()

    operator fun get(key: K): V {
        return cache.getOrPut(key) { builder(key) }
    }

    fun invalidate(key: K) = cache.remove(key)
}