import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Library;
import org.bridj.objc.NSObject;

@Library("test")
public class Mirror extends NSObject {
    static {
        BridJ.register();
    }

    public static native Pointer<Mirror> alloc();

    @SuppressWarnings("unchecked")
    public native Pointer<Mirror> init();

    public static native Pointer<Mirror> get(Pointer<Mirror> object);
}
