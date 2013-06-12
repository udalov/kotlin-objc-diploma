import org.rococoa.ObjCClass;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSObject;

public abstract class Singleton extends NSObject {
    public static _class_ CLASS = Rococoa.createClass("Singleton", _class_.class);

    public static abstract class _class_ implements ObjCClass {
        public abstract void createInstance();

        public abstract Singleton getInstance();
    }
}
