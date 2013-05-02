package jet.runtime.objc;

@SuppressWarnings("UnusedDeclaration")
public abstract class ObjCObject {
    protected final ID id;

    protected ObjCObject(ID id) {
        this.id = id;
    }
}
