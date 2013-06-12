import org.rococoa.ObjCClass;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSObject;

public abstract class IncreaseCounter extends NSObject {
    public static _class_ CLASS = Rococoa.createClass("IncreaseCounter", _class_.class);

    public static abstract class _class_ implements ObjCClass {
        public abstract IncreaseCounter alloc();
    }

    public abstract IncreaseCounter init();


    public abstract void increase();

    public abstract int get();
}
