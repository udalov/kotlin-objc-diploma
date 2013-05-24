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
    jfieldID callbackFunctionArityField;
    jfieldID objcObjectPointerField;
    jfieldID pointerPeerField;
    jfieldID primitiveValueValueField;

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
        callbackFunctionArityField = env->GetFieldID(callbackFunctionClass, "arity", "I");
        objcObjectPointerField = env->GetFieldID(objcObjectClass, "pointer", "J");
        pointerPeerField = env->GetFieldID(pointerClass, "peer", "J");
        primitiveValueValueField = env->GetFieldID(primitiveValueClass, "value", "J");

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
    const char *chars = env->GetStringUTFChars(path, 0);
    if (!dlopen(chars, RTLD_GLOBAL)) {
        // TODO: report an error properly
        fprintf(stderr, "Library not found: %s\n", chars);
        exit(42);
    }
    env->ReleaseStringUTFChars(path, chars);
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
    const char *chars = env->GetStringUTFChars(name, 0);
    id objcClass = objc_getClass(chars);
    env->ReleaseStringUTFChars(name, chars);
    return A2L(objcClass);
}


void *createNativeClosureForFunction(JNIEnv *env, jobject function, jint arity);

std::vector<void *> extractArgumentsFromJArray(JNIEnv *env, jobjectArray argArray) {
    jsize length = env->GetArrayLength(argArray);
    std::vector<void *> args;
    if (!length) return args;

    args.reserve(length);
    for (jsize i = 0; i < length; i++) {
        jobject arg = env->GetObjectArrayElement(argArray, i);
        if (env->IsInstanceOf(arg, cache->callbackFunctionClass)) {
            jobject function = env->GetObjectField(arg, cache->callbackFunctionFunctionField);
            jint arity = env->GetIntField(arg, cache->callbackFunctionArityField);
            void *closure = createNativeClosureForFunction(env, function, arity);
            args.push_back(closure);
        } else if (env->IsInstanceOf(arg, cache->pointerClass)) {
            jlong peer = env->GetLongField(arg, cache->pointerPeerField);
            args.push_back(L2A(peer));
        } else if (env->IsInstanceOf(arg, cache->objcObjectClass)) {
            jlong pointer = env->GetLongField(arg, cache->objcObjectPointerField);
            args.push_back(L2A(pointer));
        } else if (env->IsInstanceOf(arg, cache->primitiveValueClass)) {
            jlong value = env->GetLongField(arg, cache->primitiveValueValueField);
            args.push_back(L2A(value));
        }
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

SEL selectorFromJString(JNIEnv *env, jstring name) {
    const char *chars = env->GetStringUTFChars(name, 0);
    SEL selector = sel_registerName(chars);
    env->ReleaseStringUTFChars(name, chars);
    return selector;
}

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

    SEL selector = selectorFromJString(env, selectorName);

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
    int arity;
};

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

    jclass function0 = env->FindClass("jet/Function0");
    jmethodID invoke = env->GetMethodID(function0, "invoke", "()Ljava/lang/Object;");

    jobject result = env->CallObjectMethod(data->function, invoke);
    result = env->PopLocalFrame(result);

    // TODO: cast result to id properly and save to *ret
    *(int *)ret = 0;

    if (!attached) {
        vm->DetachCurrentThread();
    }

    // TODO: deallocate closure, ClosureData, 'function' global reference, etc.
}

void *createNativeClosureForFunction(JNIEnv *env, jobject function, jint arity) {
    // TODO: arity > 0
    assert(arity == 0 || "Callbacks with parameters aren't supported yet");

    ClosureData *data = new ClosureData;

    if (jint vm = env->GetJavaVM(&data->vm)) {
        fprintf(stderr, "Error getting Java VM: %d\n", vm);
        return 0;
    }

    data->function = env->NewGlobalRef(function);
    env->DeleteLocalRef(function);

    data->arity = arity;

    ffi_type *args[1];
    if (ffi_prep_cif(&data->cif, FFI_DEFAULT_ABI, 0, &ffi_type_void, args) != FFI_OK) {
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

    return data->fun;
}
