package bgu.spl.a2.sim.actions;

import bgu.spl.a2.Promise;
import bgu.spl.a2.callback;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import bgu.spl.a2.sim.privateStates.DepartmentPrivateState;
import bgu.spl.a2.structures.ActionHolder;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.List;

public class CloseCourse extends ActionServices<ImmutablePair<Enum, String>> {

    private CoursePrivateState cps;
    private String courseName;
    private DepartmentPrivateState dps;
    private String department;


    public CloseCourse(CoursePrivateState cps, String courseName, DepartmentPrivateState dps, String department) {
        this.cps = cps;
        this.courseName = courseName;
        this.dps = dps;
        this.department = department;
        this.setActionName("Close Course");
    }


    /**
     *   unreg. all student, and set available   places to be -1
     */
    @Override
    protected void start() {

        // create actions for course - unregister all its students
        ActionServices<Boolean> unregisterAll = new ActionServices<Boolean>() {
            @Override
            protected void start() {
                List<Promise<ImmutablePair<Enum, String>>> unregPromises = new ArrayList<>();
                List<Unregister> unregActions = new ArrayList<>();

                for (String s : cps.getRegStudents()) {
                    Unregister unregAction = new Unregister(cps, getStudent(s), courseName, s);
                    unregActions.add(unregAction);
                }

                for (Unregister unreg : unregActions) {
                    unregPromises.add((Promise<ImmutablePair<Enum, String>>) sendMessage(unreg, courseName, cps));

                }

                // course could be student-less
                if (unregActions.size() > 0) {
                    // check that students had been un-registered successfully
                    then(unregActions, () -> {
                        for (Promise<ImmutablePair<Enum, String>> p : unregPromises) {
                            if (p.get().left != statuses.SUCCESS) {
                                complete(false);
                                return;
                            }
                        }
                        complete(true);
                    });
                } else {
                    complete(true);
                }
            }
        };
        // -------------------------------------------------------------------------------------------------------------

        cps.setAsClosed();

        // check that un-registration passed successfully and change the course to be un-registable
        Promise<Boolean> coursePromise = (Promise<Boolean>) sendMessage(unregisterAll, courseName, cps);
        then(new ActionHolder(unregisterAll), new callback() {
            @Override
            public void call() {
                if (cps.getRegStudents().size() != 0) {
                    complete(new ImmutablePair<>(statuses.FAIL,
                            String.format("course %s wasn't closed properly", courseName)));

                } else {
                    dps.removeCourse(courseName);
                    complete(new ImmutablePair<>(statuses.SUCCESS,
                            String.format("course %s closed successfully", courseName)));
                }
            }
        });
    }
}
