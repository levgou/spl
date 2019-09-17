//
// Created by levgou on 11/16/17.
//

#include <Environment.h>
#include <iostream>
#include <GlobalVariables.h>


using namespace std;

Environment::Environment(): commandsHistory(), fs() {}

const vector<BaseCommand *> &Environment::getHistory() const {
    return commandsHistory;
}

void Environment::addToHistory(BaseCommand *command) {
    commandsHistory.push_back(command);
}

FileSystem &Environment::getFileSystem() {
    return fs;
}

void Environment::start() {
    fs.setWorkingDirectory(&fs.getRootDirectory());
    string userCMD;

    do {
        printPrompt();
        userCMD = getUserCmd();
        if (userCMD!= "verbose 0" && userCMD!= "exit" && (verbose==2 || verbose==3))
            printFullCMD(userCMD);
        exeUserCmd(userCMD);

    } while (userCMD != "exit");

}

void Environment::printPrompt() {
    string promptStr = fs.getWorkingDirectory().getAbsolutePath() + ">";
    cout << promptStr;
}

string Environment::getUserCmd() {
    string userCMD;
    getline(cin, userCMD);
    return userCMD;
}

void Environment::exeUserCmd(string userCMD) {
    if (userCMD == "exit") {
        return;
    } else if (userCMD.size() == 0) {
        return;
    }

    BaseCommand *curCMD = decideCMD(userCMD);
    curCMD->execute(fs);
    commandsHistory.push_back(curCMD);
}

BaseCommand *Environment::decideCMD(string &userCMD) {
    pair<string, string> cmd_args;
    updateCMDandArgs(userCMD, cmd_args);
    BaseCommand *bcp = nullptr;


    if (cmd_args.first == "pwd") {
        bcp = new PwdCommand(cmd_args.second);
    }
    else if (cmd_args.first == "cd"){
        bcp=new CdCommand(cmd_args.second);
    }
    else if (cmd_args.first == "ls"){
        bcp=new LsCommand(cmd_args.second);
    }
    else if (cmd_args.first == "mkdir"){
        bcp=new MkdirCommand(cmd_args.second);
    }
    else if (cmd_args.first == "mkfile"){
        bcp=new MkfileCommand(cmd_args.second);
    }
    else if(cmd_args.first == "cp"){
        bcp=new CpCommand(cmd_args.second);
    }
    else if(cmd_args.first == "mv"){
        bcp=new MvCommand(cmd_args.second);
    }
    else if (cmd_args.first == "rm"){
        bcp=new RmCommand(cmd_args.second);
    }
    else if (cmd_args.first == "history"){
        bcp=new HistoryCommand(cmd_args.second, commandsHistory);
    }
    else if (cmd_args.first == "rename"){
        bcp=new RenameCommand(cmd_args.second);
    }
    else if (cmd_args.first == "exec"){
        bcp=new ExecCommand(cmd_args.second, commandsHistory);
    }
    else if (cmd_args.first == "verbose"){
        bcp=new VerboseCommand(cmd_args.second);
    }

    else {
        // pass both command and args - for printing
        string bothCmdAndArgs = cmd_args.first;
        if (cmd_args.second.size() >0 ){
            bothCmdAndArgs += " " + cmd_args.second;
        }

        bcp = new ErrorCommand(bothCmdAndArgs);
    }

    return bcp;
}

void Environment::updateCMDandArgs(string &userCMD, pair<string, string> &cmd_args) {
    size_t spaceIndx = userCMD.find(' ', 0);

    if (spaceIndx == string::npos) {
        cmd_args.first = userCMD;
    } else {
        cmd_args.first = userCMD.substr(0, spaceIndx);
        cmd_args.second = userCMD.substr(spaceIndx + 1, userCMD.length() - spaceIndx - 1);
    }

}

void Environment::printFullCMD(string userCMD){
    cout << userCMD << endl;
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


void Environment::cleanHistoryAndLeaveThisForsakenLand() {
    while (!commandsHistory.empty()) {
        delete commandsHistory.back();
        commandsHistory.pop_back();
    }
}

void Environment::letsCopyThatThingThatYouWantToCopy(const Environment &other) {
    for (BaseCommand *bsc: other.commandsHistory){
        commandsHistory.push_back(bsc->clone());
    }
}

void Environment::cleanUpYourMessSon() {
    while (!commandsHistory.empty()) {
        delete commandsHistory.back();
        commandsHistory.pop_back();
    }
    // fs will be out of scope & its dtor will be called
}

// >>>>>>>>>>>>>>>>>>>>>>>>>>> ctors & dtors & operator= <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

Environment::~Environment() {
    cleanHistoryAndLeaveThisForsakenLand();

    if (verbose==1 || verbose==3)
        cout << "Environment::~Environment()" << endl;
}

Environment::Environment(const Environment &other): commandsHistory(), fs(other.fs) {
    letsCopyThatThingThatYouWantToCopy(other);
    if (verbose==1 || verbose==3)
        cout << "Environment::Environment(const Environment &other)" << endl;
}

Environment::Environment(Environment &&other): commandsHistory(move(other.commandsHistory)),
                                               fs(move(other.fs)) {
    if (verbose==1 || verbose==3)
        cout << "Environment::Environment(Environment &&other)" << endl;
}

Environment &Environment::operator=(const Environment &other) {
    cleanUpYourMessSon();
    letsCopyThatThingThatYouWantToCopy(other);
    this->fs = other.fs;

    if (verbose==1 || verbose==3)
        cout << "Environment &Environment::operator=(const Environment &other)" << endl;

    return *this;
}

Environment &Environment::operator=(Environment &&other) {
    this->commandsHistory = move(other.commandsHistory);
    this->fs = move(other.fs);

    if (verbose==1 || verbose==3)
        cout << "Environment &Environment::operator=(Environment &&other)" << endl;

    return *this;
}









