# All Targets
all: fs

fs: bin/Main.o
	g++ -o bin/fs bin/Commands.o  bin/Environment.o  bin/Files.o  bin/FileSystem.o    bin/Main.o

###
#bin/GlobalVariables.o:
#	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/GlobalVariables.o include/GlobalVariables.h 

###
bin/Main.o: src/Main.cpp bin/Environment.o
	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/Main.o src/Main.cpp
	
###
bin/Environment.o: src/Environment.cpp  bin/Commands.o bin/Files.o
	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/Environment.o src/Environment.cpp
	
###
bin/Commands.o: src/Commands.cpp bin/FileSystem.o
	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/Commands.o src/Commands.cpp 
###
bin/FileSystem.o: src/FileSystem.cpp bin/Files.o
	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/FileSystem.o src/FileSystem.cpp 
	
###
bin/Files.o: src/Files.cpp
	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/Files.o src/Files.cpp 

#Clean the build directory
clean: 
	rm -f bin/*
	
