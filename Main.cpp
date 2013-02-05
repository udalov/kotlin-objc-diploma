#include <fstream>
#include <iostream>

#include "ObjCIndex.h"

int main(int argc, char *argv[]) {
    const char *headers[] = {"testData/c.h"};
    const char *outputFile = "result.out";

    buildObjCIndex(headers, 1, outputFile);

    std::ifstream input(outputFile);
    std::string line;
    while (std::getline(input, line)) {
        std::cout << line;
        std::cout << std::endl;
    }
    input.close();

    return 0;
}
