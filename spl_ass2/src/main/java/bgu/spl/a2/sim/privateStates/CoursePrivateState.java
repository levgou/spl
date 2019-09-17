package bgu.spl.a2.sim.privateStates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import bgu.spl.a2.PrivateState;

/**
 * this class describe course's private state
 */
public class CoursePrivateState extends PrivateState implements Serializable {


    private Integer availableSpots;
    private Integer registered;
    private List<String> regStudents;
    private List<String> prequisites;

    /**
     * Implementors note: you may not add other constructors to this class nor
     * you allowed to add any other parameter to this constructor - changing
     * this may cause automatic tests to fail..
     */
    public CoursePrivateState() {
        regStudents = new ArrayList<>();

    }

    public Integer getAvailableSpots() {
        return availableSpots;
    }

    public Integer getRegistered() {
        return registered;
    }

    public List<String> getRegStudents() {
        return regStudents;
    }

    public List<String> getPrequisites() {
        return prequisites;
    }

    /**
     * set num of registered students - 0
     *
     * @param courseCapacity - will initialize available spots to be as initial course capacity
     */
    public void InitAvailableAndRegistered(Integer courseCapacity) {
        this.availableSpots = courseCapacity;
        this.registered = 0;
    }

    public void setPrequisites(List<String> prequisites) {
        this.prequisites = prequisites;
    }


    /**
     * @param name add student to list - to be used only after checking that the student qualifies for the course
     *             increase num of reg. students by 1 & decrease available places by 1
     */
    public void addStudentToCourse(String name) {

        regStudents.add(name);
        --availableSpots;
        ++registered;
    }

    /**
     * @param name student to remove
     *             if course is not closed - add places in course
     *             decrease num of reg. students by 1
     */
    public void removeStudentFromCourse(String name) {

        regStudents.remove(name);
        if (availableSpots != -1) {
            ++availableSpots;
        }
        --registered;
    }

    public void increaseAvailablePlaces(int howMuchMan) {
        availableSpots += howMuchMan;
    }

    /**
     *  close course -> availableSpots = -1
     */
    public void setAsClosed() {
        this.availableSpots = -1;
    }

    @Override
    public String toString() {
        return super.toString() + " \n" + "CoursePrivateState{" +
                "availableSpots=" + availableSpots +
                ", registered=" + registered +
                ", regStudents=" + regStudents +
                ", prequisites=" + prequisites +
                '}';
    }
}
