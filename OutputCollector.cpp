#include <fstream>
#include <string>

#include "asserts.h"
#include "OutputCollector.h"

void OutputCollector::writeToFile(const std::string& outputFile) {
    std::ofstream output(outputFile.c_str());
    m_result.SerializeToOstream(&output);
}

void OutputCollector::saveClassByUSR(const std::string& usr, ObjCClass *clazz) {
    assertTrue(m_classes.find(usr) == m_classes.end());
    m_classes[usr] = clazz;
}

void OutputCollector::saveProtocolByUSR(const std::string& usr, ObjCProtocol *protocol) {
    assertTrue(m_protocols.find(usr) == m_protocols.end());
    m_protocols[usr] = protocol;
}

void OutputCollector::saveCategoryByUSR(const std::string& usr, ObjCCategory *category) {
    assertTrue(m_categories.find(usr) == m_categories.end());
    m_categories[usr] = category;
}

ObjCClass *OutputCollector::loadClassByUSR(const std::string& usr) const {
    auto it = m_classes.find(usr);
    if (it == m_classes.end()) return nullptr;
    auto clazz = it->second;
    assertNotNull(clazz);
    return clazz;
}

ObjCProtocol *OutputCollector::loadProtocolByUSR(const std::string& usr) const {
    auto it = m_protocols.find(usr);
    if (it == m_protocols.end()) return nullptr;
    auto protocol = it->second;
    assertNotNull(protocol);
    return protocol;
}

ObjCCategory *OutputCollector::loadCategoryByUSR(const std::string& usr) const {
    auto it = m_categories.find(usr);
    if (it == m_categories.end()) return nullptr;
    auto category = it->second;
    assertNotNull(category);
    return category;
}
