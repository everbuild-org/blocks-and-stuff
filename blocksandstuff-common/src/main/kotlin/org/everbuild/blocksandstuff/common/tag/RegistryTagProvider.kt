package org.everbuild.blocksandstuff.common.tag

abstract class RegistryTagProvider<T>(type: String) : TagProvider<T> {
    private val values = mutableMapOf<String, MutableSet<T>>()

    private val initialLoad by lazy {
        loadAdditionalTags("blocksandstuff", RegistryTagProvider::class.java)
    }

    protected abstract fun map(key: String): T

    fun loadAdditionalTags(path: String, module: Class<*>) {
        val raw = TagRegistryLoader.loadTags(path, "block", module)
        raw.forEach { (k, v) ->
            values[k] = v.map { map(it) }.toMutableSet()
        }
    }

    override fun getTaggedWith(tag: String): Set<T> {
        initialLoad.javaClass
        values[tag]?.let { return it }
        return setOf()
    }

    override fun hasTag(element: T, tag: String): Boolean {
        initialLoad.javaClass
        return values[tag]?.contains(element) ?: false
    }
}