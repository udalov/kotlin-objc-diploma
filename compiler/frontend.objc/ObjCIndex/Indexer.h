#pragma once

#include <vector>
#include <string>

class Indexer {
    private:
        const std::vector<std::string> headers;
        const std::string outputFile;

    public:
        Indexer(const std::vector<std::string>& headers, const std::string& outputFile):
            headers(headers),
            outputFile(outputFile)
        {}

        void run() const;
};
