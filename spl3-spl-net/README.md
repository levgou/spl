# spl3-spl-net - implementation of a "Blockbuster" server
## Overview:

### Implements both design patterns for a TCP server process:  
* Thread per client server    
* Reactor server  

### Network protocol  
* The network protocol is based on "Text based protocol", meaning the Client-Server interface is raw text based  
* User internal protocol to interpret user commands, for ex: REGISTER/LOGIN/RENT/etc

### Processing
* In the TPC implementation, each client gets a thread, which handles requests end-to-end  
* Using the Reactor design pattern, we ensure Scalability, where 1 thread is responsible for IO,  
  while all the heavyweight processing is done using a Thread-Pool
 
 ### Actor Thread-Pool  
 * To ensure linear processing of user requests - an Actor Thread-Pool is used,  
  thus each users requests are processed in order of submission


### Notes:
* All of the above was implemented using the latest Java 1.8
