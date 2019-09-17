//
// Created by lev on 1/3/18.
//

#include <connectionHandler.h>
#include "ServerListener.h"
#include "ClientMain.h"


ServerListener::ServerListener(ConnectionHandler *ch) :
        m_ch(ch) {}

// for threading
void ServerListener::operator()() {

    while (ClientMain::startedSignout != ACKNOWLEDGED) {
        std::string serverInput;
        if (!m_ch->getLine(serverInput)) {
//            cout << "Disconnected. Exiting..." << endl;
            break;

        } else {
            handleServerInput(serverInput);
        }

    }

}

bool ServerListener::handleServerInput(string serverInput) {
    cout << serverInput << flush;


    if (ClientMain::startedSignout == ASKED && serverInput == "ACK signout succeeded\n"){
        ClientMain::startedSignout = ACKNOWLEDGED;
    }

    return true;
}

ServerListener::ServerListener(const ServerListener &other): m_ch(other.m_ch) {}

ServerListener &ServerListener::operator=(const ServerListener &other) {
    m_ch = other.m_ch;
    return *this;
}
