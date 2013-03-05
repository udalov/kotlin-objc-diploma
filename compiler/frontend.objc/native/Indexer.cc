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

std::string getCursorTypeSpelling(const CXType& cursorType) {
    // TODO: full type serialization
    auto type = cursorType;

    while (type.kind == CXType_Typedef) {
        auto declaration = clang_getTypeDeclaration(type);
        type = clang_getTypedefDeclUnderlyingType(declaration);
    }
    assertFalse(type.kind == CXType_Invalid);

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

void indexCategory(const CXIdxDeclInfo *info, OutputCollector *data) {
    assertTrue(info->isDefinition);
    auto categoryDeclInfo = clang_index_getObjCCategoryDeclInfo(info);
    assertNotNull(categoryDeclInfo);
    
    auto category = data->result().add_category();
    category->set_class_name(categoryDeclInfo->objcClass->name);

    category->set_category_name(info->entityInfo->name);
    
    auto protocols = categoryDeclInfo->protocols;
    assertNotNull(protocols);
    for (auto protocolName : extractProtocolNames(protocols)) {
        category->add_base_protocol(protocolName);
    }

    data->saveCategoryByUSR(info->entityInfo->USR, category);
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

std::string getNotNullSemanticContainerUSR(const CXIdxDeclInfo *info) {
    assertNotNull(info->semanticContainer);
    AutoCXString container = clang_getCursorUSR(info->semanticContainer->cursor);
    return container.str();
}

ObjCMethod *createMethodInItsContainer(const CXIdxDeclInfo *info, OutputCollector *data) {
    auto container = getNotNullSemanticContainerUSR(info);

    auto clazz = data->loadClassByUSR(container);
    if (clazz) return clazz->add_method();
    auto protocol = data->loadProtocolByUSR(container);
    if (protocol) return protocol->add_method();
    auto category = data->loadCategoryByUSR(container);
    if (category) return category->add_method();

    return nullptr;
}

void indexMethod(const CXIdxDeclInfo *info, OutputCollector *data, bool isClassMethod) {
    ObjCMethod *method = createMethodInItsContainer(info, data);
    assertNotNull(method);

    method->set_class_method(isClassMethod);

    auto function = method->mutable_function();
    function->set_name(info->entityInfo->name);

    auto type = getCursorTypeSpelling(clang_getCursorResultType(info->cursor));
    function->set_return_type(type);

    // TODO: handle variadic arguments
    auto numArguments = clang_Cursor_getNumArguments(info->cursor);
    for (auto i = 0; i < numArguments; ++i) {
        auto argument = clang_Cursor_getArgument(info->cursor, i);
        auto parameter = function->add_parameter();
        AutoCXString name = clang_getCursorSpelling(argument);
        parameter->set_name(name.str());
        auto type = getCursorTypeSpelling(clang_getCursorType(argument));
        parameter->set_type(type);
    }
}

ObjCProperty *createPropertyInItsContainer(const CXIdxDeclInfo *info, OutputCollector *data) {
    // TODO: generify the code somehow (see the same method above)
    auto container = getNotNullSemanticContainerUSR(info);

    auto clazz = data->loadClassByUSR(container);
    if (clazz) return clazz->add_property();
    auto protocol = data->loadProtocolByUSR(container);
    if (protocol) return protocol->add_property();
    auto category = data->loadCategoryByUSR(container);
    if (category) return category->add_property();

    return nullptr;
}

void indexProperty(const CXIdxDeclInfo *info, OutputCollector *data) {
    ObjCProperty *property = createPropertyInItsContainer(info, data);
    assertNotNull(property);

    property->set_name(info->entityInfo->name);

    auto type = getCursorTypeSpelling(clang_getCursorType(info->cursor));
    property->set_type(type);
}

ObjCIvar *createIvarInItsContainer(const CXIdxDeclInfo *info, OutputCollector *data) {
    auto container = getNotNullSemanticContainerUSR(info);
    auto clazz = data->loadClassByUSR(container);
    return clazz ? clazz->add_ivar() : nullptr;
}

void indexIvar(const CXIdxDeclInfo *info, OutputCollector *data) {
    ObjCIvar *ivar = createIvarInItsContainer(info, data);
    assertNotNull(ivar);

    ivar->set_name(info->entityInfo->name);

    auto type = getCursorTypeSpelling(clang_getCursorType(info->cursor));
    ivar->set_type(type);
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
        case CXIdxEntity_ObjCCategory:
            indexCategory(info, data); break;
        case CXIdxEntity_ObjCInstanceMethod:
            indexMethod(info, data, false); break;
        case CXIdxEntity_ObjCClassMethod:
            indexMethod(info, data, true); break;
        case CXIdxEntity_ObjCProperty:
            indexProperty(info, data); break;
        case CXIdxEntity_ObjCIvar:
            indexIvar(info, data); break;
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
