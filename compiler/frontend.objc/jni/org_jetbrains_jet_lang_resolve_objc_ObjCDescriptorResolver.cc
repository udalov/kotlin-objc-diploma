#include <cstdio>
#include <jni.h>

#include "org_jetbrains_jet_lang_resolve_objc_ObjCDescriptorResolver.h"

#include "ObjCIndex.h"

JNIEXPORT void JNICALL Java_org_jetbrains_jet_lang_resolve_objc_ObjCDescriptorResolver_buildObjCIndex
        (JNIEnv *env, jobject resolver, jstring headerString, jstring outputFileNameString) {
    const char *header = env->GetStringUTFChars(headerString, NULL);
    const char *const headers[] = { header };

    const char *outputFile = env->GetStringUTFChars(outputFileNameString, NULL);

    buildObjCIndex(headers, 1, outputFile);

    env->ReleaseStringUTFChars(headerString, header);
    env->ReleaseStringUTFChars(outputFileNameString, outputFile);
}
