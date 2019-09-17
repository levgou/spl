package bgu.spl.a2;

import bgu.spl.a2.sim.Simulator;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import bgu.spl.a2.sim.privateStates.DepartmentPrivateState;
import bgu.spl.a2.sim.privateStates.StudentPrivateState;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Or on 19/12/2017.
 */
public class IterativeTest {
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_CYAN = "\u001B[36m";
    private static int errorCount;

    private static void printError(String text) {
        System.out.println(ANSI_RED + text + ANSI_RESET);
        errorCount++;
    }

    private static void printSucc(String text) {
        System.out.println(ANSI_GREEN + text + ANSI_RESET);
    }

    private static void printHead(String text) {
        System.out.println(ANSI_CYAN + text + ANSI_RESET);
    }

    public static void main(String[] args) {
        errorCount = 0;
        printHead("####### PART 1 STARTS HERE! #######");
        final int numOfActors = 100;
        final int actionsPerActor = 300;
        final int threads = 20;
        System.out.println("CREATING " + numOfActors + " ACTORS");
        CountDownLatch actorslatch = new CountDownLatch(numOfActors);
        ActorThreadPool pool = new ActorThreadPool(threads);
        for (int i = 1; i <= numOfActors; i++) {
            final int id = i;
            pool.submit(new Action<String>() {
                @Override
                protected void start() {
                    setActionName("Created actor " + id);
                    complete("Actor " + id + " created");
                    actorslatch.countDown();
                }
            }, "Actor" + id, new PrivateState() {
            });
        }
        pool.start();

        try {
            actorslatch.await(5, TimeUnit.SECONDS);
            if (actorslatch.getCount() > 0) {
                printError(actorslatch.getCount() + " actors were not created (action never finished)!");
            } else if (pool.getActors().size() != numOfActors) {
                printError(numOfActors - pool.getActors().size() + " actors were not added to the pool!");
            } else {
                printSucc("TEST OF ACTOR CREATION WAS COMPLETED WITH SUCCESS!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("ADDING " + numOfActors * actionsPerActor + " MINI-ACTIONS TO THE ACTORS IN THE POOL");
        CountDownLatch miniLatch = new CountDownLatch(numOfActors * actionsPerActor);
        for (int k = 1; k <= numOfActors * actionsPerActor; k++) {
            final String nextActor = "Actor" + (1 + (k % (numOfActors)));
            final int name = k;
            Action<String> miniAction = new Action<String>() {
                @Override
                protected void start() {
                    setActionName("SubAction " + name);
                    complete("Action num " + name + " is completed");
                }
            };
            miniAction.getResult().subscribe(new callback() {
                @Override
                public void call() {
                    miniLatch.countDown();
                }
            });
            pool.submit(miniAction, nextActor, pool.getPrivateState(nextActor));
        }

        try {
            miniLatch.await(5, TimeUnit.SECONDS);
            if (miniLatch.getCount() > 0) {
                printError(miniLatch.getCount() + " actions are not done!");
            } else {
                printSucc("TEST OF MINI-ACTIONS WAS COMPLETED WITH SUCCESS!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // for each of the actors we created we will add one big action
        System.out.println("ADDING " + numOfActors + " BIG-ACTIONS TO THE ACTORS IN THE POOL");
        CountDownLatch megaLatch = new CountDownLatch(numOfActors);
//        miniLatch = new CountDownLatch(numOfActors*(actionsPerActor-1));
        for (int j = 1; j <= numOfActors; j++) {
            final int currentActorID = j;
            String currentActorName = "Actor" + j;
            PrivateState actor = new PrivateState() {
            };
            Collection<Action<String>> actions = new LinkedList<>();
            Action<String> bigAction = new Action<String>() {
                protected List<String> completedSubs;

                @Override
                protected void start() {
                    setActionName("BigAction of " + currentActorName);
                    //the big actions will send sub-actions to other actors
                    completedSubs = new ArrayList<>();
                    for (int i = 1; i <= actionsPerActor - 1; i++) {
                        final String nextActor = "Actor" + (1 + ((currentActorID + i) % (numOfActors)));
                        final int name = i;
                        Action<String> subAction = new Action<String>() {
                            @Override
                            protected void start() {
                                setActionName("SubAction " + name);
                                complete("Action num " + name + " is completed");
                            }
                        };
                        actions.add(subAction);
                        sendMessage(subAction, nextActor, pool.getPrivateState(nextActor));
                    }
                    // after all the sub-actions are finished we will finish the big action
                    then(actions, new callback() {
                        @Override
                        public void call() {
                            complete("Big action of " + currentActorName + " is completed");
                        }
                    });
                }
            };
            pool.submit(bigAction, currentActorName, actor);
            bigAction.getResult().subscribe(new callback() {
                @Override
                public void call() {
                    megaLatch.countDown();
                }
            });
        }
        try {
            megaLatch.await(5, TimeUnit.SECONDS);
            if (megaLatch.getCount() > 0) {
                printError(megaLatch.getCount() + " actors didn't finish their mega-actions! (if you pass previous test," +
                        "check your Action.then(...) method [THIS IS NOT 100% THE PROBLEM]");
            } else {
                printSucc("TEST OF MEGA-ACTIONS WAS COMPLETED WITH SUCCESS!");
            }
            System.out.println("CHECKING IF ALL OF THE THREADS ARE IDLE (SHOULD BE WAITING)");
            Thread.sleep(1000);
            int waiting = 0;
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            for (Thread thread : threadSet) {
                if (thread.getName().contains("Thread")) {
                    if (thread.getState().equals(Thread.State.WAITING)) {
                        waiting++;
                    }
                }
            }
            if (waiting != threads) {
                if (waiting == 0) {
                    printError("ALO: All the threads are not in waiting state, make sure you use VersionMonitor" +
                            " when no actions are in the pool to avoid busy waiting!" +
                            " Also make sure that Version Monitor is using Thread.wait()");
                } else {
                    printError(threads - waiting + " threads are not in WAITING state when should be!");
                }
            } else {
                printSucc("TEST OF WAITING THREADS WAS COMPLETED WITH SUCCESS!");
            }

            System.out.println("TESTING SHUTDOWN");
            Thread shutter = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        pool.shutdown();
                    } catch (InterruptedException e) {
                        System.out.println("Pool shutdown took to long! - interrupting shutdown!");
                    }
                }
            });
            shutter.start();
            Thread.sleep(1000);
            shutter.interrupt();
            shutter.join();
            waiting = 0;
            for (Thread thread : threadSet) {
                if (thread.getName().contains("Thread")) {
                    if (thread.getState().equals(Thread.State.WAITING)) {
                        waiting++;
                    }
                }
            }
            if (waiting > 0) {
                if (waiting == threads) {
                    printError("ALO: all of the threads are in WAITING state, and shouldn't be!!!");
                } else {
                    printError(waiting + " threads are in WAITING state, and shouldn't be!");
                }
                System.out.println("The threads should be informed via VersionMonitor that a shutdown is performed!");
            } else {
                printSucc("TEST OF SHUTDOWN WAS COMPLETED WITH SUCCESS!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (errorCount == 0) {
            printHead("####### PART 1 COMPLETED WITH NO ERRORS #######");
        } else {
            printError("ALO: ####### PART 1 FINISHED WITH " + errorCount + " ERRORS!  #######");
        }


//         SECOND PART TEST:
        int imale = 1;
        while (true) {
            errorCount = 0;
            printHead("####### PART 2 STARTS HERE! #######");
            String[] input = {"src/main/java/bgu/spl/a2/testInput.json"};
            CountDownLatch simLatch = new CountDownLatch(1);
            Thread shutter = new Thread(new Runnable() {
                @SuppressWarnings("Duplicates")
                @Override
                public void run() {
                    try {
                        Simulator.main(input);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    simLatch.countDown();
                }
            });
            shutter.start();
            try {
                simLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
            }
            if (simLatch.getCount() != 0) {
                if (shutter.getState().equals(Thread.State.WAITING)) {
                    printError("ALO: STOPPED SIMULATOR DUE TO TIMEOUT! (it took too long, probably got stuck...)");
                }
                shutter.interrupt();
            }
            try {
                // Reading the object from a file
                FileInputStream file = new FileInputStream("result.ser");
                ObjectInputStream in = new ObjectInputStream(file);

                // Method for deserialization of object
                HashMap<String, PrivateState> result = (HashMap<String, PrivateState>) in.readObject();
                in.close();
                file.close();

                //Department 1 test
                DepartmentPrivateState dep1 = ((DepartmentPrivateState) result.get("dep1"));
                for (int i = 1; i <= 3; i++) {
                    if (!dep1.getCourseList().contains("course1-" + i)) {
                        printError("ALO: dep1 does not contain course1-" + i + "and should!");
                    }
                }
                for (int i = 1; i <= 12; i++) {
                    if (!dep1.getStudentList().contains("student" + i)) {
                        printError("ALO: dep1 does not contain student" + i + " and should!");
                    }
                }
                //Department 2 test
                DepartmentPrivateState dep2 = ((DepartmentPrivateState) result.get("dep2"));
                for (int i = 1; i <= 1; i++) {
                    if (!dep2.getCourseList().contains("course2-" + i)) {
                        printError("ALO: dep2 does not contain course2-" + i + " and should!");
                    }
                }
                for (int i = 1; i <= 6; i++) {
                    if (!dep2.getStudentList().contains("student2-" + i)) {
                        printError("ALO: dep2 does not contain student2-" + i + " and should!");
                    }
                }

                //Course 1 test
                CoursePrivateState course1 = ((CoursePrivateState) result.get("course1-1"));
                if (course1.getAvailableSpots() != 3) {
                    printError("ALO: course1-1 should have 3 available spots but have " + course1.getAvailableSpots());
                }
                if (course1.getRegistered() != 7) {
                    printError("ALO: course1-1 should have 7 registered students but have " + course1.getRegistered());
                }
                if (!course1.getPrequisites().isEmpty()) {
                    printError("ALO: course1-1 should not have prerequisites but has " + course1.getPrequisites().size());
                }
                if (course1.getRegStudents().size() != course1.getRegistered()) {
                    printError("ALO: course 1-1 number of actual registered students (" + course1.getRegStudents().size() + ") do not match" +
                            " the indication (" + course1.getRegistered() + ")");
                }

                //Course 2 test
                CoursePrivateState course2 = ((CoursePrivateState) result.get("course1-2"));
                if (course2.getAvailableSpots() != 4) {
                    printError("ALO: course1-2 should have 4 available spots but have " + course2.getAvailableSpots());
                }
                if (course2.getRegistered() != 7) {
                    printError("ALO: course1-2 should have 7 registered students but have " + course2.getRegistered());
                }
                if (!course2.getPrequisites().isEmpty()) {
                    printError("ALO: course1-2 should not have prerequisites but has " + course2.getPrequisites().size());
                }
                if (course2.getRegStudents().size() != course2.getRegistered()) {
                    printError("ALO: course 1-2 number of actual registered students (" + course2.getRegStudents().size() + ") do not match" +
                            " the indication (" + course2.getRegistered() + ")");
                }
                //Course 3 test
                CoursePrivateState course3 = ((CoursePrivateState) result.get("course1-3"));
                if (course3.getAvailableSpots() != 5) {
                    printError("ALO: course1-3 should have 5 available spots but have " + course3.getAvailableSpots());
                }
                if (course3.getRegistered() != 5) {
                    printError("ALO: course1-3 should have 5 registered students but have " + course3.getRegistered());
                }
                if (course3.getPrequisites().size() != 2) {
                    printError("ALO: course1-3 should have 2 prerequisites but has " + course3.getPrequisites().size());
                }
                if (course3.getRegStudents().size() != course3.getRegistered()) {
                    printError("ALO: course 1-3 number of actual registered students (" + course3.getRegStudents().size() + ") do not match" +
                            " the indication (" + course3.getRegistered() + ")");
                }
                if (course1.getRegStudents().contains("student6")) {
                    printError("ALO: student6 should have been removed from course1-1 but is still there!");
                    printError("ALO: this test is very strict and checks if the removal only happens after the student has been added");
                }
                if (course1.getRegStudents().contains("student7")) {
                    printError("ALO: student7 should have been removed from course1-1 but is still there!");
                    printError("ALO: this test is very strict and checks if the removal only happens after the student has been added");
                }
                if (course1.getRegStudents().contains("student8")) {
                    printError("ALO: student8 should have been removed from course1-1 but is still there!");
                    printError("ALO: this test is very strict and checks if the removal only happens after the student has been added");
                }
                //Course 4 test
                CoursePrivateState course4 = ((CoursePrivateState) result.get("course1-4"));
                if (course4.getAvailableSpots() != -1) {
                    // TODO: 12/26/17 put breakpoint here!!! 
                    printError("ALO: course1-4 was closed and should have -1 available spots but has " + course4.getAvailableSpots());
                }
                if (course4.getRegistered() != 0) {
                    printError("ALO: course1-4 should have 0 registered studnets but has " + course4.getRegistered());
                }
                if (course4.getRegStudents().size() != 0) {
                    printError("ALO: course1-4 should not have actual registered studnets but has " + course4.getRegStudents().size());
                }
                //Course 5 test
                CoursePrivateState course5 = ((CoursePrivateState) result.get("course2-1"));
                if (course5.getAvailableSpots() != 0) {
                    printError("ALO: course2-1 should have no available spots but has " + course5.getAvailableSpots());
                }
                if (course5.getRegistered() != 5) {
                    printError("ALO: course2-1 should have 5 registered studnets but has " + course5.getRegistered());
                    if (course5.getRegistered() == 6) {
                        printError("ALO: make sure that you check for available spots before you add a student to a course");
                    }
                }
                if (course5.getRegStudents().size() != 5) {
                    printError("ALO: course2-1 should have 5 actual registered studnets but has " + course5.getRegStudents().size());
                }


                // Students
                StudentPrivateState student6 = (StudentPrivateState) result.get("student6");
                if (student6.getGrades().containsKey("course1-1")) {
                    printError("ALO: student6 still has grades for course1-1! (which he was removed from)");
                }
                StudentPrivateState student7 = (StudentPrivateState) result.get("student7");
                if (student7.getGrades().containsKey("course1-1")) {
                    printError("ALO: student7 still has grades for course1-1! (which he was removed from)");
                }
                StudentPrivateState student8 = (StudentPrivateState) result.get("student8");
                if (student8.getGrades().containsKey("course1-1")) {
                    printError("ALO: student8 still has grades for course1-1! (which he was removed from)");
                }
                StudentPrivateState student1 = (StudentPrivateState) result.get("student1");
                if (student1.getGrades().containsKey("course1-4")) {
                    printError("ALO: student1 still has grades for course1-4! (which was closed)");
                }
                StudentPrivateState student2 = (StudentPrivateState) result.get("student2");
                if (student2.getGrades().containsKey("course1-4")) {
                    printError("ALO: student2 still has grades for course1-4! (which was closed)");
                }
                StudentPrivateState student3 = (StudentPrivateState) result.get("student3");
                if (student3.getGrades().containsKey("course1-4")) {
                    printError("ALO: student3 still has grades for course1-4! (which was closed)");
                }
                StudentPrivateState student12 = (StudentPrivateState) result.get("student12");
                if (!student12.getGrades().containsKey("course1-2") | student12.getGrades().size() != 1) {
                    printError("ALO: student12 should have grades for course1-2! (registered with preferences)");
                }
                if (student12.getGrades().size() != 1) {
                    printError("ALO: student12 was supposed to register to exactly 1 course but is registered to " + student12.getGrades().size());
                }
                StudentPrivateState student13 = (StudentPrivateState) result.get("student13");
                if (!student13.getGrades().containsKey("course1-2")) {
                    printError("ALO: student13 should have grades for course1-2! (registered with preferences)");
                }
                if (student13.getGrades().size() != 1) {
                    printError("ALO: student13 was supposed to register to exactly 1 course but is registered to " + student13.getGrades().size());
                }

                //Admin Check

                StudentPrivateState student4 = (StudentPrivateState) result.get("student4");
                StudentPrivateState student11 = (StudentPrivateState) result.get("student11");
               // if (student1.getSignature() != 111) {
             //       printError("ALO: ALO: Student1 signature is not right! should be 111! found " + student1.getSignature());
           //     }
                if (student12.getSignature() != 222) {
                    printError("ALO: ALO: Student2 signature is not right! should be 222! found " + student12.getSignature());
                }
                if (student2.getSignature() != 111) {
                    printError("ALO: ALO: Student 1 signature is not right! should be 111! found " + student2.getSignature());
                }
                if (student3.getSignature() != 111) {
                    printError("ALO: ALO: Student 1 signature is not right! should be 111! found " + student3.getSignature());
                }
                if (student4.getSignature() != 333) {
                    printError("ALO: ALO: Student 1 signature is not right! should be 333! found " + student4.getSignature());
                }
                if (student11.getSignature() != 444) {
                    printError("ALO: ALO: Student 1 signature is not right! should be 444! found " + student11.getSignature());
                }

                if (errorCount == 0) {
                    printHead("####### PART 2 COMPLETED WITH NO ERRORS ####### iter num: " + imale);
                    imale++;
                } else {
                    printError("ALO: ALO: ####### PART 2 FINISHED WITH " + errorCount + " ERRORS!  #######");
                }
                Thread.sleep(100);
            } catch (FileNotFoundException e1) {
                printError("ALO: ALO: PLEASE MAKE SURE THAT testInput.txt IS IN \"src/main/java/bgu/spl/a2/\"");
            } catch (IOException | ClassNotFoundException e2) {
                e2.printStackTrace();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}