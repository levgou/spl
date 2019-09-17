# spl_ass2 - Actor Thread Pool implementation  

## Overview:  
  
### Motivation:
* In this assignment we implemented a course registration system
* This system includes many users, of different types - Secretaries, Computer, Students, etc  
* For each user we want to process he's requests lineary - meaning in the order they were posted
* Furthermore, some action are interdependent, so some kind of wait is needed.

### Implementation:
* We use a thread pool with a Queue for each actor (User) - which ensures order of execution
* To ensure continuum of interdependent actions, we implement the Promise design pattern, which will hold  
  status at EOE, & further execution instructions
* To ensure there is no busy poll of tasks, thread are by default in a sleep state, while a VersionMonitor is used  
  to indicate the state of the system, which is implemented using the wait/notify system
* All endangered behaviour is either synchronized, or is protected with some lock, for example - Semaphore, Count down latch, etc
* Where it is possible, we prefer the use of atomic operations - to reduce the overhead of locks  

### Notes:
* All of the above was implemented using the latest Java 1.8
