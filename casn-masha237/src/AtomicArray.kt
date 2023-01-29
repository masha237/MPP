@file:Suppress("UNCHECKED_CAST")

import kotlinx.atomicfu.*

class AtomicArray<E>(size: Int, initialValue: E) {
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
            if (expected1 == expected2) {
                return cas(index1, expected1, update2);
            }
            return false
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

    }
    abstract class Descriptor {
        abstract fun complete()

    }
    class RDCSSDescriptor<A>(
        val a: Ref<A>, val expectA: A, val updateA: A,
        val b: Ref<State>, val expectB: State,
    ) : Descriptor() {
        val state: AtomicRef<State> = atomic(State.UNDECIDED);
        override fun complete() {
            val update = if (b.v.value === expectB)
                State.SUCCESS else State.FAIL
            state.compareAndSet(State.UNDECIDED, update);
            if (state.value == State.SUCCESS) {
                a.v.compareAndSet(this, updateA);
            } else {
                a.v.compareAndSet(this, expectA);
            }
        }
    }

    class CAS2Descriptor<A, B>(
        val a: Ref<A>, val expectA: A, val updateA: A,
        val b: Ref<B>, val expectB: B, val updateB: B
    ) : Descriptor() {
        val state: Ref<State> = Ref(State.UNDECIDED);
        override fun complete() {
            while (true) {
                if (state.value != State.UNDECIDED) break;
                val curValue = b.v.value;
                if (curValue == this) {
                    state.cas(State.UNDECIDED, State.SUCCESS);
                    break;
                } else if (curValue == expectB) {
                    val desc = RDCSSDescriptor(b, expectB, this as B, state, State.UNDECIDED);
                    b.v.compareAndSet(expectB, desc);
                } else if (curValue is Descriptor) {
                    curValue.complete();
                } else {
                    state.cas(State.UNDECIDED, State.FAIL);
                    break;
                }
            }
            if (state.value == State.SUCCESS) {
                a.v.compareAndSet(this, updateA)
                b.v.compareAndSet(this, updateB)
            } else {
                a.v.compareAndSet(this, expectA)
                b.v.compareAndSet(this, expectB)
            }

        }
    }
    enum class State {
        UNDECIDED, SUCCESS, FAIL
    }
}