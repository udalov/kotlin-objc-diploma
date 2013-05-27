#include "KotlinNative.h"

#include <dlfcn.h>

#include <ffi.h>

#include <objc/message.h>
#include <objc/objc.h>
#include <objc/runtime.h>

#include <cassert>
#include <cstdio>
#include <memory>
#include <sstream>
#include <string>
#include <vector>

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

    jclass objcObjectClass;
    jclass objcSelectorClass;
    jclass pointerClass;

    jclass integerClass;
    jclass longClass;
    jclass shortClass;
    jclass floatClass;
    jclass doubleClass;
    jclass characterClass;
    jclass booleanClass;
    jclass unitClass;

    jfieldID objcObjectPointerField;
    jfieldID pointerPeerField;

    jmethodID objectGetClassMethod;
    jmethodID objectToStringMethod;
    jmethodID classGetNameMethod;
    jmethodID classGetDeclaredMethodsMethod;
    jmethodID methodIsBridgeMethod;
    jmethodID methodGetReturnTypeMethod;
    jfieldID methodNameField;

    jfieldID integerValueField;
    jfieldID longValueField;
    jfieldID shortValueField;
    jfieldID floatValueField;
    jfieldID doubleValueField;
    jfieldID characterValueField;
    jfieldID booleanValueField;
    jmethodID integerValueOfMethod;
    jmethodID longValueOfMethod;
    jmethodID shortValueOfMethod;
    jmethodID floatValueOfMethod;
    jmethodID doubleValueOfMethod;
    jmethodID characterValueOfMethod;
    jmethodID booleanValueOfMethod;

    jmethodID objcSelectorConstructor;
    jmethodID pointerConstructor;

    JVMDeclarationsCache(JNIEnv *env): env(env) {
        objcObjectClass = findClass("jet/objc/ObjCObject");
        objcSelectorClass = findClass("jet/objc/ObjCSelector");
        pointerClass = findClass("jet/objc/Pointer");

        objcObjectPointerField = env->GetFieldID(objcObjectClass, "pointer", "J");
        pointerPeerField = env->GetFieldID(pointerClass, "peer", "J");

        integerClass = findClass("java/lang/Integer");
        longClass = findClass("java/lang/Long");
        shortClass = findClass("java/lang/Short");
        floatClass = findClass("java/lang/Float");
        doubleClass = findClass("java/lang/Double");
        characterClass = findClass("java/lang/Character");
        booleanClass = findClass("java/lang/Boolean");
        unitClass = findClass("jet/Unit");

        jclass objectClass = findClass("java/lang/Object");
        jclass classClass = findClass("java/lang/Class");
        jclass methodClass = findClass("java/lang/reflect/Method");
        objectGetClassMethod = env->GetMethodID(objectClass, "getClass", "()Ljava/lang/Class;");
        objectToStringMethod = env->GetMethodID(objectClass, "toString", "()Ljava/lang/String;");
        classGetNameMethod = env->GetMethodID(classClass, "getName", "()Ljava/lang/String;");
        classGetDeclaredMethodsMethod = env->GetMethodID(classClass, "getDeclaredMethods", "()[Ljava/lang/reflect/Method;");
        methodIsBridgeMethod = env->GetMethodID(methodClass, "isBridge", "()Z");
        methodGetReturnTypeMethod = env->GetMethodID(methodClass, "getReturnType", "()Ljava/lang/Class;");
        methodNameField = env->GetFieldID(methodClass, "name", "Ljava/lang/String;");

        integerValueField = env->GetFieldID(integerClass, "value", "I");
        longValueField = env->GetFieldID(longClass, "value", "J");
        shortValueField = env->GetFieldID(shortClass, "value", "S");
        floatValueField = env->GetFieldID(floatClass, "value", "F");
        doubleValueField = env->GetFieldID(doubleClass, "value", "D");
        characterValueField = env->GetFieldID(characterClass, "value", "C");
        booleanValueField = env->GetFieldID(booleanClass, "value", "Z");

        integerValueOfMethod = env->GetStaticMethodID(integerClass, "valueOf", "(I)Ljava/lang/Integer;");
        longValueOfMethod = env->GetStaticMethodID(longClass, "valueOf", "(J)Ljava/lang/Long;");
        shortValueOfMethod = env->GetStaticMethodID(shortClass, "valueOf", "(S)Ljava/lang/Short;");
        floatValueOfMethod = env->GetStaticMethodID(floatClass, "valueOf", "(F)Ljava/lang/Float;");
        doubleValueOfMethod = env->GetStaticMethodID(doubleClass, "valueOf", "(D)Ljava/lang/Double;");
        characterValueOfMethod = env->GetStaticMethodID(characterClass, "valueOf", "(C)Ljava/lang/Character;");
        booleanValueOfMethod = env->GetStaticMethodID(booleanClass, "valueOf", "(Z)Ljava/lang/Boolean;");

        objcSelectorConstructor = env->GetMethodID(objcSelectorClass, "<init>", "(J)V");
        pointerConstructor = env->GetMethodID(pointerClass, "<init>", "(J)V");
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

std::string getClassGetName(JNIEnv *env, jobject object) {
    jobject classObject = env->CallObjectMethod(object, cache->objectGetClassMethod);
    AutoJString className(env, (jstring) env->CallObjectMethod(classObject, cache->classGetNameMethod));
    return className.str();
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
    return (jlong) objc_getClass(nameStr.str());
}


void *createNativeClosureForFunction(JNIEnv *env, jobject function);

bool isKotlinFunction(JNIEnv *env, jobject object) {
    // Check if object implements any FunctionN interface
    // TODO: check for some special annotation or a common superclass of FunctionN instead
    for (unsigned i = 0; i < 23; i++) {
        std::ostringstream className;
        className << "jet/Function" << i;
        std::string nameStr = className.str();
        jclass clazz = env->FindClass(nameStr.c_str());
        if (env->IsInstanceOf(object, clazz)) {
            return true;
        }
    }
    return false;
}

void coerceJVMToNative(JNIEnv *env, jobject object, void *ret) {
    std::string nameStr = getClassGetName(env, object);
    const char *name = nameStr.c_str();
    if (!strncmp(name, "java.lang.", 10)) {
        const char *simple = name + 10;
        if (!strcmp(simple, "Integer")) {
            *(int *) ret = env->GetIntField(object, cache->integerValueField);
        } else if (!strcmp(simple, "Long")) {
            *(long *) ret = env->GetLongField(object, cache->longValueField);
        } else if (!strcmp(simple, "Short")) {
            *(short *) ret = env->GetShortField(object, cache->shortValueField);
        } else if (!strcmp(simple, "Float")) {
            *(float *) ret = env->GetFloatField(object, cache->floatValueField);
        } else if (!strcmp(simple, "Double")) {
            *(double *) ret = env->GetDoubleField(object, cache->doubleValueField);
        } else if (!strcmp(simple, "Character")) {
            *(char *) ret = env->GetCharField(object, cache->characterValueField);
        } else if (!strcmp(simple, "Boolean")) {
            *(BOOL *) ret = env->GetBooleanField(object, cache->booleanValueField);
        } else {
            fprintf(stderr, "Unsupported JVM primitive wrapper type: %s\n", name);
            *(void **) ret = NULL;
        }
    } else if (!strcmp(name, "jet.Unit")) {
        *(void **) ret = NULL;
    } else if (env->IsInstanceOf(object, cache->pointerClass)) {
        *(long *) ret = env->GetLongField(object, cache->pointerPeerField);
    } else if (env->IsInstanceOf(object, cache->objcObjectClass)) {
        *(long *) ret = env->GetLongField(object, cache->objcObjectPointerField);
    } else if (isKotlinFunction(env, object)) {
        *(void **) ret = createNativeClosureForFunction(env, object);
    } else {
        fprintf(stderr, "Unsupported JVM object type: %s\n", name);
        *(void **) ret = NULL;
    }
}

std::vector<void *> extractArgumentsFromJArray(JNIEnv *env, jobjectArray argArray) {
    jsize length = env->GetArrayLength(argArray);
    std::vector<void *> args;
    args.reserve(length);

    for (jsize i = 0; i < length; i++) {
        jobject argObj = env->GetObjectArrayElement(argArray, i);
        void *arg;
        coerceJVMToNative(env, argObj, &arg);
        args.push_back(arg);
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
        case _C_CHR: case _C_UCHR: return &ffi_type_schar;
        case _C_INT: case _C_UINT: return &ffi_type_sint;
        case _C_SHT: case _C_USHT: return &ffi_type_sshort;
        case _C_LNG: case _C_ULNG: return &ffi_type_slong;
        case _C_LNG_LNG: case _C_ULNG_LNG: return &ffi_type_sint64;
        case _C_FLT: return &ffi_type_float;
        case _C_DBL: return &ffi_type_double;
        case _C_VOID: return &ffi_type_void;
        // TODO: structs, arrays, other types
        default: return &ffi_type_pointer;
    }
}

enum TypeKind {
    TYPE_VOID,
    TYPE_INT,
    TYPE_LONG,
    TYPE_SHORT,
    TYPE_FLOAT,
    TYPE_DOUBLE,
    TYPE_CHAR,
    TYPE_BOOLEAN,
    TYPE_POINTER,
    TYPE_SELECTOR,
    TYPE_CLASS,
    TYPE_OBJECT,
};

TypeKind typeKindFromEncoding(char *encoding) {
    while (IGNORED_TYPE_ENCODINGS.find(*encoding) != std::string::npos) {
        encoding++;
    }
    // This method cannot return TYPE_BOOLEAN since BOOL is encoded as 'signed char' in Objective-C.
    // There's a hack on this in the caller, which coerces java.lang.Character to java.lang.Boolean
    switch (*encoding) {
        case _C_CHR: case _C_UCHR: return TYPE_CHAR;
        case _C_INT: case _C_UINT: return TYPE_INT;
        case _C_SHT: case _C_USHT: return TYPE_SHORT;
        case _C_LNG: case _C_ULNG: case _C_LNG_LNG: case _C_ULNG_LNG: return TYPE_LONG;
        case _C_FLT: return TYPE_FLOAT;
        case _C_DBL: return TYPE_DOUBLE;
        case _C_VOID: return TYPE_VOID;

        case _C_SEL: return TYPE_SELECTOR;
        case _C_CLASS: return TYPE_CLASS;
        case _C_PTR: case _C_CHARPTR: return TYPE_POINTER;

        default: return TYPE_OBJECT;
    }
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

    void *invoke() {
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

        void *result;
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

jobject coerceNativeToJVM(JNIEnv *env, void *value, TypeKind kind) {
    if (kind == TYPE_VOID) {
        return NULL;
    } else if (kind == TYPE_INT) {
        return env->CallStaticObjectMethod(cache->integerClass, cache->integerValueOfMethod, *(int *) &value);
    } else if (kind == TYPE_LONG) {
        return env->CallStaticObjectMethod(cache->longClass, cache->longValueOfMethod, *(long *) &value);
    } else if (kind == TYPE_SHORT) {
        return env->CallStaticObjectMethod(cache->shortClass, cache->shortValueOfMethod, *(short *) &value);
    } else if (kind == TYPE_FLOAT) {
        return env->CallStaticObjectMethod(cache->floatClass, cache->floatValueOfMethod, *(float *) &value);
    } else if (kind == TYPE_DOUBLE) {
        return env->CallStaticObjectMethod(cache->doubleClass, cache->doubleValueOfMethod, *(double *) &value);
    } else if (kind == TYPE_CHAR) {
        return env->CallStaticObjectMethod(cache->characterClass, cache->characterValueOfMethod, *(char *) &value);
    } else if (kind == TYPE_BOOLEAN) {
        return env->CallStaticObjectMethod(cache->booleanClass, cache->booleanValueOfMethod, *(BOOL *) &value);
    } else if (kind == TYPE_SELECTOR) {
        return env->NewObject(cache->objcSelectorClass, cache->objcSelectorConstructor, value);
    } else if (kind == TYPE_CLASS) {
        // TODO: what if there's no such class object?
        std::string className = OBJC_PACKAGE_PREFIX + object_getClassName((id) value);
        std::string classObjectDescriptor = "L" + className + "$object;";
        jclass clazz = env->FindClass(className.c_str());
        jfieldID classObjectField = env->GetStaticFieldID(clazz, "object$", classObjectDescriptor.c_str());
        return env->GetStaticObjectField(clazz, classObjectField);
    } else if (kind == TYPE_POINTER) {
        return env->NewObject(cache->pointerClass, cache->pointerConstructor, value);
    } else if (kind == TYPE_OBJECT) {
        id object = (id) value;
        // TODO: don't call getClassName if value==nil
        Class clazz = object_getClass(object);

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
            fprintf(stderr, "Class not found for object of class: %s\n", object_getClassName(object));
            // TODO: return new NotFoundObjCClass(className, value) or something
            exit(42);
        }

        return createMirrorObjectOfClass(env, object, jvmClass);
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
    void *result = invocation.invoke();
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

    coerceJVMToNative(env, result, ret);

    if (!attached) {
        vm->DetachCurrentThread();
    }

    // TODO: deallocate closure, ClosureData, 'function' global reference, etc.
}

ffi_type *ffiTypeFromJavaClass(JNIEnv *env, jobject classObject) {
    AutoJString nameStr(env, (jstring) env->CallObjectMethod(classObject, cache->objectToStringMethod));
    std::string name = nameStr.str();
    if (name == "char" || name == "boolean") return &ffi_type_schar;
    else if (name == "int") return &ffi_type_sint;
    else if (name == "short") return &ffi_type_sshort;
    else if (name == "long") return &ffi_type_slong;
    else if (name == "float") return &ffi_type_float;
    else if (name == "double") return &ffi_type_double;
    else if (name == "void") return &ffi_type_void;
    else return &ffi_type_pointer;
}

jobject reflectMethodFromKotlinFunction(JNIEnv *env, jobject function, bool bridge) {
    jobject classObject = env->CallObjectMethod(function, cache->objectGetClassMethod);
    jobjectArray methods = (jobjectArray) env->CallObjectMethod(classObject, cache->classGetDeclaredMethodsMethod);
    for (jsize i = 0, size = env->GetArrayLength(methods); i < size; i++) {
        jobject method = env->GetObjectArrayElement(methods, i);
        AutoJString name(env, (jstring) env->GetObjectField(method, cache->methodNameField));
        if (!strcmp(name.str(), "invoke")) {
            bool isBridge = env->CallBooleanMethod(method, cache->methodIsBridgeMethod);
            if (isBridge == bridge) return method;
        }
    }

    // TODO: do something meaningful
    std::string className = getClassGetName(env, function);
    fprintf(stderr, "No non-bridge invoke() method in a function literal class: %s\n", className.c_str());
    return NULL;
}

void *createNativeClosureForFunction(JNIEnv *env, jobject function) {
    ClosureData *data = new ClosureData;

    if (jint vm = env->GetJavaVM(&data->vm)) {
        fprintf(stderr, "Error getting Java VM: %d\n", vm);
        return 0;
    }

    data->function = env->NewGlobalRef(function);
    env->DeleteLocalRef(function);

    jobject method = reflectMethodFromKotlinFunction(env, data->function, false);
    jobject returnTypeClassObject = env->CallObjectMethod(method, cache->methodGetReturnTypeMethod);
    ffi_type *returnType = ffiTypeFromJavaClass(env, returnTypeClassObject);

    // TODO: get ffi types of all arguments
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

    jobject bridgeMethod = reflectMethodFromKotlinFunction(env, data->function, true);
    data->invokeMethodID = env->FromReflectedMethod(bridgeMethod);

    return data->fun;
}
