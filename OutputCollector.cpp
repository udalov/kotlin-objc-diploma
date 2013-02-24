#include <fstream>
#include <string>

#include "OutputCollector.h"

void OutputCollector::writeToFile(const std::string& outputFile) {
    std::ofstream output(outputFile.c_str());
    m_result.SerializeToOstream(&output);
}
