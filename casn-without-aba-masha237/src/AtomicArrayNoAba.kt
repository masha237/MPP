@file:Suppress("UNCHECKED_CAST")

import kotlinx.atomicfu.*

class AtomicArrayNoAba<E>(size: Int, initialValue: E) {
    private val a = Array(size) { Ref(initialValue) }

    init {
        for (i in 0 until size) a[i].value = initialValue
    }

    fun get(index: Int) =
        a[index].value!!

    fun cas(index: Int, expected: E, update: E) =
        a[index].cas(expected, update)

    fun cas2(index1: Int, expected1: E, update1: E,
             index2: Int, expected2: E, update2: E): Boolean {

        if (index1 == index2) {
            return cas(index1, expected1, (update1 as Int + 1) as E);
        }

        val descriptor:CAS2Descriptor<E, E>;
        if (index1 < index2) {
            descriptor = CAS2Descriptor(a[index1], expected1, update1, a[index2], expected2, update2)
        } else {
            descriptor = CAS2Descriptor(a[index2], expected2, update2, a[index1], expected1, update1)
        }
        if (!descriptor.a.cas(descriptor.expectA, descriptor)) {
            return false;
        } else {
            descriptor.complete();
            return descriptor.state.value == State.SUCCESS;
        }
    }
    class Ref<T>(initial: T) {
        val v = atomic<Any?>(initial)

        var value: T
            get()  {
                v.loop { cur ->
                    when(cur) {
                        is Descriptor -> cur.complete()
                        else -> return cur as T
                    }
                }

            }

            set(upd) {
                v.loop { cur ->
                    when(cur) {
                        is Descriptor -> cur.complete()
                        else -> if (v.compareAndSet(cur, upd))
                            return
                    }
                }
            }

        fun cas(expect: Any?, update: Any?): Boolean {
            v.loop { cur ->
                if (cur is Descriptor) {
                    cur.complete()
                } else if (expect != cur) {
                    return false
                } else if (v.compareAndSet(cur, update)) {
                    return true
                }
            }
        }

        fun cas(element: AtomicRef<Any?>, expect: Any?, update: Any?): Boolean {
            return v.compareAndSet(expect, update);
        }
    }
    abstract class Descriptor {
        abstract fun complete(): Boolean

    }

   /* fun <A,B> dcss(
        a: Ref<A>, expectA: A, updateA: A,
        b: Ref<B>, expectB: B, updateB: B): Boolean {
        val descriptor = RDCSSDescriptor(a, expectA, updateA, b, expectB, updateB);
        if (a.cas(expectA, descriptor)) {
            return descriptor.complete();
        } else {
            return false;
        }
    }*/
    class RDCSSDescriptor<A, B>(
        val a: Ref<A>, val expectA: A, val updateA: A,
        val b: Ref<B>, val expectB: B
    ) : Descriptor() {
       val state: AtomicRef<State> = atomic(State.UNDECIDED);
        override fun complete(): Boolean {
            val update = if (b.value === expectB)
                updateA else expectA
            return a.v.compareAndSet(this, update)
        }
    }

    class CAS2Descriptor<A, B>(
        val a: Ref<A>, val expectA: A, val updateA: A,
        val b: Ref<B>, val expectB: B, val updateB: B
    ) : Descriptor() {
        val state: AtomicRef<State> = atomic(State.UNDECIDED);
        override fun complete(): Boolean {
            if (b.v.compareAndSet(expectB, this) ) {
                state.compareAndSet(State.UNDECIDED, State.SUCCESS);
            } else {
                state.compareAndSet(State.UNDECIDED, State.FAIL);
            }
            if (state.value == State.SUCCESS) {
                a.v.compareAndSet(this, updateA)
                b.v.compareAndSet(this, updateB)
                return true
            } else {
                a.v.compareAndSet(this, expectA)
                b.v.compareAndSet(this, expectB)
                return false
            }
        }
    }
    enum class State {
        UNDECIDED, SUCCESS, FAIL
    }
}