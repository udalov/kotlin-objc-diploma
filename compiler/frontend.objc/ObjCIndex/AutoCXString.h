#pragma once

#include <string>

#include "clang-c/Index.h"

class AutoCXString {
    public:
        /* implicit */ AutoCXString(const CXString& source):
            m_string(clang_getCString(source))
        {
            clang_disposeString(source);
        }

        const std::string& str() const { return m_string; }

    private:
        const std::string m_string;
};
