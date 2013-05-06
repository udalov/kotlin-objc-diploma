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

    @Override
    public String toString() {
        return "[ObjCClass " + className + "]";
    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObjCClass)) return false;
        ObjCClass obj = (ObjCClass) o;
        return className == null ? obj.className == null : className.equals(obj.className);
    }
}
