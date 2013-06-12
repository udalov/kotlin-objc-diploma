import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Library;
import org.bridj.objc.NSObject;

@Library("test")
public class IncreaseCounter extends NSObject {
    static {
        BridJ.register();
    }

    public static native Pointer<IncreaseCounter> alloc();

    @SuppressWarnings("unchecked")
    public native Pointer<IncreaseCounter> init();

    public native void increase();

    public native int get();
}
