#ifndef ENVIRONMENT_H_
#define ENVIRONMENT_H_

#include "Files.h"
#include "Commands.h"

#include <string>
#include <vector>

using namespace std;

class Environment {
private:
    vector<BaseCommand *> commandsHistory;
    FileSystem fs;

    void printPrompt();

    string getUserCmd();

    void exeUserCmd(string userCMD);

    BaseCommand *decideCMD(string &userCMD);

    void updateCMDandArgs(string &userCMD, pair<string, string> &cmd_args);

    void cleanHistoryAndLeaveThisForsakenLand();

    void printFullCMD(string userCMD);

    void letsCopyThatThingThatYouWantToCopy(const Environment &other);

    void cleanUpYourMessSon();

public:
    Environment();

    // >>>>>>>>>>>>>>>>>>>>>>>>>>> ctors & dtors & operator= <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    ~Environment();

    Environment(const Environment &other);

    Environment(Environment &&other);

    Environment &operator=(const Environment &other);

    Environment &operator=(Environment &&other);

    void start();

    FileSystem &getFileSystem(); // Get a reference to the file system
    void addToHistory(BaseCommand *command); // Add a new command to the history
    const vector<BaseCommand *> &getHistory() const; // Return a reference to the history of commands
};

#endif