import org.rococoa.ObjCClass;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSObject;

public abstract class SumOfManyPrimitives extends NSObject {
    public static _class_ CLASS = Rococoa.createClass("SumOfManyPrimitives", _class_.class);

    public static abstract class _class_ implements ObjCClass {
        public abstract double calculate_withShort_withLong_withFloat_withDouble(
                int i, short s, long j, float f, double d
        );
    }
}
