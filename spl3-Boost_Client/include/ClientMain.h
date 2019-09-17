//
// Created by lev on 1/5/18.
//

#ifndef BOOST_ECHO_CLIENT_CLIENTMAIN_H
#define BOOST_ECHO_CLIENT_CLIENTMAIN_H

#include <string>
#include <connectionHandler.h>

enum signoutStatus {
    NOT_STARTED,
    ASKED,
    ACKNOWLEDGED,
};

class ClientMain {
public:
    ClientMain(ConnectionHandler *ch);
    void mainLoop();
    ~ClientMain();
    void operator()();

    ClientMain(const ClientMain& other);
    ClientMain &operator=(const ClientMain &other);

    // keep this static bool for both writer/listener threads
    static signoutStatus startedSignout;

private:
    ConnectionHandler *ch;
};




#endif //BOOST_ECHO_CLIENT_CLIENTMAIN_H
