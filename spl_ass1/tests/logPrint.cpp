//
// Created by levgou on 11/11/17.
//

#include <chrono>
#include <iomanip>
#include <ctime>
#include <iostream>
#include <sstream>
#include "logPrint.h"

string logPrint::genTimeStamp() {

    auto t = std::time(nullptr);
    auto tm = *std::localtime(&t);
    auto time_stamp = std::put_time(&tm, "%d-%m-%Y_%H-%M-%S");
    std::stringstream buffer;
    buffer << time_stamp;

    return buffer.str();
}

void logPrint::infoPrint(string msg, string funcName) {

    cout << genTimeStamp() << " - " << funcName << " - INFO - " << msg;

}


