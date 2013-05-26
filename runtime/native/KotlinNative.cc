#include "KotlinNative.h"

#include <dlfcn.h>

#include <ffi.h>

#include <objc/message.h>
#include <objc/objc.h>
#include <objc/runtime.h>

#include <cassert>
#include <cstdio>
#include <memory>
#include <string>
#include <vector>

#define L2A(x) ((void *)(x))
#define A2L(x) ((jlong)(x))

const std::string OBJC_PACKAGE_PREFIX = "objc/";

// TODO: hide everything util under a namespace
// TODO: process all possible JNI errors
// TODO: delete local JNI references where there can be too many of them
// TODO: fail gracefully if any class/method/field is not found

class AutoJString {
    JNIEnv *const env;
    const jstring jstr;
    const char *const chars;

    public:

    AutoJString(JNIEnv *env, jstring jstr):
        env(env),
        jstr(jstr),
        chars(env->GetStringUTFChars(jstr, 0))
    { }

    ~AutoJString() {
        env->ReleaseStringUTFChars(jstr, chars);
    }

    const char *const str() const {
        return chars;
    }
};

// --------------------------------------------------------
// Classes, methods, fields cache
// --------------------------------------------------------

class JVMDeclarationsCache {
    JNIEnv *const env;

    public:

    jclass callbackFunctionClass;
    jclass objcObjectClass;
    jclass objcSelectorClass;
    jclass pointerClass;
    jclass primitiveValueClass;

    jfieldID callbackFunctionFunctionField;
    jfieldID callbackFunctionSignatureField;
    jfieldID objcObjectPointerField;
    jfieldID pointerPeerField;
    jfieldID primitiveValueValueField;

    jmethodID objectGetClassMethod;
    jmethodID classGetNameMethod;
    jfieldID integerValueField;
    jfieldID longValueField;
    jfieldID shortValueField;
    jfieldID floatValueField;
    jfieldID doubleValueField;
    jfieldID characterValueField;
    jfieldID booleanValueField;

    jmethodID objcSelectorConstructor;
    jmethodID pointerConstructor;
    jmethodID primitiveValueConstructor;

    JVMDeclarationsCache(JNIEnv *env): env(env) {
        callbackFunctionClass = findClass("jet/objc/CallbackFunction");
        objcObjectClass = findClass("jet/objc/ObjCObject");
        objcSelectorClass = findClass("jet/objc/ObjCSelector");
        pointerClass = findClass("jet/objc/Pointer");
        primitiveValueClass = findClass("jet/objc/PrimitiveValue");

        callbackFunctionFunctionField = env->GetFieldID(callbackFunctionClass, "function", "Ljava/lang/Object;");
        callbackFunctionSignatureField = env->GetFieldID(callbackFunctionClass, "signature", "Ljava/lang/String;");
        objcObjectPointerField = env->GetFieldID(objcObjectClass, "pointer", "J");
        pointerPeerField = env->GetFieldID(pointerClass, "peer", "J");
        primitiveValueValueField = env->GetFieldID(primitiveValueClass, "value", "J");

        jclass objectClass = findClass("java/lang/Object");
        jclass classClass = findClass("java/lang/Class");
        jclass integerClass = findClass("java/lang/Integer");
        jclass longClass = findClass("java/lang/Long");
        jclass shortClass = findClass("java/lang/Short");
        jclass floatClass = findClass("java/lang/Float");
        jclass doubleClass = findClass("java/lang/Double");
        jclass characterClass = findClass("java/lang/Character");
        jclass booleanClass = findClass("java/lang/Boolean");
        objectGetClassMethod = env->GetMethodID(objectClass, "getClass", "()Ljava/lang/Class;");
        classGetNameMethod = env->GetMethodID(classClass, "getName", "()Ljava/lang/String;");
        integerValueField = env->GetFieldID(integerClass, "value", "I");
        longValueField = env->GetFieldID(longClass, "value", "J");
        shortValueField = env->GetFieldID(shortClass, "value", "S");
        floatValueField = env->GetFieldID(floatClass, "value", "F");
        doubleValueField = env->GetFieldID(doubleClass, "value", "D");
        characterValueField = env->GetFieldID(characterClass, "value", "C");
        booleanValueField = env->GetFieldID(booleanClass, "value", "Z");

        objcSelectorConstructor = env->GetMethodID(objcSelectorClass, "<init>", "(J)V");
        pointerConstructor = env->GetMethodID(pointerClass, "<init>", "(J)V");
        primitiveValueConstructor = env->GetMethodID(primitiveValueClass, "<init>", "(J)V");
    }

    ~JVMDeclarationsCache() {
        // TODO: delete global references
    }

    private:

    jclass findClass(const char *name) {
        jclass localRef = env->FindClass(name);
        // TODO: figure out why JNA uses weak global references for this
        jclass globalRef = (jclass) env->NewGlobalRef(localRef);
        env->DeleteLocalRef(localRef);
        return globalRef;
    }
};

JVMDeclarationsCache *cache;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    // TODO: extract repeating code
    JNIEnv *env;
    int attached = vm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_OK;
    if (!attached) {
        if (vm->AttachCurrentThread((void **) &env, 0) != JNI_OK) {
            fprintf(stderr, "Error attaching native thread to VM on load\n");
            return 0;
        }
    }

    cache = new JVMDeclarationsCache(env);

    if (!attached) {
        vm->DetachCurrentThread();
    }

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *) {
    JNIEnv *env;
    int attached = vm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_OK;
    if (!attached) {
        if (vm->AttachCurrentThread((void **) &env, 0) != JNI_OK) {
            fprintf(stderr, "Error attaching native thread to VM on unload\n");
            return;
        }
    }

    delete cache;

    if (!attached) {
        vm->DetachCurrentThread();
    }
}



// --------------------------------------------------------
// Dynamic libraries
// --------------------------------------------------------

JNIEXPORT void JNICALL Java_jet_objc_Native_dlopen(
        JNIEnv *env,
        jclass,
        jstring path
) {
    AutoJString pathStr(env, path);
    if (!dlopen(pathStr.str(), RTLD_GLOBAL)) {
        // TODO: report an error properly
        fprintf(stderr, "Library not found: %s\n", pathStr.str());
        exit(42);
    }
}


// --------------------------------------------------------
// Pointers
// --------------------------------------------------------

JNIEXPORT jlong JNICALL Java_jet_objc_Native_malloc(
        JNIEnv *,
        jclass,
        jlong bytes
) {
    void *memory = malloc(bytes);
    return *(jlong *)&memory;
}

JNIEXPORT void JNICALL Java_jet_objc_Native_free(
        JNIEnv *,
        jclass,
        jlong pointer
) {
    free(*(void **)&pointer);
}

JNIEXPORT jlong JNICALL Java_jet_objc_Native_getWord(
        JNIEnv *,
        jclass,
        jlong pointer
) {
    return *(jlong *)pointer;
}

JNIEXPORT void JNICALL Java_jet_objc_Native_setWord(
        JNIEnv *,
        jclass,
        jlong pointer,
        jlong value
) {
    *(jlong *)pointer = value;
}


// --------------------------------------------------------
// Objective-C
// --------------------------------------------------------

JNIEXPORT jlong JNICALL Java_jet_objc_Native_objc_1getClass(
        JNIEnv *env,
        jclass,
        jstring name
) {
    AutoJString nameStr(env, name);
    id objcClass = objc_getClass(nameStr.str());
    return A2L(objcClass);
}


void *createNativeClosureForFunction(JNIEnv *env, jobject function, jstring signature);

void *coerceJVMNativeValueToNative(JNIEnv *env, jobject object) {
    if (env->IsInstanceOf(object, cache->callbackFunctionClass)) {
        jobject function = env->GetObjectField(object, cache->callbackFunctionFunctionField);
        jstring signature = (jstring) env->GetObjectField(object, cache->callbackFunctionSignatureField);
        return createNativeClosureForFunction(env, function, signature);
    } else if (env->IsInstanceOf(object, cache->pointerClass)) {
        return L2A(env->GetLongField(object, cache->pointerPeerField));
    } else if (env->IsInstanceOf(object, cache->objcObjectClass)) {
        return L2A(env->GetLongField(object, cache->objcObjectPointerField));
    } else if (env->IsInstanceOf(object, cache->primitiveValueClass)) {
        return L2A(env->GetLongField(object, cache->primitiveValueValueField));
    } else {
        fprintf(stderr, "Unsupported JVM object type\n");
        return 0;
    }
}

std::vector<void *> extractArgumentsFromJArray(JNIEnv *env, jobjectArray argArray) {
    jsize length = env->GetArrayLength(argArray);
    std::vector<void *> args;
    args.reserve(length);

    for (jsize i = 0; i < length; i++) {
        jobject arg = env->GetObjectArrayElement(argArray, i);
        args.push_back(coerceJVMNativeValueToNative(env, arg));
    }

    return args;
}

// TODO: figure out if an autorelease pool is needed and how to use it properly
/*
id createAutoreleasePool() {
    static id autoreleasePoolClass = objc_getClass("NSAutoreleasePool");
    static SEL alloc = sel_registerName("alloc");
    static SEL init = sel_registerName("init");
    id pool = objc_msgSend(autoreleasePoolClass, alloc);
    return objc_msgSend(pool, init);
}

void drainAutoreleasePool(id pool) {
    static SEL drain = sel_registerName("drain");
    objc_msgSend(pool, drain);
}
*/

jobject createMirrorObjectOfClass(JNIEnv *env, id object, jclass jvmClass) {
    // TODO: release in finalize
    static SEL retain = sel_registerName("retain");
    objc_msgSend(object, retain);

    jmethodID constructor = env->GetMethodID(jvmClass, "<init>", "(J)V");
    return env->NewObject(jvmClass, constructor, object);
}

// These qualifiers (Objective-C Runtime Type Encodings, Table 6-2) are discarded when decoding types:
// r const, n in, N inout, o out, O bycopy, R byref, V oneway
const std::string IGNORED_TYPE_ENCODINGS = "rnNoORV";

ffi_type *ffiTypeFromEncoding(char *encoding) {
    while (IGNORED_TYPE_ENCODINGS.find(*encoding) != std::string::npos) {
        encoding++;
    }
    switch (*encoding) {
        case _C_CHR: return &ffi_type_schar;
        case _C_INT: return &ffi_type_sint;
        case _C_SHT: return &ffi_type_sshort;
        case _C_LNG: return &ffi_type_slong;
        case _C_LNG_LNG: return &ffi_type_sint64;
        case _C_UCHR: return &ffi_type_uchar;
        case _C_UINT: return &ffi_type_uint;
        case _C_USHT: return &ffi_type_ushort;
        case _C_ULNG: return &ffi_type_ulong;
        case _C_ULNG_LNG: return &ffi_type_uint64;
        case _C_FLT: return &ffi_type_float;
        case _C_DBL: return &ffi_type_double;
        case _C_VOID: return &ffi_type_void;
        // TODO: structs, arrays, other types
        default: return &ffi_type_pointer;
    }
}

enum TypeKind {
    TYPE_VOID,
    TYPE_PRIMITIVE,
    TYPE_POINTER,
    TYPE_SELECTOR,
    TYPE_CLASS,
    TYPE_OBJECT,
};

TypeKind typeKindFromEncoding(char *encoding) {
    while (IGNORED_TYPE_ENCODINGS.find(*encoding) != std::string::npos) {
        encoding++;
    }
    char c = *encoding;
    
    if (c == 'v') return TYPE_VOID;
    if (c == ':') return TYPE_SELECTOR;
    if (c == '#') return TYPE_CLASS;
    if (c == '*' || c == '^') return TYPE_POINTER;

    if (ffiTypeFromEncoding(encoding) == &ffi_type_pointer) return TYPE_OBJECT;

    return TYPE_PRIMITIVE;
}

class MsgSendInvocation {
    const id receiver;
    const SEL selector;
    const std::vector<void *>& args;
    
    Method method;

    char *returnTypeEncoding;

    public:

    MsgSendInvocation(id receiver, SEL selector, const std::vector<void *>& args):
        receiver(receiver),
        selector(selector),
        args(args)
    {
        Class receiverClass = object_getClass(receiver);
        method = class_getInstanceMethod(receiverClass, selector);
        returnTypeEncoding = 0;
    }

    ~MsgSendInvocation() {
        if (returnTypeEncoding) {
            free(returnTypeEncoding);
        }
    }

    TypeKind returnTypeKind() const {
        return typeKindFromEncoding(returnTypeEncoding);
    }

    id invoke() {
        unsigned numArguments = method_getNumberOfArguments(method);

        std::vector<ffi_type *> argTypes;
        std::vector<void *> argValues;
        calculateArgumentTypesAndValues(numArguments, argTypes, argValues);

        returnTypeEncoding = method_copyReturnType(method);
        ffi_type *methodReturnType = ffiTypeFromEncoding(returnTypeEncoding);
        void (*fun)();
        ffi_type *returnType;
        if (methodReturnType == &ffi_type_double || methodReturnType == &ffi_type_float) {
            // From Objective-C Runtime Reference:
            // "On the i386 platform you must use objc_msgSend_fpret for functions returning non-integral type"
            fun = (void (*)()) objc_msgSend_fpret;
            returnType = &ffi_type_double;
        } else {
            fun = (void (*)()) objc_msgSend;
            returnType = &ffi_type_pointer;
        }

        ffi_cif cif;
        ffi_status status = ffi_prep_cif(&cif, FFI_DEFAULT_ABI, numArguments, returnType, &argTypes[0]);
        if (status != FFI_OK) {
            // TODO: throw a JVM exception
            fprintf(stderr, "ffi_prep_cif failed: %d\n", status);
            exit(42);
        }

        id result;
        ffi_call(&cif, fun, &result, &argValues[0]);

        return result;
    }

    private:

    void calculateArgumentTypesAndValues(unsigned size, std::vector<ffi_type *>& types, std::vector<void *>& values) {
        types.reserve(size);
        values.reserve(size);

        types.push_back(&ffi_type_pointer);
        values.push_back((void *) &receiver);

        types.push_back(&ffi_type_pointer);
        values.push_back((void *) &selector);

        for (unsigned i = 2; i < size; i++) {
            char *argTypeEncoding = method_copyArgumentType(method, i);
            ffi_type *type = ffiTypeFromEncoding(argTypeEncoding);

            types.push_back(type);
            values.push_back((void *) &args[i-2]);

            free(argTypeEncoding);
        }
    }
};

jobject coerceNativeToJVM(JNIEnv *env, id result, TypeKind kind) {
    if (kind == TYPE_VOID) {
        return NULL;
    } else if (kind == TYPE_PRIMITIVE) {
        return env->NewObject(cache->primitiveValueClass, cache->primitiveValueConstructor, result);
    } else if (kind == TYPE_SELECTOR) {
        return env->NewObject(cache->objcSelectorClass, cache->objcSelectorConstructor, result);
    } else if (kind == TYPE_CLASS) {
        // TODO: what if there's no such class object?
        std::string className = OBJC_PACKAGE_PREFIX + object_getClassName(result);
        std::string classObjectDescriptor = "L" + className + "$object;";
        jclass clazz = env->FindClass(className.c_str());
        jfieldID classObjectField = env->GetStaticFieldID(clazz, "object$", classObjectDescriptor.c_str());
        return env->GetStaticObjectField(clazz, classObjectField);
    } else if (kind == TYPE_POINTER) {
        return env->NewObject(cache->pointerClass, cache->pointerConstructor, result);
    } else if (kind == TYPE_OBJECT) {
        // TODO: don't call getClassName if result==nil
        Class clazz = object_getClass(result);

        jclass jvmClass = NULL;
        while (clazz) {
            // TODO: free?
            const char *className = class_getName(clazz);
            std::string fqClassName = OBJC_PACKAGE_PREFIX + className;
            if ((jvmClass = env->FindClass(fqClassName.c_str()))) break;
            env->ExceptionClear();

            clazz = class_getSuperclass(clazz);
        }

        if (!jvmClass) {
            fprintf(stderr, "Class not found for object of class: %s\n", object_getClassName(result));
            // TODO: return new NotFoundObjCClass(className, result) or something
            exit(42);
        }

        return createMirrorObjectOfClass(env, result, jvmClass);
    } else {
        // TODO: throw a JVM exception
        fprintf(stderr, "Unsupported type kind: %d\n", kind);
        exit(42);
    }
}

JNIEXPORT jobject JNICALL Java_jet_objc_Native_objc_1msgSend(
        JNIEnv *env,
        jclass,
        jobject receiverJObject,
        jstring selectorName,
        jobjectArray argArray
) {
    id receiver = (id) env->GetLongField(receiverJObject, cache->objcObjectPointerField);

    AutoJString selectorNameStr(env, selectorName);
    SEL selector = sel_registerName(selectorNameStr.str());

    std::vector<void *> args = extractArgumentsFromJArray(env, argArray);

    MsgSendInvocation invocation(receiver, selector, args);
    id result = invocation.invoke();
    TypeKind returnType = invocation.returnTypeKind();

    return coerceNativeToJVM(env, result, returnType);
}

// --------------------------------------------------------
// Closures
// --------------------------------------------------------

struct ClosureData {
    ffi_cif cif;
    ffi_closure *closure;
    void *fun;
    JavaVM *vm;
    jobject function;
    jmethodID invokeMethodID;
};

void coerceJVMObjectToNative(JNIEnv *env, jobject object, void *ret) {
    // TODO: get the correct type from the function signature instead
    jobject classObject = env->CallObjectMethod(object, cache->objectGetClassMethod);
    AutoJString nameStr(env, (jstring) env->CallObjectMethod(classObject, cache->classGetNameMethod));
    const char *name = nameStr.str();
    if (!strcmp(name, "jet.Unit")) {
        *(void **) ret = NULL;
    } else if (!strcmp(name, "java.lang.Integer")) {
        *(int *) ret = env->GetIntField(object, cache->integerValueField);
    } else if (!strcmp(name, "java.lang.Long")) {
        *(long *) ret = env->GetLongField(object, cache->longValueField);
    } else if (!strcmp(name, "java.lang.Short")) {
        *(short *) ret = env->GetShortField(object, cache->shortValueField);
    } else if (!strcmp(name, "java.lang.Float")) {
        *(float *) ret = env->GetFloatField(object, cache->floatValueField);
    } else if (!strcmp(name, "java.lang.Double")) {
        // TODO: this doesn't work as a closure return type, fix it and write a test
        *(double *) ret = env->GetDoubleField(object, cache->doubleValueField);
    } else if (!strcmp(name, "java.lang.Character")) {
        *(char *) ret = env->GetCharField(object, cache->characterValueField);
    } else if (!strcmp(name, "java.lang.Boolean")) {
        *(BOOL *) ret = env->GetBooleanField(object, cache->booleanValueField);
    } else {
        *(void **) ret = coerceJVMNativeValueToNative(env, object);
    }
}

void closureHandler(ffi_cif *cif, void *ret, void *args[], void *userData) {
    ClosureData *data = (ClosureData *) userData;
    JavaVM *vm = data->vm;
    JNIEnv *env;

    int attached = vm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_OK;
    if (!attached) {
        // TODO: test native threads
        if (vm->AttachCurrentThread((void **) &env, 0) != JNI_OK) {
            fprintf(stderr, "Error attaching native thread to VM\n");
            return;
        }
    }

    env->PushLocalFrame(16);

    jobject result = env->CallObjectMethod(data->function, data->invokeMethodID);
    result = env->PopLocalFrame(result);

    coerceJVMObjectToNative(env, result, ret);

    if (!attached) {
        vm->DetachCurrentThread();
    }

    // TODO: deallocate closure, ClosureData, 'function' global reference, etc.
}

ffi_type *ffiTypeFromJavaDescriptor(const char *descriptor) {
    switch (*descriptor) {
        case 'C': case 'Z': return &ffi_type_schar;
        case 'I': return &ffi_type_sint;
        case 'S': return &ffi_type_sshort;
        case 'J': return &ffi_type_slong;
        case 'F': return &ffi_type_float;
        case 'D': return &ffi_type_double;
        case 'V': return &ffi_type_void;
        default: return &ffi_type_pointer;
    }
}

void *createNativeClosureForFunction(JNIEnv *env, jobject function, jstring signature) {
    ClosureData *data = new ClosureData;

    if (jint vm = env->GetJavaVM(&data->vm)) {
        fprintf(stderr, "Error getting Java VM: %d\n", vm);
        return 0;
    }

    data->function = env->NewGlobalRef(function);
    env->DeleteLocalRef(function);

    // TODO: get ffi types of all arguments
    AutoJString descStr(env, signature);
    const char *desc = descStr.str();
    while (desc[1]) desc++; // go until the last character
    ffi_type *returnType = ffiTypeFromJavaDescriptor(desc);

    ffi_type *args[1];
    if (ffi_prep_cif(&data->cif, FFI_DEFAULT_ABI, 0, returnType, args) != FFI_OK) {
        fprintf(stderr, "Error preparing CIF\n");
        return 0;
    }

    data->closure = (ffi_closure *) ffi_closure_alloc(sizeof(ffi_closure), &data->fun);
    if (!data->closure) {
        fprintf(stderr, "Error allocating closure\n");
        return 0;
    }
    
    if (ffi_prep_closure_loc(data->closure, &data->cif, &closureHandler, data, data->fun) != FFI_OK) {
        fprintf(stderr, "Error preparing closure\n");
        return 0;
    }

    jclass function0 = env->FindClass("jet/Function0");
    data->invokeMethodID = env->GetMethodID(function0, "invoke", "()Ljava/lang/Object;");

    return data->fun;
}
