#ifndef FILES_H_
#define FILES_H_

#include <string>
#include <vector>

using namespace std;

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BASEFILE <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

class BaseFile {
private:
    string name;
    bool isDir;

public:
    BaseFile(string name);

    string getName() const;

    void setToBeDir();

    bool amidir();

    void setName(string newName);

    virtual int getSize() = 0;

    virtual string getString() = 0;

    virtual ~BaseFile();



};

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> FILE <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

class File : public BaseFile {
private:
    int size;

public:

    File(string name, int size); // Constructor
    int getSize(); // Return the size of the file
    string getString();

};

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> DIRECTORY <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

class Directory : public BaseFile {
private:
    Directory *parent;
    vector<BaseFile *> children;

public:


    Directory(string name, Directory *parent); // Constructor

    // >>>>>>>>>>>>>>>>>>>>>>>>>>> ctors & dtors & operator= <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    virtual ~Directory(); // Destructor
    Directory(const Directory &other); // copy constructor
    Directory(Directory &&other); // move constructor
    Directory &operator=(const Directory &other); // copy assignment operator
    Directory &operator=(Directory &&other); // move assignment operator

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


    // >>>>>>>>>>>>>>>>>>>>>>>>>>> services for constructors & destructors <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    void clean();
    void copy (const Directory & other);
    void steal (Directory &other);

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> getters <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    Directory *getParent() const; // Return a pointer to the parent of this directory
    int getSize(); // Return the size of the directory (recursively)
    vector<BaseFile *> getChildren(); // Return children
    string getAbsolutePath();  //Return the path from the root to this
    void stringOfPath(string &path);

    string getString();
    string getChildrenString(); // returns a string representing children list
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> comps & sorts <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    static bool stringComp(BaseFile *bfp1, BaseFile *bfp2); //comparator of strings
    static bool sizeComp(BaseFile *bfp1, BaseFile *bfp2); //comparator of BaseFiles sizes

    void sortByName(); // Sort children by name alphabetically (not recursively)
    void sortBySize(); // Sort children by size (not recursively)
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> setters <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    void setParent(Directory *newParent); // Change the parent of this directory



    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> others <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    void addFile(BaseFile *file); // Add the file to children
    void removeFile(string name); // Remove the file with the specified name from children
    void removeFile(BaseFile *file); // Remove the file from children
    BaseFile *removeAndReturnChildPointerOnly(BaseFile *ptrToRemove); // removes only child ptr & returns it

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> search functions  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    Directory *getChildORFatherDirPointer(string &dirName);
    BaseFile *getChildORFatherBaseFilePointer(string &fileName);
    bool childExists(string &name, bool isDir); // searches child file/dir by name & type
    bool fileExists(BaseFile *file); // check if file/dir is already in dir
    BaseFile *findByVal(BaseFile &file); // return BaseFile pointer if file/dir found else - nullptr
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


};

#endif