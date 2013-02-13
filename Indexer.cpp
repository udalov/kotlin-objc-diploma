#include <fstream>
#include <vector>

#include "clang-c/Index.h"

#include "Indexer.h"
#include "ObjCIndex.pb.h"


struct IndexerClientData {
    TranslationUnit result;

    IndexerClientData():
        result()
    {}
};


void indexDeclaration(CXClientData clientData, const CXIdxDeclInfo *info) {
    if (!info->isDefinition) {
        return;
    }

    IndexerClientData *data = static_cast<IndexerClientData *>(clientData);
    TranslationUnit& result = data->result;
    const CXIdxEntityInfo *entityInfo = info->entityInfo;

    if (entityInfo->kind == CXIdxEntity_ObjCClass) {
        ObjCClass *clazz = result.add_class_();
        clazz->set_name(entityInfo->name);
    }
    else if (entityInfo->kind == CXIdxEntity_ObjCProtocol) {
        ObjCProtocol *protocol = result.add_protocol();
        protocol->set_name(entityInfo->name);
    }
}


void Indexer::run() const {
    GOOGLE_PROTOBUF_VERIFY_VERSION;

    CXIndex index = clang_createIndex(false, false);
    CXIndexAction action = clang_IndexAction_create(index);

    IndexerCallbacks callbacks = {};
    callbacks.indexDeclaration = indexDeclaration;

    IndexerClientData clientData;

    std::vector<const char *> args;
    std::transform(headers.begin(), headers.end(), std::back_inserter(args), std::mem_fun_ref(&std::string::c_str));
    args.push_back("-ObjC");

    clang_indexSourceFile(action, &clientData, &callbacks, sizeof(callbacks), 0, 0,
            &args[0], static_cast<int>(args.size()), 0, 0, 0, 0);

    clang_IndexAction_dispose(action);
    clang_disposeIndex(index);

    std::ofstream output(outputFile.c_str());
    clientData.result.SerializeToOstream(&output);
}
