#include <connectionHandler.h>
#include <boost/thread.hpp>
#include "ServerListener.h"
#include "ClientMain.h"



int main(int argc, char *argv[]) {

    if (argc < 3) {
        cout << "Usage: " << argv[0] << " host port";
        return -1;
    }

    std::string host = argv[1];
    short port = atoi(argv[2]);

    ConnectionHandler ch(host, port);
    if (!ch.connect()) {
        cout << "Cannot connect to " << host << ":" << port;
        return 1;
    }

    ClientMain clientMain(&ch);
    boost::thread writer(clientMain);

    ServerListener slis(&ch);
    boost::thread lis(slis);

    cout << "Established connection to " << host << ":" << port << endl;

//    clientMain();
    lis.join();

    return 0;
}
