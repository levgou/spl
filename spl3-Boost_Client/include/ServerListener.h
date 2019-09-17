//
// Created by lev on 1/3/18.
//

#ifndef BOOST_ECHO_CLIENT_SERVERLISTENER_H
#define BOOST_ECHO_CLIENT_SERVERLISTENER_H

#include <string>
#include <boost/thread.hpp>

using namespace std;


class ServerListener {

private:
    ConnectionHandler *m_ch;


public:
    ServerListener(ConnectionHandler *ch);

    void operator()();

    bool handleServerInput(string serverInput);

    ServerListener(const ServerListener &other);

    ServerListener &operator=(const ServerListener &other);
};


#endif //BOOST_ECHO_CLIENT_SERVERLISTENER_H
