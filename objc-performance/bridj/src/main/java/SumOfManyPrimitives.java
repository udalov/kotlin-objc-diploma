import org.bridj.BridJ;
import org.bridj.ann.Library;
import org.bridj.objc.NSObject;

@Library("test")
public class SumOfManyPrimitives extends NSObject {
    static {
        BridJ.register();
    }

    public static native double calculate_withShort_withLong_withFloat_withDouble(
            int i, short s, long j, float f, double d
    );
}
