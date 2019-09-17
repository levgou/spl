#ifndef FILESYSTEM_H_
#define FILESYSTEM_H_

#include "Files.h"


class FileSystem {
private:
    Directory *rootDirectory;
    Directory *workingDirectory;
public:
    FileSystem();

    Directory &getRootDirectory() const; // Return reference to the root directory
    Directory &getWorkingDirectory() const; // Return reference to the working directory
    void setWorkingDirectory(Directory *newWorkingDirectory); // Change the working directory of the file system

    // >>>>>>>>>>>>>>>>>>>>>>>>>>> ctors & dtors & operator= <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    virtual ~FileSystem();

    FileSystem(const FileSystem &other);

    FileSystem(FileSystem &&other);

    FileSystem& operator=(const FileSystem& other);

    FileSystem& operator=(FileSystem&& other);

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    // >>>>>>>>>>>>>>>>>>>>>>>>>>> services for constructors & destructors <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    void clean();
    void copy(const FileSystem& other);
    void steal(FileSystem& other);

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

};


#endif
