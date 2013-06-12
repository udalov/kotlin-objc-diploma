import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Library;
import org.bridj.objc.NSObject;

@Library("test")
public class Singleton extends NSObject {
    static {
        BridJ.register();
    }

    public static native void createInstance();

    public static native Pointer<Singleton> getInstance();
}
