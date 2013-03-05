#include <vector>
#include <string>

#include "Indexer.h"
#include "ObjCIndex.h"

void buildObjCIndex(const char *const *headers, int numHeaders, const char *outputFile) {
    std::vector<std::string> headersVector;
    headersVector.reserve(static_cast<size_t>(numHeaders));
    for (int i = 0; i < numHeaders; i++) {
        headersVector.push_back(headers[i]);
    }
    Indexer indexer(headersVector, outputFile);
    indexer.run();
}

JNIEXPORT void JNICALL Java_org_jetbrains_jet_lang_resolve_objc_ObjCDescriptorResolver_buildObjCIndex
        (JNIEnv *env, jobject resolver, jstring headerString, jstring outputFileNameString) {
    const char *header = env->GetStringUTFChars(headerString, NULL);
    const char *const headers[] = { header };

    const char *outputFile = env->GetStringUTFChars(outputFileNameString, NULL);

    buildObjCIndex(headers, 1, outputFile);

    env->ReleaseStringUTFChars(headerString, header);
    env->ReleaseStringUTFChars(outputFileNameString, outputFile);
}
