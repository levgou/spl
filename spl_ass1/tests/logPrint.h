//
// Created by levgou on 11/11/17.
//

#ifndef SPL_ASS1_LOGPRINT_H
#define SPL_ASS1_LOGPRINT_H

#include <iostream>
#include <ctime>

using namespace std;

class logPrint {
public:

    string genTimeStamp();

    void infoPrint(string msg, string funcName);
};


#endif //SPL_ASS1_LOGPRINT_H
