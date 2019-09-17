//
// Created by lev on 1/5/18.
//

#include "ClientMain.h"
#include <boost/thread.hpp>

#include <boost/algorithm/string/predicate.hpp>


signoutStatus ClientMain::startedSignout = NOT_STARTED;
void ClientMain::mainLoop() {

    try {
        while (ClientMain::startedSignout != ACKNOWLEDGED) {
            const short bufsize = 1024;
            char buf[bufsize];

            std::cin.getline(buf, bufsize);
            std::string line(buf);

//            std::cout << "got: " << line;

            if (boost::starts_with(line, "SIGNOUT")) {
                ClientMain::startedSignout = ASKED;
            }

            if (!ch->sendLine(line)) {
//                cout << "Disconnected. Exiting...\n";
                break;
            }

            // connectionHandler.sendLine(line) appends '\n' to the message. Therefor we send len+1 bytes.
//            std::cout << "Sent <" << line << "> to server";
        }
    }

    catch (boost::thread_interrupted &) {
//        cout << "Disconnected. Exiting...\n";
    }

    return;
}

void ClientMain::operator()() {
    mainLoop();
}

ClientMain::~ClientMain() {

}

ClientMain::ClientMain(ConnectionHandler *ch) :
        ch(ch) {}

ClientMain::ClientMain(const ClientMain &other): ch(other.ch) {}

ClientMain &ClientMain::operator=(const ClientMain &other) {
    ch = other.ch;
    return *this;
}




