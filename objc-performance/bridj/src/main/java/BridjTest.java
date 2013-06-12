import org.bridj.Pointer;

public class BridjTest {
    public static final int TRIES = 100000;

    private void runIncreaseCounter() {
        IncreaseCounter x = IncreaseCounter.alloc().get().init().get();
        assert x.get() == 0 : "Starting x " + x.get();
        for (int i = 0; i < TRIES; i++) {
            x.increase();
        }
        assert x.get() == TRIES : "Ending x " + x.get();
    }

    private void runSumOfManyPrimitives() {
        for (int i = 0; i < TRIES; i++) {
            SumOfManyPrimitives.calculate_withShort_withLong_withFloat_withDouble(1, (short) 2, 3, 4, 5);
        }
    }

    private void runSingleton() {
        Singleton.createInstance();

        for (int i = 0; i < TRIES; i++) {
            Singleton.getInstance();
        }
    }

    private void runMirror() {
        Pointer<Mirror> m = Mirror.alloc().get().init();
        for (int i = 0; i < TRIES; i++) {
            Mirror.get(m);
        }
    }

    private void runInvokeCallback() {
        InvokeCallback.CallbackParam c = new InvokeCallback.CallbackParam() {
            public void invoke() { }
        };
        Pointer p = c.toPointer();
        for (int i = 0; i < TRIES; i++) {
            InvokeCallback.invoke(p);
        }
    }

    public static void main(String[] args) {
        long start = System.nanoTime();
        BridjTest t = new BridjTest();
        // t.runIncreaseCounter();
        // t.runSumOfManyPrimitives();
        // t.runSingleton();
        // t.runMirror();
        t.runInvokeCallback();
        long end = System.nanoTime();
        System.out.println(TRIES + ": " + ((end - start) / 1e9));
    }
}
