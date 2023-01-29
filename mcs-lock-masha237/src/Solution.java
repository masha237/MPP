import java.util.concurrent.atomic.*;

public class Solution implements Lock<Solution.Node> {
    private final Environment env;
    private final AtomicReference<Node> tail = new AtomicReference<>();

    // todo: необходимые поля (final, используем AtomicReference)

    public Solution(Environment env) {
        this.env = env;
    }

    @Override
    public Node lock() {
        Node my = new Node(); // сделали узел
        my.lock.set(true);
        var pred = tail.getAndSet(my);
        if (pred != null) {
            pred.next.set(my);
            while (my.lock.get()) {
                env.park();
            }
        }
        return my; // вернули узел
    }

    @Override
    public void unlock(Node node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return;
            } else {
                while (node.lock.get()) {

                }
            }

        }
        node.next.get().lock.set(false);
        env.unpark(node.next.get().thread);
    }

    static class Node {
        final Thread thread = Thread.currentThread(); // запоминаем поток, которые создал узел
        final AtomicReference<Boolean> lock = new AtomicReference<>(false);
        final AtomicReference<Node> next = new AtomicReference<>();
    }
}
