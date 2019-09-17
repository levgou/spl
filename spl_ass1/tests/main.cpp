#include "Environment.h"

// ... You may not change this file

unsigned int verbose = 0;

int main(int , char **) {
    Environment env;

    Environment env2 = env;
    const FileSystem &env1fs = env.getFileSystem();
    FileSystem fs = env1fs;
    Directory &d1 = fs.getWorkingDirectory();
    return 0;
}
