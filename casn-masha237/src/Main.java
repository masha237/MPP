public class Main {
    public static void main(String[] args) {
        var x = new AtomicArray<Integer>(5, 0);
        System.out.println(x.cas2(0, 0, 1, 1, 0, 2));
    }
}
