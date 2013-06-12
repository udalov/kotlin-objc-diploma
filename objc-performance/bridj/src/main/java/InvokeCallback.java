import org.bridj.BridJ;
import org.bridj.Callback;
import org.bridj.Pointer;
import org.bridj.ann.Library;
import org.bridj.objc.NSObject;

@Library("test")
public class InvokeCallback extends NSObject {
    static {
        BridJ.register();
    }

    public static abstract class CallbackParam extends Callback {
        public abstract void invoke();
    }

    public static native void invoke(Pointer<CallbackParam> c);
}
