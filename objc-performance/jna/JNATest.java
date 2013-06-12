import com.sun.jna.*;

public class JNATest {
    public static final int TRIES = 100000;

    private Pointer sendMessage(Pointer object, String selectorName, Object... args) {
        Selector selector = TestLibrary.I.sel_registerName(selectorName);
        return TestLibrary.I.objc_msgSend(object, selector, args);
    }

    private long sendMessageLong(Pointer object, String selector, Object... args) {
        Pointer p = sendMessage(object, selector, args);
        return Pointer.nativeValue(p);
    }

    private Pointer classObject(String name) {
        return TestLibrary.I.objc_getClass(name);
    }

    private void runIncreaseCounter() {
        Pointer p = sendMessage(classObject("IncreaseCounter"), "alloc");
        p = sendMessage(p, "init");

        assert sendMessageLong(p, "get") == 0;
        for (int i = 0; i < TRIES; i++) {
            sendMessage(p, "increase");
        }
        assert sendMessageLong(p, "get") == TRIES;
    }

    private void runSumOfManyPrimitives() {
        Pointer p = classObject("SumOfManyPrimitives");

        for (int i = 0; i < TRIES; i++) {
            sendMessage(p, "calculate:withShort:withLong:withFloat:withDouble:", 1, 2, 3, 4, 5);
        }
    }

    private void runSingleton() {
        Pointer p = classObject("Singleton");

        sendMessage(p, "createInstance");
        for (int i = 0; i < TRIES; i++) {
            sendMessage(p, "getInstance");
        }
    }

    private void runMirror() {
        Pointer p = classObject("Mirror");
        Pointer q = sendMessage(p, "alloc");
        q = sendMessage(q, "init");

        for (int i = 0; i < TRIES; i++) {
            sendMessage(p, "get:", q);
        }
    }

    private void runInvokeCallback() {
        Pointer p = classObject("InvokeCallback");
        Callback callback = new Callback() {
            public void callback() {
            }
        };
        for (int i = 0; i < TRIES; i++) {
            sendMessage(p, "invoke:", callback);
        }
    }

    public static void main(String[] args) {
        long start = System.nanoTime();
        JNATest t = new JNATest();
        // t.runIncreaseCounter();
        // t.runSumOfManyPrimitives();
        // t.runSingleton();
        // t.runMirror();
        t.runInvokeCallback();
        long end = System.nanoTime();
        System.out.println(TRIES + ": " + ((end - start) / 1e9));
    }
}
