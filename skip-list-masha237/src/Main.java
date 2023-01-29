import mpp.skiplist.SkipList;

public class Main {
    public static void main(String[] args) {
        var x = new SkipList<Integer>();
        printElements(x);
        System.out.println(x.add(2));
        printElements(x);
        System.out.println(x.add(2));
        printElements(x);
        System.out.println(x.add(0));
        printElements(x);
        System.out.println(x.add(4));
        printElements(x);
        System.out.println(x.add(4));
        printElements(x);
        System.out.println(x.remove(2));
        printElements(x);

        System.out.println(x.add(1));
        printElements(x);
        System.out.println(x.add(3));
        printElements(x);
        System.out.println(x.remove(1));
        printElements(x);
        System.out.println(x.remove(2));
        printElements(x);
        System.out.println(x.add(2));
        printElements(x);
        System.out.println(x.add(5));
        printElements(x);
//        x.add(10);
    }

    private static void printElements(final SkipList<Integer> x) {
        for (int i = 0; i < 10; i++) {
            System.out.print(x.contains(i) ? "+" : "-");
        }
        System.out.println();
    }

}