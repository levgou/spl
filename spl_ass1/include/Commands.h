#ifndef COMMANDS_H_
#define COMMANDS_H_

#include <string>
#include "FileSystem.h"


class BaseCommand {
private:
	string args;
	vector<string> suffix;

public:
	BaseCommand(string args);
	string getArgs();
	virtual void execute(FileSystem & fs) = 0;
	virtual string toString() = 0;
    virtual BaseCommand * clone() const =0;
	void updateSuffix(string& args);
	const vector<string>& getSuffix();
	static void parsePath (vector<string> & vec, string path);
    static BaseFile *getFirstBaseFilePointerFromVector(vector<string> &pathVec, FileSystem &fs);
    static Directory * getWorkDirPointerFromVector(vector<string> &pathVec, FileSystem &fs);
    static BaseFile *genBFPointerFromVecPath(vector<string> &pathVec, FileSystem &fs);
    pair<BaseFile*, Directory*> getSrcBaseFileAndDestDir(FileSystem &fs); // good for both cp & mv
	static void parseAndUpdateArgs(vector<string> &argsVec,string userArgs, char delim);
	bool mvDirIsWorkDirOrParent(Directory *workDir, BaseFile *mvDir);
    virtual ~BaseCommand();

};

class PwdCommand : public BaseCommand {
private:
public:
	PwdCommand(string args);
	void execute(FileSystem & fs); // Every derived class should implement this function according to the document (pdf)
	virtual string toString();
    virtual PwdCommand * clone() const;

};

class CdCommand : public BaseCommand {
private:
public:
	CdCommand(string args);
	void execute(FileSystem & fs);
	string toString();
    virtual CdCommand * clone() const;

};

class LsCommand : public BaseCommand {
private:
public:
	LsCommand(string args);
	void execute(FileSystem & fs);
	string toString();
    virtual LsCommand * clone() const;

};

class MkdirCommand : public BaseCommand {
private:
public:
	MkdirCommand(string args);
	void execute(FileSystem & fs);
	string toString();
    virtual MkdirCommand * clone() const;

};

class MkfileCommand : public BaseCommand {
private:
public:
	MkfileCommand(string args);
	void execute(FileSystem & fs);
	string toString();
    virtual MkfileCommand * clone() const;

};

class CpCommand : public BaseCommand {
private:
public:
	CpCommand(string args);
	void execute(FileSystem & fs);
	string toString();
    virtual CpCommand * clone() const;

};

class MvCommand : public BaseCommand {
private:
public:
	MvCommand(string args);
	void execute(FileSystem & fs);
	string toString();
    virtual MvCommand * clone() const;

};

class RenameCommand : public BaseCommand {
private:
public:
	RenameCommand(string args);
	void execute(FileSystem & fs);
	string toString();
    virtual RenameCommand * clone() const;

};

class RmCommand : public BaseCommand {
private:
public:
	RmCommand(string args);
	void execute(FileSystem & fs);
	string toString();
    virtual RmCommand * clone() const;

};

class HistoryCommand : public BaseCommand {
private:
	const vector<BaseCommand *> & history;
public:
	HistoryCommand(string args, const vector<BaseCommand *> & history);
	void execute(FileSystem & fs);
	string toString();
    virtual HistoryCommand * clone() const;

};


class VerboseCommand : public BaseCommand {
private:
public:
	VerboseCommand(string args);
	void execute(FileSystem & fs);
	string toString();
    virtual VerboseCommand * clone() const;

};

class ErrorCommand : public BaseCommand {
private:
public:
	ErrorCommand(string args);
	void execute(FileSystem & fs);
	string toString();
    virtual ErrorCommand * clone() const;

};

class ExecCommand : public BaseCommand {
private:
	const vector<BaseCommand *> & history;
public:
	ExecCommand(string args, const vector<BaseCommand *> & history);
	void execute(FileSystem & fs);
	string toString();
    virtual ExecCommand * clone() const;

};


#endif
