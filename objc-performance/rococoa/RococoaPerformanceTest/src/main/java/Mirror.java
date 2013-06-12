import org.rococoa.ObjCClass;
import org.rococoa.ObjCObject;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSObject;

public abstract class Mirror extends NSObject {
    public static _class_ CLASS = Rococoa.createClass("Mirror", _class_.class);

    public static abstract class _class_ implements ObjCClass {
        public abstract Mirror alloc();

        public abstract ObjCObject get(ObjCObject object);
    }

    public abstract Mirror init();
}
