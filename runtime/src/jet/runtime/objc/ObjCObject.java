package jet.runtime.objc;

@SuppressWarnings("UnusedDeclaration")
public abstract class ObjCObject {
    public final ID id;

    protected ObjCObject(ID id) {
        this.id = id;
    }
}
