package jet.runtime.objc;

@SuppressWarnings("UnusedDeclaration")
public abstract class ObjCObject {
    public final ID id;

    protected ObjCObject(ID id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "[ObjCObject " + getClass().getName() + " " + id.toString() + "]";
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObjCObject)) return false;
        ObjCObject obj = (ObjCObject) o;
        return id == null ? obj.id == null : id.equals(obj.id);
    }
}
