//
// Created by levgou on 11/16/17.
//

#include <iostream>
#include <utility>
#include <Commands.h>
#include <GlobalVariables.h>


using namespace std;

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BASE <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

BaseCommand::BaseCommand(string args): args(args), suffix() {}

BaseCommand::~BaseCommand() {};


void BaseCommand::parseAndUpdateArgs(vector<string> &argsVec, string userArgs, char delim) {

    while (userArgs.find(delim, 0) != string::npos) {
        size_t spcIndx = userArgs.find(delim, 0);
        argsVec.push_back(userArgs.substr(0, spcIndx));
        userArgs = userArgs.substr(spcIndx + 1, userArgs.length() - spcIndx - 1);
    }
    if (userArgs.length() > 0)
        argsVec.push_back(userArgs);
}

void BaseCommand::updateSuffix(string &args) {
    parseAndUpdateArgs(suffix, args, ' ');
}

const vector<string> &BaseCommand::getSuffix() {
    return suffix;

}

string BaseCommand::getArgs() {
    return args;
}

void BaseCommand::parsePath(vector<string> &vec, string path) {


    if (path == ""){
        return;
    }

    if (path.at(0) == '/') {
        vec.emplace_back("/");
        parseAndUpdateArgs(vec, path.substr(1), '/');
    } else {
        parseAndUpdateArgs(vec, path, '/');
    }

}

// will return root or working dir
Directory *BaseCommand::getWorkDirPointerFromVector(vector<string> &pathVec, FileSystem &fs) {
    if (pathVec[0] == "/") {
        return &fs.getRootDirectory();
    } else {
        return &fs.getWorkingDirectory();
    }

}

// get vector of sub paths -> if path is legal - return pointer to the last item in vector
// iterate the vector in order to achieve
BaseFile *BaseCommand::genBFPointerFromVecPath(vector<string> &pathVec, FileSystem &fs) {
    if (pathVec.size() == 0) {
        return nullptr;

    } else if (pathVec.size() == 1) {
        return getFirstBaseFilePointerFromVector(pathVec, fs);
    }

    // if more than 1 string in vector - consists of at least 1 dir
    else {
        Directory *pathPointer = getWorkDirPointerFromVector(pathVec, fs);
        vector<string>::iterator pathIter = pathVec.begin();

        // in case "/" is in vector, pass ther iter to next pos
        if (pathVec[0] == "/") {
            ++pathIter;
        }

        for (; pathIter != pathVec.end() -1; ++pathIter) {
            BaseFile *child = pathPointer->getChildORFatherBaseFilePointer(*pathIter);

            // didn't found:
            if (!child) {
                return nullptr;
            }
            // found but its a file and not a dir (shouldnt be along the path)
            if (!child->amidir()) {
//                if (pathIter+1 == pathVec.end())
//                    return pathPointer->getChildORFatherBaseFilePointer(pathVec.back());
//                else
                return nullptr;
            } else {
                pathPointer = dynamic_cast<Directory *>(child);
            }
        }

        // currently pathPointer should point at 1 before last dir
        return pathPointer->getChildORFatherBaseFilePointer(pathVec.back());
    }


}

BaseFile *BaseCommand::getFirstBaseFilePointerFromVector(vector<string> &pathVec, FileSystem &fs) {
    if (pathVec.size() == 0) {
        return nullptr;
    }

    if (pathVec[0] == "/") {
        return &fs.getRootDirectory();
    } else {
        return fs.getWorkingDirectory().getChildORFatherBaseFilePointer(pathVec[0]);
    }

}

pair<BaseFile *, Directory *> BaseCommand::getSrcBaseFileAndDestDir(FileSystem &fs) {

    vector<string> dirPathSrc;
    parsePath(dirPathSrc, getSuffix()[0]);

    vector<string> dirPathDest;
    parsePath(dirPathDest, getSuffix()[1]);

    BaseFile *srcBF = genBFPointerFromVecPath(dirPathSrc, fs);
    // destination should be dir
    Directory *destDir = dynamic_cast<Directory *>(genBFPointerFromVecPath(dirPathDest, fs));

    pair<BaseFile *, Directory *> rpair(srcBF, destDir);

    return rpair;
}

bool BaseCommand::mvDirIsWorkDirOrParent(Directory *workDir, BaseFile *mvDir) {
    // file cannot be parent
    if (!mvDir->amidir()) {
        return false;
    }

    Directory *curDir = workDir;
    while (curDir) {
        if (curDir == mvDir) {
            return true;
        }
        curDir = curDir->getParent();
    }

    // mv dir is not work dir or its parent
    return false;
}



// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> PWD <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

PwdCommand::PwdCommand(string args) : BaseCommand(std::move(args)) {

}

void PwdCommand::execute(FileSystem &fs) {
    cout << fs.getWorkingDirectory().getAbsolutePath() << endl;
}

string PwdCommand::toString() {
    return "pwd";
}

PwdCommand *PwdCommand::clone() const {
    return new PwdCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> CD <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

CdCommand::CdCommand(string args) : BaseCommand(args) {
    updateSuffix(args);
}

void CdCommand::execute(FileSystem &fs) {

    vector<string> dirPath;
    parsePath(dirPath, getArgs());

    Directory *curDir = dynamic_cast<Directory *>(genBFPointerFromVecPath(dirPath, fs));
    if (!curDir) {
        cout << "The system cannot find the path specified" << endl;
        return;
    }

    fs.setWorkingDirectory(curDir);


}

string CdCommand::toString() {
    return "cd";
}

CdCommand *CdCommand::clone() const {
    return new CdCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> LS <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

LsCommand::LsCommand(string args) : BaseCommand(args) {
    updateSuffix(args);
}

void LsCommand::execute(FileSystem &fs) {

    vector<string> path;

    //check if the input has an '-s' in prefix
    bool ifSize = false;
    if (! getSuffix().empty() && getSuffix()[0] == "-s") {
        ifSize = true;
        if (getArgs().length() >= 4)  // "-s x......"
            parsePath(path, getArgs().substr(3)); // "-s "
    }
    else parsePath(path, getArgs());

    Directory *curDir;
    if (!path.empty()) {
        curDir = dynamic_cast<Directory *>(genBFPointerFromVecPath(path, fs));
    }
    else {
        curDir=&fs.getWorkingDirectory();
    }

    if (!curDir) {
        cout << "The system cannot find the path specified" << endl;
        return;
    }


    // sorts:
    if (ifSize) curDir->sortBySize();
    else curDir->sortByName();

    for (BaseFile *bf : curDir->getChildren()){
        if (bf->amidir()) cout << "DIR\t";
            else cout <<"FILE\t";
        cout << bf->getName() << "\t" << bf->getSize() << endl ;
    }

}

string LsCommand::toString() {
    return "ls";
}

LsCommand *LsCommand::clone() const {
    return new LsCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> MKDIR <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

MkdirCommand::MkdirCommand(string args) : BaseCommand(std::move(args)) {}

void MkdirCommand::execute(FileSystem &fs) {

    vector<string> dirPath;
    parsePath(dirPath, getArgs());
    bool dididoit = false;


    Directory *pathPointer = getWorkDirPointerFromVector(dirPath, fs);

    // remove root from dirPath if its first - so iteration will start form first child in both cases
    if (dirPath[0] == "/") {
        dirPath.erase(dirPath.begin());
    }

    for (string dirInPath : dirPath) {
        if (!pathPointer->childExists(dirInPath, true) && dirInPath != "..") {
            Directory *newDir = new Directory(dirInPath, pathPointer);
            pathPointer->addFile(newDir);

            pathPointer = newDir;


            // if 1 dir was created all others inside it also will be created and not found
            dididoit = true;

        } else {
            // if last dir in path and its already exist


            pathPointer = pathPointer->getChildORFatherDirPointer(dirInPath);

            // nullptr - one of the cases is that file is in the middle or last:
            if (!pathPointer) {
                cout << "The directory already exists" << endl;
                return;
            }
        }
    }

    // if the dir already exists pathPointer should point on it
    if (pathPointer->getName() == dirPath.back() && !dididoit) {
        cout << "The directory already exists" << endl;
    }

}

string MkdirCommand::toString() {
    return "mkdir";
}

MkdirCommand *MkdirCommand::clone() const {
    return new MkdirCommand(*this);
}


// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> MKFILE <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

MkfileCommand::MkfileCommand(string args) : BaseCommand(args) {
    updateSuffix(args);
}

string MkfileCommand::toString() {
    return "mkfile";
}

void MkfileCommand::execute(FileSystem &fs) {


    vector<string> dirPath;
    parsePath(dirPath, getSuffix()[0]);
    string fileName;
    Directory *pathPointer = nullptr;

    // if dirPath is longer than 1 - args consist of path + filename
    fileName = dirPath.back();
    dirPath.pop_back();

    if (dirPath.size() > 0) {
        pathPointer = getWorkDirPointerFromVector(dirPath, fs);
    }
        // only file argument ex: mkfile f1
    else {
        pathPointer = &fs.getWorkingDirectory();
    }

    if (dirPath[0] == "/") {
        dirPath.erase(dirPath.begin());
    }

    // dir path is empty if only filename was passed as argument
    for (string dirInPath : dirPath) {
        if (!pathPointer->childExists(dirInPath, true) && dirInPath != "..") {
            cout << "The system cannot find the path specified" << endl;
            return;
        } else {
            pathPointer = pathPointer->getChildORFatherDirPointer(dirInPath);

            // nullptr:
            if (!pathPointer) {
                cout << "The system cannot find the path specified" << endl;
                return;
            }
        }
    }

    // if didn't return we are in the right dir
    if (pathPointer->childExists(fileName, false)) {
        cout << "File already exists" << endl;
        return;
    }

    // stoi converts str to int
    File *fileNew = new File(fileName, stoi(getSuffix()[1]));
    pathPointer->addFile(fileNew);

}

MkfileCommand *MkfileCommand::clone() const {
    return new MkfileCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> CP <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

CpCommand::CpCommand(string args) : BaseCommand(args) {
    updateSuffix(args);
}

string CpCommand::toString() {
    return "cp";
}

void CpCommand::execute(FileSystem &fs) {

    pair<BaseFile *, Directory *> src_dst = getSrcBaseFileAndDestDir(fs);
    BaseFile *srcBF = src_dst.first;
    Directory *destDir = src_dst.second;

    // dirs/files dont exist:
    if (!srcBF || !destDir) {
        cout << "No such file or directory" << endl;
        return;
    }

    if (destDir->fileExists(srcBF)) {
        return;
    }

    // create a copy of srcBF
    BaseFile *bfNew;
    if (srcBF->amidir()) {
        bfNew = new Directory(*dynamic_cast<Directory *>(srcBF));
        // set parent of the copied dir (before set its nullptr)
        dynamic_cast<Directory *>(bfNew)->setParent(destDir);
    } else {
        bfNew = new File(*dynamic_cast<File *>(srcBF));
    }

    // add new file/dir to destDIr
    destDir->addFile(bfNew);

}

CpCommand *CpCommand::clone() const {
    return new CpCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> MV <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

MvCommand::MvCommand(string args) : BaseCommand(args) {
    updateSuffix(args);
}

string MvCommand::toString() {
    return "mv";
}

void MvCommand::execute(FileSystem &fs) {

    pair<BaseFile *, Directory *> src_dst = getSrcBaseFileAndDestDir(fs);
    BaseFile *srcBF = src_dst.first;
    Directory *destDir = src_dst.second;

    // dirs/files dont exist:
    if (!srcBF || !destDir) {
        cout << "No such file or directory" << endl;
        return;
    }

    // if the dir we're trying to move is working dir or its ancestor - return (catches the case if its root)
    if (mvDirIsWorkDirOrParent(&fs.getWorkingDirectory(), srcBF)) {
        cout << "Can't move directory" << endl;
        return;
    }

    // if src is file need to know who is father to kill his son
    if (srcBF->amidir()) {
        destDir->addFile((dynamic_cast<Directory *>(srcBF))->getParent()->removeAndReturnChildPointerOnly(srcBF));
        (dynamic_cast<Directory *>(srcBF))->setParent(destDir);
    } else {

        vector<string> dirPathSrc;
        parsePath(dirPathSrc, getSuffix()[0]);

        // to find parent remove the last file from path
        dirPathSrc.pop_back();

        Directory *fileParent;
        // if vector is empty - only filename was passed (without parents before it) ex: mv f1 d1 -
        // thus parent == current working dir
        if (dirPathSrc.empty()) {
            fileParent = &fs.getWorkingDirectory();
        } else {
            fileParent = dynamic_cast<Directory *>(genBFPointerFromVecPath(dirPathSrc, fs));
        }

        destDir->addFile(fileParent->removeAndReturnChildPointerOnly(srcBF));
    }


}

MvCommand *MvCommand::clone() const {
    return new MvCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ERROR <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

ErrorCommand::ErrorCommand(string args) : BaseCommand(args) {}

string ErrorCommand::toString() {
    return "error";
}

void ErrorCommand::execute(FileSystem &fs) {
    vector <string> tmpV;
    parseAndUpdateArgs(tmpV, getArgs(), ' ');
    cout << tmpV[0] << ": Unknown command" << endl;
}

ErrorCommand *ErrorCommand::clone() const {
    return new ErrorCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> HISTORY <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


HistoryCommand::HistoryCommand(string args, const vector<BaseCommand *> &history) : BaseCommand(args),
                                                                                    history(history) {}

string HistoryCommand::toString() {
    return "history";
}

void HistoryCommand::execute(FileSystem &fs) {
    int i = 0;
    for (BaseCommand *cmd: history) {
        if (cmd->toString() != "error") {
            cout << i << "\t" << cmd->toString() << " " << cmd->getArgs() << endl;
        }
        else {
            cout << i << "\t"  << cmd->getArgs() << endl;
        }
        ++i;

    }
}

HistoryCommand *HistoryCommand::clone() const {
    return  new HistoryCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> EXEC <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

ExecCommand::ExecCommand(string args, const vector<BaseCommand *> &history): BaseCommand(args), history(history) {}

string ExecCommand::toString() {
    return "exec";
}

void ExecCommand::execute(FileSystem &fs) {
    int cmdNum = stoi(getArgs());
    if (cmdNum < 0 || cmdNum > (int)history.size() -1) {
        cout << "Command not found" <<  endl;
        return;
    }

    history[cmdNum]->execute(fs);
}

ExecCommand *ExecCommand::clone() const {
    return new ExecCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> RM <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

RmCommand::RmCommand(string args) : BaseCommand(args) {}

string RmCommand::toString() {
    return "rm";
}

void RmCommand::execute(FileSystem &fs) {

    vector<string> dirPath;
    parsePath(dirPath, getArgs());



    BaseFile *toRemove = genBFPointerFromVecPath(dirPath, fs);

    if (!toRemove) {
        cout << "No such file or directory" << endl;
        return;
    }

    if (mvDirIsWorkDirOrParent(&fs.getWorkingDirectory(), toRemove)) {
        cout << "Can't remove directory" << endl;
        return;
    }

    if (toRemove->amidir()) {
        Directory *dirParent = dynamic_cast<Directory*>(toRemove)->getParent();
        dirParent->removeFile(toRemove);
    } else {
        Directory *fileFatherDir;
        if (dirPath.size()==1) {
            fileFatherDir = &fs.getWorkingDirectory();
            fileFatherDir->removeFile(toRemove);
        }
        else {
            // need to pop file from dirPath
            dirPath.pop_back();
            Directory *fileFatherDir = dynamic_cast<Directory *>(genBFPointerFromVecPath(dirPath, fs));
            fileFatherDir->removeFile(toRemove);
        }
    }

}

RmCommand *RmCommand::clone() const {
    return new RmCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> RENAME <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

RenameCommand::RenameCommand(string args) : BaseCommand(args) {
    updateSuffix(args);
}

void RenameCommand::execute(FileSystem &fs) {

    vector<string> dirPathSrc;
    parsePath(dirPathSrc, getSuffix()[0]);
    string newName=getSuffix()[1];

    BaseFile *curBF = genBFPointerFromVecPath(dirPathSrc, fs);
    if (curBF == nullptr) {
        cout << "No such file or directory" << endl;
        return;
    }

    // find parent:
    dirPathSrc.pop_back();
    Directory *parentOfFile = dynamic_cast<Directory*> (genBFPointerFromVecPath(dirPathSrc, fs));
    // if parent exist - check that curBf not among children
    if (parentOfFile && parentOfFile->childExists(newName, curBF->amidir())){
        return;
    }
    else if (!parentOfFile && fs.getRootDirectory().childExists(newName, curBF->amidir())) {
        return;
    }


    if (curBF != &fs.getWorkingDirectory()){
        curBF->setName(newName);
    } else {
        cout << "Can't rename the working directory" << endl;
        return;
    }
}

string RenameCommand::toString() {
    return "rename";
}

RenameCommand *RenameCommand::clone() const {
    return new RenameCommand(*this);
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> VERBOSE <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


VerboseCommand::VerboseCommand(string args) : BaseCommand(args) {}

void VerboseCommand::execute(FileSystem &fs) {
    int VNum = stoi(getArgs());
    if (VNum < 0 || VNum > 3) {
        cout << "Wrong verbose input" <<  endl;
        return;
    }
    else verbose = VNum;
}

string VerboseCommand::toString() {
    return "verbose";
}

VerboseCommand *VerboseCommand::clone() const {
    return new VerboseCommand(*this);
}
