import com.sun.jna.Library;
import com.sun.jna.Native;

public interface TestLibrary extends Library {
    TestLibrary INSTANCE = (TestLibrary) Native.loadLibrary("test", TestLibrary.class);
}
