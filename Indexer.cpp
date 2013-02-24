#include <fstream>
#include <vector>

#include "clang-c/Index.h"

#include "Indexer.h"
#include "ObjCIndex.pb.h"


#define fail(msg) fprintf(stderr, "Assertion failed: %s (%s:%d)\n", msg, __FILE__, __LINE__), exit(1)
#define assertWithMessage(condition, message) do { if (!(condition)) fail(message); } while (0)
#define assertNotNull(o) assertWithMessage(o, "'" #o "' cannot be null")
#define assertTrue(cond) assertWithMessage(cond, "'" #cond "' should be true")
#define assertFalse(cond) assertWithMessage(cond, "'" #cond "' should be false")
#define assertEquals(o1, o2) assertWithMessage((o1) == (o2), "'" #o1 "' is not equal to '" #o2 "'")


struct IndexerClientData {
    TranslationUnit result;

    IndexerClientData():
        result()
    {}
};


void indexClass(const CXIdxDeclInfo *info, TranslationUnit& result) {
    if (!info->isDefinition) return;
    const CXIdxObjCInterfaceDeclInfo *interfaceDeclInfo = clang_index_getObjCInterfaceDeclInfo(info);
    assertNotNull(interfaceDeclInfo);
    const CXIdxObjCContainerDeclInfo *containerDeclInfo = interfaceDeclInfo->containerInfo;
    assertNotNull(containerDeclInfo);
    if (containerDeclInfo->kind != CXIdxObjCContainer_Interface) {
        // TODO: report a warning if it's @implementation
        return;
    }

    ObjCClass *clazz = result.add_class_();
    clazz->set_name(info->entityInfo->name);

    const CXIdxBaseClassInfo *superInfo = interfaceDeclInfo->superInfo; 
    if (superInfo) {
        const CXIdxEntityInfo *base = superInfo->base;
        assertNotNull(base);
        clazz->set_base_class(base->name);
    }

    const CXIdxObjCProtocolRefListInfo *protocols = interfaceDeclInfo->protocols;
    assertNotNull(protocols);
    unsigned numProtocols = protocols->numProtocols;
    for (unsigned i = 0; i < numProtocols; ++i) {
        const CXIdxObjCProtocolRefInfo *refInfo = protocols->protocols[i];
        assertNotNull(refInfo);
        const CXIdxEntityInfo *protocolInfo = refInfo->protocol;
        assertNotNull(protocolInfo);
        clazz->add_protocol(protocolInfo->name);
    }
}

void indexProtocol(const CXIdxDeclInfo *info, TranslationUnit& result) {
    if (!info->isDefinition) return;

    ObjCProtocol *protocol = result.add_protocol();
    protocol->set_name(info->entityInfo->name);

    const CXIdxObjCProtocolRefListInfo *protocols = clang_index_getObjCProtocolRefListInfo(info);
    assertNotNull(protocols);
    unsigned numProtocols = protocols->numProtocols;
    for (unsigned i = 0; i < numProtocols; ++i) {
        const CXIdxObjCProtocolRefInfo *refInfo = protocols->protocols[i];
        assertNotNull(refInfo);
        const CXIdxEntityInfo *protocolInfo = refInfo->protocol;
        assertNotNull(protocolInfo);
        protocol->add_base_protocol(protocolInfo->name);
    }
}

void indexDeclaration(CXClientData clientData, const CXIdxDeclInfo *info) {
    assertNotNull(clientData);
    assertNotNull(info);
    assertNotNull(info->entityInfo);

    IndexerClientData *data = static_cast<IndexerClientData *>(clientData);
    TranslationUnit& result = data->result;

    switch (info->entityInfo->kind) {
        case CXIdxEntity_ObjCClass:
            indexClass(info, result); break;
        case CXIdxEntity_ObjCProtocol:
            indexProtocol(info, result); break;
        default:
            break;
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
