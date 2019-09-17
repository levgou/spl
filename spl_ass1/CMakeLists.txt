cmake_minimum_required(VERSION 3.8)
project(spl_ass1)

set(CMAKE_CXX_STANDARD 11)

include_directories(src)
include_directories(include)
include_directories(tests)

#src/Main.cpp
set(SOURCE_FILES tests/main.cpp
        include/Commands.h
        src/Commands.cpp
        include/Environment.h
        src/Environment.cpp
        include/Files.h
        include/FileSystem.h
        src/FileSystem.cpp
        include/GlobalVariables.h
        src/Files.cpp
        tests/filesTests.cpp
        tests/logPrint.cpp
        tests/logPrint.h
        src/FileSystem.cpp
        src/Environment.cpp
        src/Commands.cpp)


add_executable(spl_ass1 ${SOURCE_FILES})