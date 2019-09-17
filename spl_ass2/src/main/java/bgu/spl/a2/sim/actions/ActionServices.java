package bgu.spl.a2.sim.actions;


import bgu.spl.a2.*;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import bgu.spl.a2.sim.privateStates.DepartmentPrivateState;
import bgu.spl.a2.sim.privateStates.StudentPrivateState;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.lang.reflect.Type;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: 12/17/17 remove unused funcs

/**
 * this class extends Action, in purpose to support all the different actions
 * with these elementary functions (queries, get, set etc.)
 *
 * @param <T>
 */
public abstract class ActionServices<T> extends Action<T> {

    // TODO: 12/24/17
//    protected Promise<?> savePointerOfOldPromise;

    //Enum suppose to support different results of actions
    public static enum statuses {
        SUCCESS,
        FAIL,
        ALREADY_IN_DEPARTMENT,
        ILLEGAL_ACTION,
        REG_OPEN,
        REG_CLOSED,
    }

    // accessed by all & indicates if reg period is still on
    public static Enum regStatus = statuses.REG_OPEN;

    // each action will have it's own regStatus thus actions submitted before change will finish
    protected Enum myRegStatus = regStatus;


    protected boolean studentIsInDepartment(String student, DepartmentPrivateState dps) {
        return dps.getStudentList().contains(student);
    }


    protected boolean courseInDepartment(String course, String department) {
        DepartmentPrivateState dep = (DepartmentPrivateState) pool.getPrivateState(department);
        if (dep == null) {
            return false;
        }

        return courseInDepartment(course, dep);
    }


    protected boolean courseInDepartment(String course, DepartmentPrivateState dps) {
        return dps.getCourseList().contains(course);
    }


    protected DepartmentPrivateState getDepartment(String name) {
        if (pool.getActors().get(name) instanceof DepartmentPrivateState) {
            return (DepartmentPrivateState) pool.getActors().get(name);
        }
        return null;
    }


    protected StudentPrivateState getStudent(String name) {
        if (pool.getActors().get(name) instanceof StudentPrivateState) {
            return (StudentPrivateState) pool.getActors().get(name);
        }
        return null;
    }


    protected CoursePrivateState getCourse(String name) {
        if (pool.getActors().get(name) instanceof CoursePrivateState) {
            return (CoursePrivateState) pool.getActors().get(name);
        }
        return null;
    }


    protected boolean checkPassedCourses(List<String> preq, Map<String, Integer> grades) {
        for (String s : preq) {
            if (grades.get(s) == null) {
                return false;
            }
        }
        return true;
    }


    protected Boolean courseIsInGradesOfStudent(String courseName, StudentPrivateState sps) {
        return getGradeOfStudent(courseName, sps) != null;

    }


    protected Integer getGradeOfStudent(String courseName, StudentPrivateState sps) {
        return sps.getGrades().get(courseName);
    }


    protected void closeRegPeriod() {
        myRegStatus = statuses.REG_CLOSED;
    }


    public static void setThreadPool(ActorThreadPool pool) {
        Action.pool = pool;
    }


    protected void unresolveDependencies(){
        dependenciesResolved = false;
    }


}
