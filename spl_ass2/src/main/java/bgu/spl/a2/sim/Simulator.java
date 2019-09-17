/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.spl.a2.sim;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import bgu.spl.a2.Action;
import bgu.spl.a2.ActorThreadPool;
import bgu.spl.a2.PrivateState;
import bgu.spl.a2.sim.actions.*;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import bgu.spl.a2.sim.privateStates.DepartmentPrivateState;
import bgu.spl.a2.sim.privateStates.StudentPrivateState;
import bgu.spl.a2.structures.StringableHashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

/**
 * A class describing the simulator for part 2 of the assignment
 */
public class Simulator {


    private static ActorThreadPool actorThreadPool = null;
    private static Map<String, ?> myMap;
    private static Map<String, DepartmentPrivateState> dpss;

    /**
     * Begin the simulation Should not be called before attachActorThreadPool()
     */
    public static void start() {
        if (actorThreadPool == null) {
            return;
        }
        actorThreadPool.start();
    }

    /**
     * attach an ActorThreadPool to the Simulator, this ActorThreadPool will be used to run the simulation
     *
     * @param myActorThreadPool - the ActorThreadPool which will be used by the simulator
     */
    public static void attachActorThreadPool(ActorThreadPool myActorThreadPool) {
        actorThreadPool = myActorThreadPool;
        ActionServices.setThreadPool(myActorThreadPool);
    }

    /**
     * shut down the simulation
     * returns list of private states
     */
    public static HashMap<String, PrivateState> end() throws InterruptedException {
        actorThreadPool.shutdown();
        return (HashMap<String, PrivateState>) actorThreadPool.getActors();

    }


    /**
     * @param args argument for jason path
     * @return
     * @throws IOException
     *
     *  flow:
     *  1. create a new thread pool
     *  2. save it as actorThreadPool
     *  3. start the thread pool
     *  4. exe all phases from jason by submitting actions to the pool
     *  4. wait on action between phases using count down latch
     *  5. shutdown threadpool
     *  6. write results to : result.ser
     *
     */
    public static int main(String[] args) throws IOException {

        try {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, ?>>() {
            }.getType();
            JsonReader reader = new JsonReader(new FileReader(args[0]));
            myMap = gson.fromJson(reader, type);

        } catch (Exception e) {
            return 1;
        }

        ActorThreadPool pool = new ActorThreadPool(((Double) myMap.get("threads")).intValue());
        Simulator.attachActorThreadPool(pool);
        initComputers();
        HashMap<String, PrivateState> SimulationResult;
        Simulator.start();

        try {
            ExecutePhase("Phase 1");
            ExecutePhase("Phase 2");
            ExecutePhase("Phase 3");
            SimulationResult = end();
        } catch (InterruptedException ex) {
            return 1;
        }


        FileOutputStream fout = new FileOutputStream("result.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(SimulationResult);

        return 0;
    }


    /**
     * @param phase phase string - representing the phase num
     * @throws InterruptedException
     *
     *  get list of actions from jason according to phase
     *  generate actions according to list - and submit them to pool - and wait for them to finish
     */
    private static void ExecutePhase(String phase) throws InterruptedException {

        Collection<Map> actions = (Collection<Map>) myMap.get(phase);

        CountDownLatch l = new CountDownLatch(actions.size());
        for (Map a : actions) {

            ImmutableTriple<Action<?>, String, PrivateState> action = defineAction(a);
            // all null if its end of registration

            actorThreadPool.submit(action.left, action.middle, action.right);
            action.left.getResult().subscribe(() -> action.right.addRecord(action.left.getActionName()));

            action.left.getResult().subscribe(() -> {
                l.countDown();
            });
        }

        l.await();

    }


    private static void submitEndOfReg(CountDownLatch l) {
        for (String department : dpss.keySet()) {
            AnnounceEndOfReg aeor = new AnnounceEndOfReg(dpss.get(department), department);
            actorThreadPool.submit(aeor, department, dpss.get(department));
            dpss.get(department).addRecord("End Registeration");
            aeor.getResult().subscribe(() -> l.countDown());

        }
    }


    /**
     *  add computers from jason to warehouse instance
     */
    private static void initComputers() {

        Collection<Computer> computers = new ArrayList<>();
        for (Map<String, String> s : (List<Map<String, String>>) myMap.get("Computers")) {
            String compName = s.get("Type");
            Long success = Long.parseLong(s.get("Sig Success"));
            Long fail = Long.parseLong(s.get("Sig Fail"));
            computers.add(new Computer(compName, success.longValue(), fail.longValue()));
        }
        Warehouse.getInstance().addComputers(computers);
    }


    /**
     * @param actMap get map of action from jason
     * @return  action instance out of available classes
     */
    private static ImmutableTriple<Action<?>, String, PrivateState> defineAction(Map actMap) {

        if (actMap.get("Action").equals("Open Course")) {

            String departmentName = (String) actMap.get("Department");
            String courseName = (String) actMap.get("Course");
            List<String> pre = (List<String>) actMap.get("Prerequisites");
            int spaces = getIntFromString((String) actMap.get("Space"));
            // department could be absent from map when adding course
            DepartmentPrivateState dps = getOrGenDepartmentActor(departmentName);

            OpenNewCourse openNewCourse = new OpenNewCourse(
                    spaces,
                    pre,
                    courseName,
                    departmentName);

            genCoursePSInPool(courseName, spaces, pre);
            return new ImmutableTriple<>(openNewCourse, departmentName, dps);

        } else if (actMap.get("Action").equals("Add Student")) {
            String departmentName = (String) actMap.get("Department");
            String studentName = (String) actMap.get("Student");

            // department could be absent from map when adding course
            DepartmentPrivateState dps = getOrGenDepartmentActor(departmentName);

            AddStudent addStudent = new AddStudent(studentName, departmentName);

            StudentPrivateState sps = new StudentPrivateState();
            addStudentPStoPool(studentName, sps);
            return new ImmutableTriple<>(addStudent, departmentName, dps);

        } else if (actMap.get("Action").equals("Participate In Course")) {

            Integer grade = getIntFromString(((List<String>) actMap.get("Grade")).get(0));

            ParticipateInCourse participate = new ParticipateInCourse(
                    ((String) actMap.get("Student")),
                    (StudentPrivateState) actorThreadPool.getPrivateState((String) actMap.get("Student")),
                    ((String) actMap.get("Course")),
                    grade.intValue());

            return new ImmutableTriple<>(participate, ((String) actMap.get("Course")),
                    actorThreadPool.getPrivateState((String) actMap.get("Course")));

        } else if (actMap.get("Action").equals("Unregister")) {

            Unregister unreg = new Unregister(
                    ((CoursePrivateState) actorThreadPool.getPrivateState((String) actMap.get("Course"))),
                    (StudentPrivateState) actorThreadPool.getPrivateState((String) actMap.get("Student")),
                    ((String) actMap.get("Course")),
                    ((String) actMap.get("Student")));

            return new ImmutableTriple<>(unreg, ((String) actMap.get("Course")),
                    actorThreadPool.getPrivateState((String) actMap.get("Course")));

        } else if (actMap.get("Action").equals("Close Course")) {

            CloseCourse close = new CloseCourse(
                    ((CoursePrivateState) actorThreadPool.getPrivateState((String) actMap.get("Course"))),
                    ((String) actMap.get("Course")),
                    (DepartmentPrivateState) actorThreadPool.getPrivateState((String) actMap.get("Department")),
                    ((String) actMap.get("Department")));

            return new ImmutableTriple<>(close, ((String) actMap.get("Department")),
                    actorThreadPool.getPrivateState((String) actMap.get("Department")));

        } else if (actMap.get("Action").equals("Add Spaces")) {

            AddPlacesToCourse addSpaces = new AddPlacesToCourse(
                    ((CoursePrivateState) actorThreadPool.getPrivateState((String) actMap.get("Course"))),
                    Integer.parseInt((String) actMap.get("Number")));

            return new ImmutableTriple<>(addSpaces, ((String) actMap.get("Course")),
                    actorThreadPool.getPrivateState((String) actMap.get("Course")));

        } else if (actMap.get("Action").equals("Administrative Check")) {

            List<String> students = (List<String>) actMap.get("Students");
            List<ImmutablePair<String, StudentPrivateState>> studentsPairs = new ArrayList<>();
            for (String st : students) {
                studentsPairs.add(new ImmutablePair<>(st, (StudentPrivateState) actorThreadPool.getPrivateState(st)));
            }

            CheckAdministrativeObligations administrative = new CheckAdministrativeObligations(
                    ((String) actMap.get("Computer")),
                    ((List<String>) actMap.get("Conditions")),
                    studentsPairs,
                    (DepartmentPrivateState) actorThreadPool.getPrivateState((String) actMap.get("Department")),
                    ((String) actMap.get("Department")));

            return new ImmutableTriple<>(administrative, ((String) actMap.get("Department")),
                    actorThreadPool.getPrivateState((String) actMap.get("Department")));
        } else if (actMap.get("Action").equals("Register With Preferences")) {

            RegisterWithPreferences regWithPref = new RegisterWithPreferences(
                    ((StudentPrivateState) actorThreadPool.getPrivateState((String) actMap.get("Student"))),
                    ((String) actMap.get("Student")),
                    (genGradesPairs((List<String>) actMap.get("Preferences"), (List<String>) actMap.get("Grade"))));

            return new ImmutableTriple<>(regWithPref, ((String) actMap.get("Student")),
                    actorThreadPool.getPrivateState((String) actMap.get("Student")));

            // deprecated:
        } else if (actMap.get("Action").equals("End Registeration")) {

            dpss = getAllDepartments();
            return new ImmutableTriple<>(null, null, null);
        }

        return null;
    }


    /**
     * @param courseName
     * @param spaces initial placs in course
     * @param pre list of prequisites
     *
     *            generate course privates state and add it to map in pool
     */
    private static void genCoursePSInPool(String courseName, int spaces, List<String> pre) {
        if (!actorThreadPool.getActors().containsKey(courseName)) {
            CoursePrivateState cps = new CoursePrivateState();
            cps.setPrequisites(pre);
            cps.InitAvailableAndRegistered(spaces);
            actorThreadPool.getActors().put(courseName, cps);

        }
    }


    /**
     * @param studentName
     * @param sps -  generated private state
     *
     *            add StudentPrivateState to pool's map if n/a
     */
    private static void addStudentPStoPool(String studentName, StudentPrivateState sps) {
        if (!actorThreadPool.getActors().containsKey(studentName)) {
            actorThreadPool.getActors().put(studentName, sps);

        }
    }


    /**
     * @return map of departments by name
     */
    private static Map<String, DepartmentPrivateState> getAllDepartments() {
        HashMap<String, DepartmentPrivateState> dpss = new HashMap<>();
        for (String actor : myMap.keySet()) {
            if (myMap.get(actor) instanceof DepartmentPrivateState) {
                dpss.put(actor, (DepartmentPrivateState) myMap.get(actor));
            }
        }
        return dpss;
    }


    /**
     * @param departmentName
     * @return  DepartmentPrivateState if a/ @pool's map, gen. and add to pool if not - then return
     */
    private static DepartmentPrivateState getOrGenDepartmentActor(String departmentName) {
        DepartmentPrivateState dps = (DepartmentPrivateState) actorThreadPool.getPrivateState(departmentName);

        if (dps == null) {
            dps = new DepartmentPrivateState();
            actorThreadPool.getActors().put(departmentName, dps);
        }
        return dps;
    }


    /**
     * @param grade
     * @return  parsed int from string - if cannot parse return -1
     */
    private static Integer getIntFromString(String grade) {

        Integer gr = -1;
        try {
            gr = Integer.parseInt(grade);
        } catch (NumberFormatException ex) {
        }

        return gr;
    }


    /**
     * @param preferences
     * @param grade
     * @return generate list of pairs from 2 lists & return
     */
    private static List<ImmutablePair<String, Integer>> genGradesPairs(List<String> preferences, List<String> grade) {

        List<ImmutablePair<String, Integer>> courseGrades = new ArrayList<>();
        Iterator<String> it = grade.iterator();
        for (String course : preferences) {
            courseGrades.add(new ImmutablePair<>(course, getIntFromString(it.next())));
        }
        return courseGrades;

    }


}
