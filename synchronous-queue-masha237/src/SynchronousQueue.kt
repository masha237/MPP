import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * An element is transferred from sender to receiver only when [send] and [receive]
 * invocations meet in time (rendezvous), so [send] suspends until another coroutine
 * invokes [receive] and [receive] suspends until another coroutine invokes [send].
 */
class SynchronousQueue<E> {

    private val head: AtomicRef<Node>
    private val tail: AtomicRef<Node>


    init {
        val dummy = Node(Pair(null, null))
        head = atomic(dummy)
        tail = atomic(dummy)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(t: Node, x: Continuation<Any?>, e: E?): Boolean {
        val node = Node(Pair(x, e));
        if (t.next.compareAndSet(null, node)) {
            tail.compareAndSet(t, node);
            return true;
        } else {
            tail.compareAndSet(t, t.next.value!!)
            return false;
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    fun dequeue(h: Node): Node? {
        val newH = h.next.value!!;
        if (head.compareAndSet(h, newH)) {
            return newH;
        } else {
            return null;
        }
    }

    fun isEmpty(): Boolean {
        val curHead = head.value;
        val curHeadNext = curHead.next.value;
        return curHeadNext === null
    }

    /**
     * Sends the specified [element] to this channel, suspending if there is no waiting
     * [receive] invocation on this channel.
     */
    suspend fun send(element: E): Unit {
        while (true) {
            val t = tail.value;
            val h = head.value;
            if (h.next.value == null  || h.next.value!!.x.second != null) {
                val r = suspendCoroutine { cont ->
                    if (!enqueue(t, cont, element)) {
                        cont.resume(RETRY)
                    }
                }
                if (r == RETRY) {
                    continue
                }
                return;
            } else {
                val r = dequeue(h);
                if (r != null) {
                    r.x.first?.resume(element)
                    return;
                };
            }
        }
    }

    /**
     * Retrieves and removes an element from this channel if there is a waiting [send] invocation on it,
     * suspends the caller if this channel is empty.
     */
    suspend fun receive(): E {
        while (true) {
            val t = tail.value;
            val h = head.value;
            if (h.next.value == null || h.next.value!!.x.second == null) {
                val r = suspendCoroutine { cont ->
                    if (!enqueue(t, cont, null)) {
                        cont.resume(RETRY)
                    }
                }
                if (r == RETRY) {
                    continue
                }
                return r as E;
            } else {
                val r = dequeue(h);
                if (r != null) {
                    r.x.first?.resume(Unit);
                    return r.x.second as E
                };
            }
        }
    }

}
class Node(val x: Pair<Continuation<Any?>?, Any?>) {
    val next = atomic<Node?>(null)
}
object RETRY;
