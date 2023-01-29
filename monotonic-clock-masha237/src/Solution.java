import org.jetbrains.annotations.NotNull;

/**
     * В теле класса решения разрешено использовать только финальные переменные типа RegularInt.
     * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
     *
     * @author Zhogova Mariia
     */
    public class Solution implements MonotonicClock {
    private final RegularInt c1 = new RegularInt(0);
    private final RegularInt c2 = new RegularInt(0);
    private final RegularInt c3 = new RegularInt(0);
    private final RegularInt c21 = new RegularInt(0);
    private final RegularInt c22 = new RegularInt(0);
    private final RegularInt c23 = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {
        // write right-to-left
        c21.setValue(time.getD1());
        c22.setValue(time.getD2());
        c23.setValue(time.getD3());

        c3.setValue(c23.getValue());
        c2.setValue(c22.getValue());
        c1.setValue(c21.getValue());
    }

    @NotNull
    @Override
    public Time read() {
        RegularInt r1 = new RegularInt(c1.getValue());
        RegularInt r2 = new RegularInt(c2.getValue());
        RegularInt r3 = new RegularInt(c3.getValue());
        RegularInt r22 = new RegularInt(c22.getValue());
        RegularInt r21 = new RegularInt(c21.getValue());

        if (r1.getValue() == r21.getValue() && r2.getValue() == r22.getValue()) {
            return new Time(r1.getValue(), r2.getValue(), r3.getValue());
        } else if (r1.getValue() != r21.getValue()) {
            return new Time(r21.getValue(), 0, 0);
        } else {
            return new Time(r21.getValue(), r22.getValue(), 0);
        }

    }

}
