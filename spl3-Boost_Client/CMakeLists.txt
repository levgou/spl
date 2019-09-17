cmake_minimum_required(VERSION 3.8)
project(Boost_Echo_Client)

set(CMAKE_CXX_STANDARD 11)

include_directories(include)
include_directories(src)
include_directories(tests)

set(SOURCE_FILES
        include/connectionHandler.h
        src/connectionHandler.cpp
        src/BBclient.cpp
        src/ServerListener.cpp
        include/ServerListener.h
        src/ClientMain.cpp include/ClientMain.h)

set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)
find_package(Boost 1.62.0 COMPONENTS system thread REQUIRED)

if (Boost_FOUND)
    MESSAGE(STATUS "Boost_FOUND: " ${Boost_FOUND})

    include_directories(${Boost_INCLUDE_DIRS})
    add_executable(Boost_Echo_Client ${SOURCE_FILES})
    target_link_libraries(Boost_Echo_Client ${Boost_LIBRARIES} ${CMAKE_THREAD_LIBS_INIT})
endif ()