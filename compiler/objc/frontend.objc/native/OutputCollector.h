#pragma once

#include <map>
#include <set>
#include <string>

#include "ObjCIndex.pb.h"

class OutputCollector {
    public:
        TranslationUnit& result() { return m_result; }

        // Transfers ownership of the returned string to the caller
        std::string *serialize();

        void saveClassByUSR(const std::string& usr, ObjCClass *clazz);
        void saveProtocolByUSR(const std::string& usr, ObjCProtocol *protocol);
        void saveCategoryByUSR(const std::string& usr, ObjCCategory *category);

        void saveForwardDeclaredClass(const std::string& usr, const std::string& name);
        void saveForwardDeclaredProtocol(const std::string& usr, const std::string& name);

        ObjCClass *loadClassByUSR(const std::string& usr) const;
        ObjCProtocol *loadProtocolByUSR(const std::string& usr) const;
        ObjCCategory *loadCategoryByUSR(const std::string& usr) const;

        // TODO: get rid of std::pair here, invent something better
        const std::set<std::pair<std::string, std::string>>& loadForwardDeclaredClasses() const;
        const std::set<std::pair<std::string, std::string>>& loadForwardDeclaredProtocols() const;

    private:
        TranslationUnit m_result;
        std::map<std::string, ObjCClass *> m_classes;
        std::map<std::string, ObjCProtocol *> m_protocols;
        std::map<std::string, ObjCCategory *> m_categories;
        std::set<std::pair<std::string, std::string>> m_forwardClasses;
        std::set<std::pair<std::string, std::string>> m_forwardProtocols;
};
