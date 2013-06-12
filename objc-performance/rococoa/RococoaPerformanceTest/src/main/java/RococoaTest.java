import com.sun.jna.Callback;

public class RococoaTest {
    public static final int TRIES = 100000;

    private void runIncreaseCounter() {
        IncreaseCounter x = IncreaseCounter.CLASS.alloc().init();

        assert x.get() == 0 : "Starting x " + x.get();
        for (int i = 0; i < TRIES; i++) {
            x.increase();
        }
        assert x.get() == TRIES : "Ending x " + x.get();
    }

    private void runSumOfManyPrimitives() {
        for (int i = 0; i < TRIES; i++) {
            SumOfManyPrimitives.CLASS.calculate_withShort_withLong_withFloat_withDouble(1, (short) 2, 3, 4, 5);
        }
    }

    private void runSingleton() {
        Singleton.CLASS.createInstance();

        for (int i = 0; i < TRIES; i++) {
            Singleton.CLASS.getInstance();
        }
    }

    private void runMirror() {
        Mirror m = Mirror.CLASS.alloc().init();

        for (int i = 0; i < TRIES; i++) {
            Mirror.CLASS.get(m);
        }
    }

    private void runInvokeCallback() {
        Callback c = new Callback() {
            public void callback() {
            }
        };

        for (int i = 0; i < TRIES; i++) {
            InvokeCallback.CLASS.invoke(c);
        }
    }

    public static void main(String[] args) {
        long start = System.nanoTime();
        TestLibrary.INSTANCE.toString();
        RococoaTest t = new RococoaTest();
        // t.runIncreaseCounter();
        // t.runSumOfManyPrimitives();
        // t.runSingleton();
        // t.runMirror();
        t.runInvokeCallback();
        long end = System.nanoTime();
        System.out.println(TRIES + ": " + (end-start) / 1e9);
    }
}
