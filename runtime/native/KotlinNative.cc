#include "KotlinNative.h"

#include <dlfcn.h>

#include <cstdio>
#include <memory>
#include <objc/message.h>
#include <objc/objc.h>
#include <objc/runtime.h>

typedef jlong pointer_t;

const char *const CLASS_NATIVE_POINTER = "jet/runtime/objc/NativePointer";

// TODO: fail gracefully if any class/method/field is not found

jclass getNativePointerClass(JNIEnv *env) {
    return env->FindClass(CLASS_NATIVE_POINTER);
}

jobject createNativePointer(JNIEnv *env, pointer_t pointer) {
    jclass nativePointer = getNativePointerClass(env);
    jmethodID constructor = env->GetMethodID(nativePointer, "<init>", "(J)V");
    return env->NewObject(nativePointer, constructor, pointer);
}


JNIEXPORT void JNICALL Java_jet_runtime_objc_Native_dlopen(
        JNIEnv *env,
        jclass clazz,
        jstring path
) {
    const char *chars = env->GetStringUTFChars(path, 0);
    if (!dlopen(chars, RTLD_GLOBAL)) {
        // TODO: report an error
    }
    env->ReleaseStringUTFChars(path, chars);
}


JNIEXPORT jobject JNICALL Java_jet_runtime_objc_Native_objc_1getClass(
        JNIEnv *env,
        jclass clazz,
        jstring name
) {
    const char *chars = env->GetStringUTFChars(name, 0);
    id objcClass = objc_getClass(chars);
    env->ReleaseStringUTFChars(name, chars);

    pointer_t pointer = (pointer_t) objcClass;
    return createNativePointer(env, pointer);
}


JNIEXPORT jobject JNICALL Java_jet_runtime_objc_Native_objc_1msgSend(
        JNIEnv *env,
        jclass clazz,
        jobject receiver,
        jobject selector,
        jobjectArray argArray
) {
    jclass nativePointer = getNativePointerClass(env);
    jmethodID getPointer = env->GetMethodID(nativePointer, "getValue", "()J");

    pointer_t receiverPointer = env->CallLongMethod(receiver, getPointer);
    pointer_t selectorPointer = env->CallLongMethod(selector, getPointer);

    Class objcClass = (Class) receiverPointer;
    SEL message = (SEL) selectorPointer;

    id objcClassReceiver = (id) objcClass;
    objc_msgSend(objcClassReceiver, message);

    return receiver;
}


JNIEXPORT jobject JNICALL Java_jet_runtime_objc_Native_sel_1registerName(
        JNIEnv *env,
        jclass clazz,
        jstring name
) {
    const char *chars = env->GetStringUTFChars(name, 0);
    SEL selector = sel_registerName(chars);
    env->ReleaseStringUTFChars(name, chars);

    pointer_t pointer = (pointer_t) selector;
    return createNativePointer(env, pointer);
}
