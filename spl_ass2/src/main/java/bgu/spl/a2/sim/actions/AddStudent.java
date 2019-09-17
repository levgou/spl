package bgu.spl.a2.sim.actions;


import bgu.spl.a2.sim.privateStates.DepartmentPrivateState;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;

public class AddStudent extends ActionServices<ImmutablePair<Enum, String>> {

    private String studentName;
    private DepartmentPrivateState dps;
    private String department;


    public AddStudent(String studentName, String department) {

        super();
        this.department = department;
        this.studentName = studentName;
        this.dps = getDepartment(department);
        setActionName("Add Student");
    }


    /**
     *  check if student is not already in dep. , if not - add it
     */
    @Override
    protected void start() {



        // checks if this student exists in this department
        if (studentIsInDepartment(studentName, dps)) {
            this.complete(new ImmutablePair<>(statuses.ALREADY_IN_DEPARTMENT,
                    String.format("Student %s already in department: %s", studentName, department)));
            return;
        }

        // adds the student to the department
        dps.addStudent(studentName);
        complete(new ImmutablePair<>(statuses.SUCCESS,
                String.format("Student %s been added to department: %s", studentName, department)));
    }

}

