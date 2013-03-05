#pragma once

#include <jni.h>

#include <vector>
#include <string>

class Indexer {
    private:
        const std::vector<std::string> headers;
        const std::string outputFile;

    public:
        Indexer(const std::vector<std::string>& headers, const std::string& outputFile):
            headers(headers),
            outputFile(outputFile)
        {}

        void run() const;
};

#ifdef __cplusplus
extern "C" {
#endif

void buildObjCIndex(const char *const *headers, int numHeaders, const char *outputFile);

/*
 * Class:     org_jetbrains_jet_lang_resolve_objc_ObjCDescriptorResolver
 * Method:    buildObjCIndex
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_jetbrains_jet_lang_resolve_objc_ObjCDescriptorResolver_buildObjCIndex
  (JNIEnv *, jobject, jstring, jstring);

#ifdef __cplusplus
};
#endif
