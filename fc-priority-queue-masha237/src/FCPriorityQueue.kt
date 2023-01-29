import kotlinx.atomicfu.*
import java.util.*

enum class OperName {
    POLL, PEEK, ADD, DONE, INPROC
}
data class Oper<E>(val name: OperName, val element: E?)

class FCPriorityQueue<E : Comparable<E>> {
    private val q = PriorityQueue<E>()
    private val locked = atomic(false)
    private val operSize = 4 * Thread.activeCount();
    private val operations = atomicArrayOfNulls<Oper<E>?>(operSize)

    fun tryLock() = locked.compareAndSet(expect = false, update = true)

    fun unlock() {
        locked.compareAndSet(true, false)
    }

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        return addOperation(Oper(OperName.POLL, null));
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        return addOperation(Oper(OperName.PEEK, null));
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        addOperation(Oper(OperName.ADD, element));
    }

    private fun addOperation(oper: Oper<E>): E? {
        while (true) {
            if (tryLock()) {
                var ans: E? = null;
                if (oper.name == OperName.PEEK) {
                    ans = q.peek();
                }
                if (oper.name == OperName.POLL) {
                    ans = q.poll();
                }
                if (oper.name == OperName.ADD) {
                    q.add(oper.element);
                }
                for (i in 0 until operSize) {
                    val o = operations[i].value ?: continue;
                    val res = Oper(OperName.INPROC, o.element)
                    if (o.name == OperName.DONE || o.name == OperName.INPROC) {
                        continue;
                    }
                    if (operations[i].compareAndSet(o, res)) {
                        var ans1: E? = null;
                        if (o.name == OperName.ADD) {
                            q.add(o.element)
                        } else if (o.name == OperName.POLL) {
                            ans1 = q.poll();
                        } else if (o.name == OperName.PEEK) {
                            ans1 = q.peek();
                        }
                        if (!operations[i].compareAndSet(res, Oper(OperName.DONE, ans1))) {
                            error("takogo ne bivaet");
                        }

                    }
                }
                unlock();
                return ans;
            } else {
                var ans: Pair<Boolean, E?> = Pair(false, null);
                for (i in 0 until operSize) {
                    if (operations[i].compareAndSet(null, oper)) {
                        var t = 0;
                        while (t < 10) {
                            t++;
                            val o = operations[i].value;
                            if (o != null && o.name == OperName.DONE && operations[i].compareAndSet(o, null)) {
                                return o.element;
                                break;
                            } else if (o != null && o.name == OperName.INPROC) {
                                t = 0;
                            }
                        }
                        if (ans.first) {
                            return ans.second
                        }
                        if (!operations[i].compareAndSet(oper, null)) {
                            while (true) {
                                val e = operations[i].value ?: error("takogo ne bivaet");
                                if (e.name == OperName.DONE) {
                                    ans = Pair(true, e.element);
                                    operations[i].value = null;
                                    break;
                                }
                            };
                        }

                        break;
                    }
                }
                if (ans.first) {
                    return ans.second;
                }
            }
        }

    }


}