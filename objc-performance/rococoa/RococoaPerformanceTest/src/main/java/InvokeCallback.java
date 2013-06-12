import com.sun.jna.Callback;
import org.rococoa.ID;
import org.rococoa.ObjCClass;
import org.rococoa.ObjCObject;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSObject;

public abstract class InvokeCallback extends NSObject {
    public static _class_ CLASS = Rococoa.createClass("InvokeCallback", _class_.class);

    public static abstract class _class_ implements ObjCClass {
        public abstract void invoke(Callback object);
    }
}
