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
