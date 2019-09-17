//
// Created by levgou on 11/10/17.
//

#include <iostream>
#include <Files.h>
#include <algorithm>
#include <string>
#include <exception>
#include <GlobalVariables.h>


using namespace std;

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> DIRECTORY EXCEPTION <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

class direxception: public exception
{
    virtual const char* what() const throw()
    {
        return "[EX]: File/Dir with same name already exists!";
    }
};

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BASEFILE <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


BaseFile::BaseFile(string name) : name(name), isDir(false) {}

string BaseFile::getName() const {
    return name;
}

void BaseFile::setName(string newName) {
    this->name = newName;
}

BaseFile::~BaseFile() {}


void BaseFile::setToBeDir() {
    isDir = true;
}

bool BaseFile::amidir() {
    return isDir;
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> FILE<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


File::File(string name, int size) : BaseFile(name), size(size) {}

int File::getSize() {
    return size;
}

string File::getString() {
    return "[f]:" + getName();
}


// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> DIRECTORY <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

Directory *Directory::getParent() const {
    return parent;
}

void Directory::setParent(Directory *newParent) {
    this->parent = newParent;
}

void Directory::addFile(BaseFile *file) {

    if (fileExists(file)) {
        direxception dex;
        throw dex;
    }

    this->children.push_back(file);
}

void Directory::removeFile(string name) {

    vector<BaseFile *>::iterator it = children.begin();

    for (; it != children.end(); ++it) {
        if ((*it)->getName() == name) {
            delete *it;
            children.erase(it);
            break;
        }
    }
}

void Directory::removeFile(BaseFile *file) {

    // search for element by value
    vector<BaseFile *>::iterator it_file = find(children.begin(), children.end(), file);

    // not found:
    if (it_file == children.end()) {
        return;
    }

    delete *it_file;
    children.erase(it_file);

}

void Directory::sortByName() {
    sort(children.begin(), children.end(), stringComp);

}

bool Directory::stringComp(BaseFile *bfp1, BaseFile *bfp2) {
    return (bfp1->getName().compare(bfp2->getName()) < 0);

}

void Directory::sortBySize() {
    sort(children.begin(), children.end(), sizeComp);

}

bool Directory::sizeComp(BaseFile *bfp1, BaseFile *bfp2) {
    if (bfp1->getSize() < bfp2->getSize())
        return true;
    else if (bfp1->getSize() == bfp2->getSize())
        return (bfp1->getName() < bfp2->getName());
    else return false;

}

vector<BaseFile *> Directory::getChildren() {
    return children;
}

int Directory::getSize() {
    int counter = 0;
    for (BaseFile *bf : children) {
        counter += (*bf).getSize();
    }
    return counter;
}

string Directory::getAbsolutePath() {
    string path = "";
    this->stringOfPath(path);
    return path;

}

void Directory::stringOfPath(string &path) {
    if (getParent() == nullptr) {
        path += getName();
    } else {
        getParent()->stringOfPath(path);
        if (getParent()->getParent() == nullptr) {
            path += getName();
        } else path += ("/" + getName());
    }

}

string Directory::getString() {
    return "[d]:" + getName();
}

string Directory::getChildrenString() {
    if (children.empty()) {
        return "<empty>";
    }

    string rString = "";
    for (BaseFile *bf : children) {
        rString += bf->getString() + ", ";
    }

    return rString;
}


// >>>>>>>>>>>>>>>>>>>>>>>>>>> ctors & dtors & operator= <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

Directory::Directory(string name, Directory *parent) : BaseFile(name), parent(parent), children(){
    setToBeDir();
}

// Note!        if you use copy:
// Note!        new dir will have nullptr as parent
// Note!        =)

Directory::Directory(const Directory &other): BaseFile(other.getName()), parent(nullptr), children(){
    if (verbose==1 || verbose==3)
        cout << "Directory::Directory(const Directory &other)" << endl;
    copy(other);
}

Directory::Directory(Directory &&other): BaseFile(other.getName()), parent(nullptr), children() {
    if (verbose==1 || verbose==3)
        cout << "Directory::Directory(Directory &&other)" << endl;
    copy(other);
    steal(other);

}

Directory &Directory::operator=(Directory &&other) {
    if (verbose==1 || verbose==3)
        cout << "Directory &Directory::operator=(Directory &&other)" << endl;
    clean();
    steal(other);
    return *this;
}

Directory &Directory::operator=(const Directory &rhs) {
    if (verbose==1 || verbose==3)
        cout << "Directory &Directory::operator=(const Directory &rhs)" << endl;
    if (&rhs != this) {
        clean();
        copy(rhs);
    }
    return *this;

}

Directory::~Directory() {
    if (verbose==1 || verbose==3)
        cout << "Directory::~Directory()" << endl;
    clean();
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>> services for constructors & destructors <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

void Directory::copy(const Directory &other) {
    this->setName(other.getName());
    this->setParent(nullptr);
    for (BaseFile *bf : other.children) {

        if (bf->amidir()) {
            Directory &other_dir = *(dynamic_cast<Directory *>(bf));
            BaseFile *tmpDir = new Directory(other_dir);
            children.push_back(tmpDir);
        } else {
            File &other_file = *(dynamic_cast<File *>(bf));
            BaseFile *tmpFile = new File(other_file);
            children.push_back(tmpFile);
        }
    }
    setToBeDir();
}

void Directory::steal(Directory &other) {
    this->setName(other.getName());
    this->setParent(other.getParent());
    this->children = move(other.children);
    setToBeDir();

}

void Directory::clean() {

    while (!children.empty()) {
        delete children.back();
        children.pop_back();
    }

}




// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> search functions  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
BaseFile *Directory::findByVal(BaseFile &file) {
    for (BaseFile *child : children) {
        if (child->getName() == file.getName())
            return child;
    }

    return nullptr;
}

bool Directory::fileExists(BaseFile *file) {
    return findByVal(*file) != nullptr;

}

bool Directory::childExists(string &name, bool isDir) {
    BaseFile *dummy;
    if (isDir) {
        Directory dummyD(name, nullptr);
        dummy = &dummyD;
        return fileExists(dummy);
    } else {
        File dummyF(name, 0);
        dummy = &dummyF;
        return fileExists(dummy);
    }
}

Directory *Directory::getChildORFatherDirPointer(string &dirName) {
    return dynamic_cast<Directory *>(getChildORFatherBaseFilePointer(dirName));
}

// returns file/dir/parent by name
BaseFile *Directory::getChildORFatherBaseFilePointer(string &fileName) {
    if (fileName == "..") {
        return parent;
    }

    // doesnt matter if file or dir because search is based on name
    File dummyF(fileName, 0);
    return findByVal(dummyF);


}

BaseFile *Directory::removeAndReturnChildPointerOnly(BaseFile *ptrToRemove) {
    if (!fileExists(ptrToRemove)) {
        return nullptr;
    }

    // search for element by value
    vector<BaseFile *>::iterator it_file = find(children.begin(), children.end(), ptrToRemove);

    // not found:
    if (it_file == children.end()) {
        return nullptr;
    }

    children.erase(it_file);
    return  ptrToRemove;

}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<