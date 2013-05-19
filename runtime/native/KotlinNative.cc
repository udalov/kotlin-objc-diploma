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

const char *const CLASS_ID = "jet/runtime/objc/ID";
const char *const OBJC_OBJECT_CONSTRUCTOR = "(Ljet/runtime/objc/ID;)V";

const std::string OBJC_PACKAGE_PREFIX = "objc/";

// TODO: hide everything util under a namespace
// TODO: process all possible JNI errors
// TODO: delete local JNI references where there can be too many of them
// TODO: fail gracefully if any class/method/field is not found

jclass getIdClass(JNIEnv *env) {
    return env->FindClass(CLASS_ID);
}

jobject createNativePointer(JNIEnv *env, void *pointer) {
    jclass idClass = getIdClass(env);
    jmethodID constructor = env->GetMethodID(idClass, "<init>", "(J)V");
    return env->NewObject(idClass, constructor, pointer);
}

SEL lookupSelector(JNIEnv *env, jstring name) {
    const char *chars = env->GetStringUTFChars(name, 0);
    SEL selector = sel_registerName(chars);
    env->ReleaseStringUTFChars(name, chars);
    return selector;
}


// --------------------------------------------------------
// Dynamic libraries
// --------------------------------------------------------

JNIEXPORT void JNICALL Java_jet_runtime_objc_Native_dlopen(
        JNIEnv *env,
        jclass clazz,
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

JNIEXPORT jlong JNICALL Java_jet_runtime_objc_Native_malloc(
        JNIEnv *env,
        jclass,
        jlong bytes
) {
    void *memory = malloc(bytes);
    return *(jlong *)&memory;
}

JNIEXPORT void JNICALL Java_jet_runtime_objc_Native_free(
        JNIEnv *env,
        jclass,
        jlong pointer
) {
    free(*(void **)&pointer);
}

JNIEXPORT jlong JNICALL Java_jet_runtime_objc_Native_getWord(
        JNIEnv *env,
        jclass,
        jlong pointer
) {
    return *(jlong *)pointer;
}

JNIEXPORT void JNICALL Java_jet_runtime_objc_Native_setWord(
        JNIEnv *env,
        jclass,
        jlong pointer,
        jlong value
) {
    *(jlong *)pointer = value;
}


// --------------------------------------------------------
// Objective-C
// --------------------------------------------------------

JNIEXPORT jobject JNICALL Java_jet_runtime_objc_Native_objc_1getClass(
        JNIEnv *env,
        jclass clazz,
        jstring name
) {
    const char *chars = env->GetStringUTFChars(name, 0);
    id objcClass = objc_getClass(chars);
    env->ReleaseStringUTFChars(name, chars);
    return createNativePointer(env, objcClass);
}


id constructNSInvocation(id receiver, SEL selector, const std::vector<id>& args) {
    static SEL methodSignatureForSelector = sel_registerName("methodSignatureForSelector:");
    static id invocationClass = objc_getClass("NSInvocation");
    static SEL invocationWithMethodSignature = sel_registerName("invocationWithMethodSignature:");
    static SEL setTarget = sel_registerName("setTarget:");
    static SEL setSelector = sel_registerName("setSelector:");

    id signature = objc_msgSend(receiver, methodSignatureForSelector, selector);
    id invocation = objc_msgSend(invocationClass, invocationWithMethodSignature, signature);
    objc_msgSend(invocation, setTarget, receiver);
    objc_msgSend(invocation, setSelector, selector);
    
    for (size_t i = 0, n = args.size(); i < n; i++) {
        static SEL setArgument = sel_registerName("setArgument:atIndex:");
        // From NSInvocation Class Reference:
        // "Indices 0 and 1 indicate the hidden arguments self and _cmd, respectively"
        objc_msgSend(invocation, setArgument, &args[i], i + 2);
    }

    return invocation;
}

bool selectorReturnsVoid(id invocation) {
    static SEL methodSignature = sel_registerName("methodSignature");
    static SEL methodReturnType = sel_registerName("methodReturnType");

    id signature = objc_msgSend(invocation, methodSignature);
    // TODO: free?
    const char *returnType = (const char *) objc_msgSend(signature, methodReturnType);
    return !strcmp(returnType, "v");
}

std::vector<id> extractArgumentsFromJArray(JNIEnv *env, jobjectArray argArray) {
    jsize length = env->GetArrayLength(argArray);
    std::vector<id> args;
    if (!length) return args;

    // TODO: cache ID and getValue somehow
    jclass idClass = getIdClass(env);
    jmethodID getValue = env->GetMethodID(idClass, "getValue", "()J");

    args.reserve(length);
    for (jsize i = 0; i < length; i++) {
        jobject argObject = env->GetObjectArrayElement(argArray, i);
        id arg = (id) env->CallLongMethod(argObject, getValue);
        args.push_back(arg);
    }

    return args;
}

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

id sendMessage(
        JNIEnv *env,
        jobject receiverJObject,
        jstring selectorName,
        jobjectArray argArray
) {
    jclass idClass = getIdClass(env);
    jmethodID getValue = env->GetMethodID(idClass, "getValue", "()J");

    id receiver = (id) env->CallLongMethod(receiverJObject, getValue);
    SEL selector = lookupSelector(env, selectorName);
    std::vector<id> args = extractArgumentsFromJArray(env, argArray);

    // At this point, all we have to do is to call objc_msgSend(receiver,
    // selector, args) and get the result. Unfortunately, there's no portable
    // way of passing arguments to objc_msgSend, so we use NSInvocation to
    // put arguments on stack and get the result. Since it's Objective-C
    // runtime now, we also need to create NSAutoreleasePool to prevent the
    // runtime from spawning error messages about memory leaks

    // TODO: use libffi here instead, it's faster and less cumbersome

    id pool = createAutoreleasePool();

    id invocation = constructNSInvocation(receiver, selector, args);
    static SEL invoke = sel_registerName("invoke");
    objc_msgSend(invocation, invoke);

    id buffer[1];
    if (!selectorReturnsVoid(invocation)) {
        // It's illegal to call '-getReturnValue:' for void methods
        static SEL getReturnValue = sel_registerName("getReturnValue:");
        objc_msgSend(invocation, getReturnValue, buffer);
    }
    id result = buffer[0];

    drainAutoreleasePool(pool);

    return result;
}

JNIEXPORT jlong JNICALL Java_jet_runtime_objc_Native_objc_1msgSendPrimitive(
        JNIEnv *env,
        jclass clazz,
        jobject receiver,
        jstring selectorName,
        jobjectArray argArray
) {
    id result = sendMessage(env, receiver, selectorName, argArray);
    return (jlong) result;
}

JNIEXPORT jobject JNICALL Java_jet_runtime_objc_Native_objc_1msgSendObjCObject(
        JNIEnv *env,
        jclass,
        jobject receiver,
        jstring selectorName,
        jobjectArray argArray
) {
    id result = sendMessage(env, receiver, selectorName, argArray);
    // TODO: don't call getClassName if result==nil
    Class clazz = object_getClass(result);

    jclass jvmClass = NULL;
    while (clazz != Nil) {
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

    // TODO: release in finalize
    static SEL retain = sel_registerName("retain");
    objc_msgSend(result, retain);

    // Here we create an instance of this jclass, invoking a constructor which
    // takes a single ID parameter
    jmethodID constructor = env->GetMethodID(jvmClass, "<init>", OBJC_OBJECT_CONSTRUCTOR);

    jobject idInstance = createNativePointer(env, result);
    return env->NewObject(jvmClass, constructor, idInstance);
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

    int attached = vm->GetEnv((void **) &env, JNI_VERSION_1_6);
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

    // TODO: cast result to id properly and save to *ret
    *(int *)ret = 0;

    if (!attached) {
        vm->DetachCurrentThread();
    }

    // TODO: deallocate closure, ClosureData, 'function' global reference, etc.
}

JNIEXPORT jobject JNICALL Java_jet_runtime_objc_Native_createNativeClosureForFunction(
        JNIEnv *env,
        jclass clazz,
        jobject function,
        jint arity
) {
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

    return createNativePointer(env, data->fun);
}
