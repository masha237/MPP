package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = MultiQueue(workers, NODE_DISTANCE_COMPARATOR) // TODO replace me with a multi-queue based PQ!
    q.insert(start)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    val proc = AtomicInteger(1)
    repeat(workers) {
        thread {
            while (true) {
                val v = q.delete();
                if (v == null && proc.get() == 0) {
                    break;
                } else if (v == null) {
                    continue;
                }

                for (e in v.outgoingEdges) {
                    while (true) {

                        val curD = e.to.distance;
                        val newD = v.distance + e.weight;

                        if (newD < curD) {
                            if (e.to.casDistance(curD, newD)) {
                                q.insert(e.to);
                                proc.getAndIncrement();
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }

                proc.getAndDecrement();
            }

            onFinish.arrive()
        }
    }

    onFinish.arriveAndAwaitAdvance()
}

class MultiQueue(workers: Int, comparator: Comparator<Node>) {
    private val q = ArrayList<PriorityQueue<Node>>()
    private val sz = 2 * workers

    init {
        for (i in 0 until sz) {
            q.add(PriorityQueue(comparator))
        }
    }

    fun insert(task: Node) {
        val qI = (ThreadLocalRandom.current().nextInt() % sz + sz) % sz;
        synchronized(q[qI]) {
            q[qI].add(task);
        }
    }

    fun delete(): Node? {
        for (i in 0 until 100) {
            var qI1 = (ThreadLocalRandom.current().nextInt() % sz + sz) % sz;
            var qI2 = (ThreadLocalRandom.current().nextInt() % sz + sz) % sz;
            if (qI1 == qI2) {
                continue
            }
            if (qI1 > qI2) {
                val t = qI1;
                qI1 = qI2;
                qI2 = t;
            }

            synchronized(q[qI1]) {
                synchronized(q[qI2]) {
                    val e1 = q[qI1].peek();
                    val e2 = q[qI2].peek();
                    if (e1 != null && e2 != null && e1.distance < e2.distance) {
                        q[qI1].poll();
                        return e1
                    } else if (e1 != null && e2 != null) {
                        q[qI2].poll();
                        return e2
                    }
                }
            }
        }
        for (i in 0 until sz) {
            synchronized(q[i]) {
                if (q[i].size != 0) {
                    return q[i].poll()
                }
            }
        }
        return null;
    }
}