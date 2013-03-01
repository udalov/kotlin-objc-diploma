#include <fstream>
#include <string>

#include "asserts.h"
#include "OutputCollector.h"

void OutputCollector::writeToFile(const std::string& outputFile) {
    std::ofstream output(outputFile.c_str());
    m_result.SerializeToOstream(&output);
}

namespace {
    template<typename T> void saveByUSR(std::map<std::string, T *>& map, const std::string& usr, T *t) {
        assertTrue(map.find(usr) == map.end());
        map[usr] = t;
    }

    template<typename T> T *loadByUSR(const std::map<std::string, T *>& map, const std::string& usr) {
        auto it = map.find(usr);
        if (it == map.end()) return nullptr;
        auto found = it->second;
        assertNotNull(found);
        return found;
    }
}

void OutputCollector::saveClassByUSR(const std::string& usr, ObjCClass *clazz) {
    saveByUSR(m_classes, usr, clazz);
}

void OutputCollector::saveProtocolByUSR(const std::string& usr, ObjCProtocol *protocol) {
    saveByUSR(m_protocols, usr, protocol);
}

void OutputCollector::saveCategoryByUSR(const std::string& usr, ObjCCategory *category) {
    saveByUSR(m_categories, usr, category);
}

ObjCClass *OutputCollector::loadClassByUSR(const std::string& usr) const {
    return loadByUSR(m_classes, usr);
}

ObjCProtocol *OutputCollector::loadProtocolByUSR(const std::string& usr) const {
    return loadByUSR(m_protocols, usr);
}

ObjCCategory *OutputCollector::loadCategoryByUSR(const std::string& usr) const {
    return loadByUSR(m_categories, usr);
}
