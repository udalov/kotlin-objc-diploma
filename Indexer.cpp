#include <fstream>
#include <vector>

#include "clang-c/Index.h"

#include "Indexer.h"


struct IndexerClientData {
    std::ofstream& output;

    IndexerClientData(std::ofstream& output):
        output(output)
    {}
};


void indexDeclaration(CXClientData data, const CXIdxDeclInfo *info) {
    std::ofstream& output = static_cast<IndexerClientData *>(data)->output;
    output << info->entityInfo->USR << std::endl;
}


void Indexer::run() const {
    std::ofstream output(outputFile.c_str());

    CXIndex index = clang_createIndex(false, false);
    CXIndexAction action = clang_IndexAction_create(index);

    IndexerCallbacks callbacks = {};
    callbacks.indexDeclaration = indexDeclaration;

    IndexerClientData clientData(output);

    std::vector<const char *> args(headers.size());
    std::transform(headers.begin(), headers.end(), args.begin(), std::mem_fun_ref(&std::string::c_str));

    clang_indexSourceFile(action, &clientData, &callbacks, sizeof(callbacks), 0, 0,
            &args[0], static_cast<int>(args.size()), 0, 0, 0, 0);

    clang_IndexAction_dispose(action);
    clang_disposeIndex(index);
}
