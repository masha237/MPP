package mpp.dynamicarray

import kotlinx.atomicfu.*

interface DynamicArray<E> {

    /**
     * Returns the element located in the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the [size] of this array.
     */
    fun get(index: Int): E

    /**
     * Puts the specified [element] into the cell [index],
     * or throws [IllegalArgumentException] if [index]
     * exceeds the [size] of this array.
     */
    fun put(index: Int, element: E)

    /**
     * Adds the specified [element] to this array
     * increasing its [size].
     */
    fun pushBack(element: E)

    /**
     * Returns the current size of this array,
     * it increases with [pushBack] invocations.
     */
    val size: Int
}

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<Any>(INITIAL_CAPACITY))
    private val sz = atomic(0);
    override fun get(index: Int): E {
        require(index in 0 until size) {"invalid index"};
        while (true) {
            val curCore = core.value;
            val curSZ = size;
            if (curCore.next.value != null) {
                move(curCore, curSZ);
            }
            val newCore = core.value;
            if (newCore.next.value != null) {
                continue;
            }
            val e = newCore.array[index].value!!;
            if (e is Moved) {
                return e.el as E
            }
            return e as E;
        }
    }

    override fun put(index: Int, element: E) {

        require(index in 0 until size) {"invalid index"};
        while (true) {
            val curCore = core.value;
            val curSZ = size;
            if (curCore.next.value != null) {
                move(curCore, curSZ);
            }
            val newCore = core.value;
            if (newCore.next.value != null) {
                continue;
            }
            val cur = newCore.array[index].value;
            if (cur is Moved) {
                continue
            }
            if (core.value.array[index].compareAndSet(cur, element)) {
                return
            }
        }


    }

    override fun pushBack(element: E) {
        while (true) {
            var curCore = core.value;
            var curSZ = size;
            if (curCore.next.value != null) {
                move(curCore, curSZ);
            }
            curCore = core.value;
            if (curCore.next.value != null) {
                continue;
            }
            while (true) {
                curSZ = size
                val curCapacity = curCore.capacity;
                if (curCapacity <= curSZ) {
                    val newCore = Core<Any>(curCapacity * 2);
                    curCore.next.compareAndSet(null, newCore);
                    break
                }
                if (core.value.array[curSZ].compareAndSet(null, element)) {

                    sz.compareAndSet(curSZ, curSZ + 1);
                    return
                }
                sz.compareAndSet(curSZ, curSZ + 1);
                break
            }
        }

    }

    private fun move(curCore: Core<Any>, curSZ: Int) {
        val next = curCore.next.value!!;
        for (i in 0 until curCore.capacity) {
            while (true) {
                val curE = curCore.array[i].value ?: error("kek");
                if (curE !is Moved) {
                    if (!curCore.array[i].compareAndSet(curE, Moved(curE))) {
                        continue
                    }
                    next.array[i].compareAndSet(null, curE)
                } else {
                    next.array[i].compareAndSet(null, curE.el);
                }
                break;
            }
        }
        core.compareAndSet(curCore, next);
    }

    override val size: Int
        get() = sz.value
}
private class Core<E>(
    val capacity: Int,
) {
    val next = atomic<Core<E>?>(null);
    val array = atomicArrayOfNulls<E>(capacity)
    private val _size = atomic(0)

    val size: Int get() = _size.value

    @Suppress("UNCHECKED_CAST")
    fun get(index: Int): E {
        require(index < size)
        return array[index].value as E
    }
}

class Moved(val el: Any)

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME