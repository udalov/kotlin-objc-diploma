#include <fstream>

#include "ObjCIndex.h"
#include "ObjCIndex.pb.h"

const bool PRINT_TO_STDOUT = true;

int main(int argc, char *argv[]) {
    const char *headers[] = {"testData/foundation.h"};
    const char *outputFile = "result.out";

    buildObjCIndex(headers, 1, outputFile);

    if (PRINT_TO_STDOUT) {
        TranslationUnit result;
        std::ifstream input(outputFile);
        result.ParseFromIstream(&input);
        result.PrintDebugString();
    }

    return 0;
}
