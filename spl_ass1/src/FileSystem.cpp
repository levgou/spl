//
// Created by levgou on 11/16/17.
//

#include <Files.h>
#include <Commands.h>
#include <FileSystem.h>
#include <GlobalVariables.h>
#include <iostream>

using namespace std;

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> GET - SET <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

void FileSystem::setWorkingDirectory(Directory *newWorkingDirectory) {
    workingDirectory=newWorkingDirectory;
}

Directory &FileSystem::getWorkingDirectory() const {
    return *workingDirectory;
}

Directory &FileSystem::getRootDirectory() const {
    return *rootDirectory;
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>> ctors & dtors & operator= <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

FileSystem::FileSystem(): rootDirectory(new Directory ("/", nullptr)), workingDirectory(rootDirectory) {}

FileSystem::~FileSystem() {
    if (verbose==1 || verbose==3)
        cout << "FileSystem::~FileSystem()" << endl;
    clean();

}

FileSystem::FileSystem(const FileSystem &other): rootDirectory(nullptr), workingDirectory(nullptr){
    if (verbose==1 || verbose==3)
        cout << "FileSystem::FileSystem(const FileSystem &other)" << endl;
    copy(other);

}

FileSystem::FileSystem(FileSystem&& other): rootDirectory(nullptr), workingDirectory(nullptr) {
    if (verbose==1 || verbose==3)
        cout << "FileSystem::FileSystem(FileSystem&& other)" << endl;
    steal(other);
}

FileSystem &FileSystem::operator=(const FileSystem &other) {
    if (verbose==1 || verbose==3)
        cout << "FileSystem &FileSystem::operator=(const FileSystem &other)" << endl;
    clean();
    copy(other);

    return *this;
}

FileSystem &FileSystem::operator=(FileSystem &&other) {
    if (verbose==1 || verbose==3)
        cout << "FileSystem &FileSystem::operator=(FileSystem &&other)" << endl;
    clean();
    steal(other);

    return *this;
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>> services for constructors & destructors <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

void FileSystem::clean() {
    delete rootDirectory;
    rootDirectory= nullptr;
    workingDirectory = nullptr;
}

void FileSystem::copy(const FileSystem &other) {
    const Directory& tmp = other.getRootDirectory();
    rootDirectory=new Directory(tmp);

    vector<string> pathVec;
    BaseCommand::parsePath(pathVec, other.getWorkingDirectory().getAbsolutePath());
    workingDirectory = dynamic_cast<Directory *>(BaseCommand::genBFPointerFromVecPath(pathVec, *this));
}

void FileSystem::steal(FileSystem &other) {
    rootDirectory=&other.getRootDirectory();
    workingDirectory=&other.getWorkingDirectory();
    other.rootDirectory= nullptr;
    other.workingDirectory= nullptr;
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
