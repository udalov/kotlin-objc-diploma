import com.sun.jna.*;

public interface TestLibrary extends Library {
    TestLibrary I = (TestLibrary) Native.loadLibrary("test", TestLibrary.class);

    Pointer objc_msgSend(Pointer receiver, Selector selector, Object... args);

    Selector sel_registerName(String name);

    Pointer objc_getClass(String name);
}
