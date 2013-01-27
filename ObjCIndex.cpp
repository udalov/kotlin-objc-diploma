#include <cstdio>
#include "ObjCIndex.h"

void ObjCIndex_buildIndex(char** headers, int numHeaders, char* outputFile) {
    FILE* file = fopen(outputFile, "w");
    fprintf(file, "Hello, world!\n");
    fprintf(file, "%d headers passed\n", numHeaders);
    for (int i = 0; i < numHeaders; i++) {
        fprintf(file, "  %s\n", headers[i]);
    }
    fclose(file);
}
