#include "KotlinNative.h"

#include <dlfcn.h>

#include <cstdio>
#include <memory>
#include <objc/message.h>
#include <objc/objc.h>
#include <objc/runtime.h>

typedef jlong pointer_t;

const char *const CLASS_ID = "jet/runtime/objc/ID";

// TODO: fail gracefully if any class/method/field is not found

jclass getIdClass(JNIEnv *env) {
    return env->FindClass(CLASS_ID);
}

jobject createNativePointer(JNIEnv *env, pointer_t pointer) {
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


id sendMessage(
        JNIEnv *env,
        jclass clazz,
        jobject receiver,
        jstring selectorName,
        jobjectArray argArray
) {
    jclass idClass = getIdClass(env);
    jmethodID getValue = env->GetMethodID(idClass, "getValue", "()J");
    pointer_t receiverPointer = env->CallLongMethod(receiver, getValue);
    SEL message = lookupSelector(env, selectorName);

    id objcReceiver = (id) receiverPointer;

    id result = objc_msgSend(objcReceiver, message);

    return result;
}

JNIEXPORT jlong JNICALL Java_jet_runtime_objc_Native_objc_1msgSendPrimitive(
        JNIEnv *env,
        jclass clazz,
        jobject receiver,
        jstring selectorName,
        jobjectArray argArray
) {
    id result = sendMessage(env, clazz, receiver, selectorName, argArray);
    return (jlong) result;
}

JNIEXPORT jobject JNICALL Java_jet_runtime_objc_Native_objc_1msgSendObjCObject(
        JNIEnv *env,
        jclass clazz,
        jobject receiver,
        jstring selectorName,
        jobjectArray argArray
) {
    id result = sendMessage(env, clazz, receiver, selectorName, argArray);
    // TODO: object_getClassName, new ...(result)
    return receiver;
}

