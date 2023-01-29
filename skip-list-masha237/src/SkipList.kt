package mpp.skiplist

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.atomic.AtomicReference

class SkipList<E : Comparable<E>> () {

    private val head: Node<E> = Node(null, MAX_LEVEL)
    private val tail: Node<E> =  Node(null, MAX_LEVEL)
    private val rnd = ThreadLocalRandom.current();

    init {
        for (i in 0 until MAX_LEVEL + 1) {
            head.next[i].set(tail, false)
        }
    }

    /**
     * Adds the specified element to this set
     * if it is not already present.
     *
     * Returns `true` if this set did not
     * already contain the specified element.
     */
    fun add(element: E): Boolean {
        val topLevel = rnd.nextInt(MAX_LEVEL);
        val bottomLevel = 0;
        while (true) {
            var window: Window<E> = find(element);
            if (window.found) {
                return false;
            } else {
                val newNode = Node(element, topLevel);
                for (level in bottomLevel until topLevel + 1) {
                    val succ = window.succ[level]
                    newNode.next[level].set(succ as Node<E>?, false);
                }
                var pred: Node<E>? = window.pred[bottomLevel] as Node<E>?;
                var succ: Node<E>? = window.succ[bottomLevel] as Node<E>?;
                newNode.next[bottomLevel].set(succ, false);
                if (!pred!!.next[bottomLevel].compareAndSet(succ, newNode, false, false)) {
                    continue
                }
                for (level in bottomLevel + 1 until topLevel + 1) {
                    while (true) {
                        pred = window.pred[level] as Node<E>?;
                        succ = window.succ[level] as Node<E>?
                        if (pred!!.next[level].compareAndSet(succ, newNode, false, false)) {
                            break
                        }
                        window = find(element);
                    }
                }
                return true
            }
        }
    }

    private fun find(element: E): Window<E> {
        val bottomLevel = 0;
        val window = Window<E>(false)
        var snip : Boolean;
        var pred : Node<E>? = null;
        var cur : Node<E>? = null;
        var succ : Node<E>? = null;
        val marked = BooleanArray(1) {false}
        retry@ while (true) {
            pred = head;
            for (level in MAX_LEVEL downTo bottomLevel) {
                cur = pred!!.next[level].reference;
                while (true) {
                    succ = cur!!.next[level].get(marked)
                    while (marked[0]) {
                        snip = pred!!.next[level].compareAndSet(cur , succ, false, false);
                        if (!snip) {
                            continue@retry
                        }
                        cur = pred.next[level].reference
                        succ = pred.next[level].get(marked)
                    }
                    if (cmp(cur, element)) {
                        pred = cur
                        cur = succ
                    } else {
                        break
                    }

                }
                window.pred[level] = pred
                window.succ[level] = cur
            }
            window.found = cur!!.element == element

            return window
        }
    }

    fun cmp(node: Node<E>?, x: E): Boolean {
        when (node) {
            head -> {
                return true;
            }
            tail -> {
                return false;
            }
            else -> {
                return node!!.element!! < x;
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
        val bottomLevel = 0;
        var succ: Node<E>;
        while (true) {
            var window:Window<E> = find(element)
            if (!window.found) {
                return false
            } else {
                val nodeToRemove: Node<E> = window.succ[bottomLevel] as Node<E>;
                for (level in nodeToRemove.level downTo bottomLevel + 1) {
                    val marked = BooleanArray(1) { false }
                    succ = nodeToRemove.next[level].get(marked)!!;
                    while (!marked[0]) {
                        nodeToRemove.next[level].attemptMark(succ, true);

                        succ = nodeToRemove.next[level].get(marked)!!
                    }
                }
                val marked = BooleanArray(1) {false}
                succ = nodeToRemove.next[bottomLevel].get(marked)!!;
                while (true) {
                    val iMarked: Boolean = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ, false, true);
                    succ = (window.succ[bottomLevel] as Node<E>).next[bottomLevel].get(marked)!!;
                    if (iMarked) {
                        window = find(element)
                        return true
                    } else if (marked[0]) {
                        return false;
                    }
                }
            }

        }
    }

    /**
     * Returns `true` if this set contains
     * the specified element.
     */
    fun contains(element: E): Boolean {
        val bottomLevel = 0;
        val marked = BooleanArray(1) { false }
        var pred : Node<E>? = head;
        var cur : Node<E>? = null;
        var succ : Node<E>? = null;
        for (level in MAX_LEVEL downTo bottomLevel) {
            cur = pred!!.next[level].reference
            while (true) {
                succ = cur!!.next[level].get(marked);
                while (marked[0]) {
                    cur = pred!!.next[level].reference
                    succ = cur!!.next[level].get(marked);
                }
                if (cmp(cur, element)) {
                    pred = cur
                    cur = succ
                } else {
                    break
                }
            }
        }
        return cur!!.element == element
    }
}

private const val MAX_LEVEL = 5;

class Node<E>(val element: E?, val level: Int) {
    val next = MutableList(level + 1) { AtomicMarkableReference<Node<E>?>(null, false) }
}

class Window<E>(var found:Boolean) {
    var pred = arrayOfNulls<Any?>(MAX_LEVEL + 1);
    var succ = arrayOfNulls<Any?>(MAX_LEVEL + 1);
}

