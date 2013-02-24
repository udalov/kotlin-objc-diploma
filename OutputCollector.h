#pragma once

#include <string>

#include "ObjCIndex.pb.h"

class OutputCollector {
    public:
    TranslationUnit& result() { return m_result; }

    void writeToFile(const std::string& outputFile);

    private:
    TranslationUnit m_result;
};

