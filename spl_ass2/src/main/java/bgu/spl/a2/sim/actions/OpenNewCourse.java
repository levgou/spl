package bgu.spl.a2.sim.actions;

import bgu.spl.a2.Action;
import bgu.spl.a2.callback;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import bgu.spl.a2.sim.privateStates.DepartmentPrivateState;
import bgu.spl.a2.structures.ActionHolder;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;

public class OpenNewCourse extends ActionServices<ImmutablePair<Enum, String>> {

    private Integer courseCapacity;
    private List<String> prequisites;
    private String courseName;
    private String department;
    private DepartmentPrivateState dps;

    public OpenNewCourse(Integer courseCapacity, List<String> prequisites, String courseName, String department) {
        super();
        this.courseCapacity = courseCapacity;
        this.prequisites = prequisites;
        this.courseName = courseName;
        this.department = department;
        this.dps = getDepartment(department);
        setActionName("Open Course");

    }

    /**
     *  check if course is not already in department, if not - add it
     */
    @Override
    protected void start() {

        // check if course already exists
        if (courseInDepartment(courseName, dps)) {
            complete(new ImmutablePair<>(statuses.ALREADY_IN_DEPARTMENT,
                    String.format("Course %s already in department: %s", courseName, department)));
            return;
        }

        dps.addCourse(courseName);
        complete(new ImmutablePair<>(statuses.SUCCESS,
                        String.format("Course %s added to department: %s", courseName, department)));

    }
}
