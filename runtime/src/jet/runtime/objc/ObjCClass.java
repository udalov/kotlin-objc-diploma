package jet.runtime.objc;

@SuppressWarnings("UnusedDeclaration")
public abstract class ObjCClass {
    private final String className;
    private ID id;

    protected ObjCClass(String className) {
        this.className = className;
    }

    protected ID getId() {
        if (id == null) {
            id = Native.objc_getClass(className);
        }
        return id;
    }
}
