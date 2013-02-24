#pragma once

#include <string>
#include <map>

#include "ObjCIndex.pb.h"

class OutputCollector {
    public:
    TranslationUnit& result() { return m_result; }

    void writeToFile(const std::string& outputFile);

    void saveClassByUSR(const std::string& usr, ObjCClass *clazz);
    void saveProtocolByUSR(const std::string& usr, ObjCProtocol *protocol);

    ObjCClass *loadClassByUSR(const std::string& usr) const;
    ObjCProtocol *loadProtocolByUSR(const std::string& usr) const;

    private:
    TranslationUnit m_result;
    std::map<std::string, ObjCClass *> m_classes;
    std::map<std::string, ObjCProtocol *> m_protocols;
};

