package mpp.linkedlistset

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic

class LinkedListSet<E : Comparable<E>> {

    private val last = Node<E>(element = null, next = AtomicRefData(node = null, false))
    private val first = Node<E>(element = null, next = AtomicRefData(node = last, false))

    private val head = atomic(first)

    /**
     * Adds the specified element to this set
     * if it is not already present.
     *
     * Returns `true` if this set did not
     * already contain the specified element.
     */
    fun add(element: E): Boolean {
        while (true) {
            val window = findWindow(element);
            if (window.next!!.element == element) {
                return false
            }
            val node = Node(element, AtomicRefData(window.next, false));
            if (window.cur!!.next.CAS(window.next, false, node, false)) {
                return true;
            }
        }
    }

    /**
     * Removes the specified element from this set
     * if it is present.
     *
     * Returns `true` if this set contained
     * the specified element.
     */
    fun remove(element: E): Boolean {
        while (true) {
            val window = findWindow(element);
            if (window.next!!.element != element) {
                return false
            }
            val node = window.next.next.data.value.node!!
            if (window.next.next.CAS(node, false, node, true)) {
                window.cur!!.next.CAS(window.next, false, node, false);
            }
        }
    }

    /**
     * Returns `true` if this set contains
     * the specified element.
     */
    fun contains(element: E): Boolean {
        val window = findWindow(element);
        return window.next?.element == element;
    }

    fun cmp(node: Node<E>?, x: E): Boolean {
        when (node) {
            first -> {
                return true;
            }
            last -> {
                return false;
            }
            else -> {
                return node!!.element!! < x;
            }
        }
    }
    private fun findWindow(x: E): Window<E> {
        while (true) {
            var cur = head.value;
            var next = cur.next.data.value.node;
            var fl2 = false;
            while (cmp(next, x)) {
                val data: AtomicRefData.AtomicData<E> = next!!.next.data.value
                val fl = data.boolean
                val node = data.node;

                if (!fl) {
                    cur = next;
                    next = cur.next.data.value.node;
                } else {
                    if (!cur.next.CAS(next, false, node, false)) {
                        fl2 = true;
                        break
                    } else {
                        next = node
                    }
                }
            }
            if (fl2) {
                continue
            }
            val data: AtomicRefData.AtomicData<E> = next!!.next.data.value
            val fl = data.boolean
            val node = data.node;

            if (!fl) {
                return Window(cur, next);
            }
            cur.next.CAS(next, false, node, false);
        }
    }
}

data class Window<E : Comparable<E>> (val cur: Node<E>?, val next: Node<E>?)


class AtomicRefData<E : Comparable<E>>(node: Node<E>?, boolean: Boolean) {
    data class AtomicData<E : Comparable<E>>(val node: Node<E>?, val boolean: Boolean)

    val data: AtomicRef<AtomicData<E>> = atomic(AtomicData(node, boolean));
    fun CAS(oldNode: Node<E>?, oldFlag: Boolean, newNode: Node<E>?, newFlag: Boolean): Boolean {
        val curData = data.value
        return oldNode == curData.node && oldFlag == curData.boolean && ((newNode == curData.node && newFlag == curData.boolean) || data.compareAndSet(curData, AtomicData(newNode, newFlag)))
    }
}

class Node<E : Comparable<E>>(element: E?, next: AtomicRefData<E>) {


    private val _element = element // `null` for the first and the last nodes
    val element get() = _element

    private val _next = atomic(next)
    val next get() = _next.value

}
