#pragma once

#ifdef __cplusplus
extern "C" {
#endif

void buildObjCIndex(const char *const *headers, int numHeaders, const char *outputFile);

#ifdef __cplusplus
};
#endif
