CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude -DDEBUG
LDFLAGS:=-lboost_system -lboost_thread

all: BBclient
	g++ -o bin/BBclient bin/connectionHandler.o bin/ServerListener.o bin/ClientMain.o bin/BBclient.o $(LDFLAGS) 
	
BBclient: src/BBclient.cpp bin/ClientMain.o bin/ServerListener.o bin/connectionHandler.o
	g++ $(CFLAGS) -o bin/BBclient.o src/BBclient.cpp
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp

bin/ServerListener.o: src/ServerListener.cpp bin/ClientMain.o
	g++ $(CFLAGS) -o bin/ServerListener.o src/ServerListener.cpp
	
bin/ClientMain.o: src/ClientMain.cpp bin/connectionHandler.o
	g++ $(CFLAGS) -o bin/ClientMain.o src/ClientMain.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
