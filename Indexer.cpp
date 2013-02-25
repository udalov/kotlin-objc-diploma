#include <fstream>
#include <vector>

#include "clang-c/Index.h"

#include "asserts.h"
#include "AutoCXString.h"
#include "Indexer.h"
#include "OutputCollector.h"
#include "ObjCIndex.pb.h"

std::vector<std::string> extractProtocolNames(const CXIdxObjCProtocolRefListInfo *protocols) {
    std::vector<std::string> result;
    auto numProtocols = protocols->numProtocols;
    for (auto i = 0; i < numProtocols; ++i) {
        auto refInfo = protocols->protocols[i];
        assertNotNull(refInfo);
        auto protocolInfo = refInfo->protocol;
        assertNotNull(protocolInfo);
        result.push_back(protocolInfo->name);
    }
    return result;
}

std::string getCursorTypeSpelling(const CXType& type) {
    // TODO: full type serialization
    AutoCXString spelling = clang_getTypeKindSpelling(type.kind);
    return spelling.str();
}


void indexClass(const CXIdxDeclInfo *info, OutputCollector *data) {
    if (!info->isDefinition) return;
    auto interfaceDeclInfo = clang_index_getObjCInterfaceDeclInfo(info);
    assertNotNull(interfaceDeclInfo);
    auto containerDeclInfo = interfaceDeclInfo->containerInfo;
    assertNotNull(containerDeclInfo);
    if (containerDeclInfo->kind != CXIdxObjCContainer_Interface) {
        // TODO: report a warning if it's @implementation
        return;
    }

    auto clazz = data->result().add_class_();
    clazz->set_name(info->entityInfo->name);

    auto superInfo = interfaceDeclInfo->superInfo; 
    if (superInfo) {
        auto base = superInfo->base;
        assertNotNull(base);
        clazz->set_base_class(base->name);
    }

    auto protocols = interfaceDeclInfo->protocols;
    assertNotNull(protocols);
    for (auto protocolName : extractProtocolNames(protocols)) {
        clazz->add_protocol(protocolName);
    }

    data->saveClassByUSR(info->entityInfo->USR, clazz);
}

void indexProtocol(const CXIdxDeclInfo *info, OutputCollector *data) {
    if (!info->isDefinition) return;

    auto protocol = data->result().add_protocol();
    protocol->set_name(info->entityInfo->name);

    auto protocols = clang_index_getObjCProtocolRefListInfo(info);
    assertNotNull(protocols);
    for (auto protocolName : extractProtocolNames(protocols)) {
        protocol->add_base_protocol(protocolName);
    }

    data->saveProtocolByUSR(info->entityInfo->USR, protocol);
}

void indexMethod(const CXIdxDeclInfo *info, OutputCollector *data, bool isClassMethod) {
    assertNotNull(info->semanticContainer);
    AutoCXString container = clang_getCursorUSR(info->semanticContainer->cursor);
    
    ObjCMethod *method;
    auto clazz = data->loadClassByUSR(container.str());
    if (clazz) {
        method = clazz->add_method();
    } else {
        auto protocol = data->loadProtocolByUSR(container.str());
        if (!protocol) {
            // TODO: categories
            return;
        }
        method = protocol->add_method();
    }
    assertNotNull(method);

    method->set_name(info->entityInfo->name);
    method->set_class_method(isClassMethod);

    auto type = getCursorTypeSpelling(clang_getCursorResultType(info->cursor));
    method->set_return_type(type);

    // TODO: handle variadic arguments
    auto numArguments = clang_Cursor_getNumArguments(info->cursor);
    for (auto i = 0; i < numArguments; ++i) {
        auto argument = clang_Cursor_getArgument(info->cursor, i);
        auto parameter = method->add_parameter();
        AutoCXString name = clang_getCursorSpelling(argument);
        parameter->set_name(name.str());
        auto type = getCursorTypeSpelling(clang_getCursorType(argument));
        parameter->set_type(type);
    }
}

void indexDeclaration(CXClientData clientData, const CXIdxDeclInfo *info) {
    assertNotNull(clientData);
    assertNotNull(info);
    assertNotNull(info->entityInfo);

    OutputCollector *data = static_cast<OutputCollector *>(clientData);

    switch (info->entityInfo->kind) {
        case CXIdxEntity_ObjCClass:
            indexClass(info, data); break;
        case CXIdxEntity_ObjCProtocol:
            indexProtocol(info, data); break;
        case CXIdxEntity_ObjCInstanceMethod:
            indexMethod(info, data, false); break;
        case CXIdxEntity_ObjCClassMethod:
            indexMethod(info, data, true); break;
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

    OutputCollector clientData;

    std::vector<const char *> args;
    std::transform(headers.begin(), headers.end(), std::back_inserter(args), std::mem_fun_ref(&std::string::c_str));
    args.push_back("-ObjC");

    clang_indexSourceFile(action, &clientData, &callbacks, sizeof(callbacks), 0, 0,
            &args[0], static_cast<int>(args.size()), 0, 0, 0, 0);

    clang_IndexAction_dispose(action);
    clang_disposeIndex(index);

    clientData.writeToFile(outputFile);
}
