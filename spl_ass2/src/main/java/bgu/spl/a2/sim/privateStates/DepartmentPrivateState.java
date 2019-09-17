package bgu.spl.a2.sim.privateStates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import bgu.spl.a2.PrivateState;

/**
 * this class describe department's private state
 */
public class DepartmentPrivateState extends PrivateState implements Serializable {
    private List<String> courseList;
    private List<String> studentList;

    /**
     * Implementors note: you may not add other constructors to this class nor
     * you allowed to add any other parameter to this constructor - changing
     * this may cause automatic tests to fail..
     */
    public DepartmentPrivateState() {
        courseList = new ArrayList<>();
        studentList = new ArrayList<>();
    }


    public List<String> getCourseList() {
        return courseList;
    }


    public List<String> getStudentList() {
        return studentList;
    }


    /**
     * @param courseName course to add to list
     * @return false if already in list else: true
     */
    public boolean addCourse(String courseName) {
        if (courseList.contains(courseName)) {
            return false;
        }

        courseList.add(courseName);
        return true;

    }


    /**
     * @param studentName student to add to list
     * @return false if already in list, else: true
     */
     public boolean addStudent(String studentName) {
        if (studentList.contains(studentName)) {
            return false;
        }

        studentList.add(studentName);
        return true;

    }


    public void removeCourse(String courseName){
        this.courseList.remove(courseName);
    }


    @Override
    public String toString() {
        return super.toString() + " \n" +  "DepartmentPrivateState{" +
                "courseList=" + courseList +
                ", studentList=" + studentList +
                '}';
    }
}
