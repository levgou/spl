package bgu.spl.a2.sim.actions;

import bgu.spl.a2.Action;
import bgu.spl.a2.callback;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import bgu.spl.a2.sim.privateStates.DepartmentPrivateState;
import bgu.spl.a2.structures.ActionHolder;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;

public class AnnounceEndOfReg extends ActionServices<ImmutablePair<Enum, String>>{


    private DepartmentPrivateState dps;
    private String department;

    public AnnounceEndOfReg(DepartmentPrivateState dps, String department) {
        this.dps = dps;
        this.department = department;
        this.setActionName("End Registeration");
    }

    @Override
    protected void start() {
        regStatus = statuses.REG_CLOSED;
        ArrayList<Action<ImmutablePair<Boolean, String>>> closeRegAndSmallCourses =
                new ArrayList<>(dps.getCourseList().size() * 2);

        // add only courses already in dps's list because if reg_closed -> no more new courses will be added
        for (String course : dps.getCourseList()) {
            Action<ImmutablePair<Boolean, String>> courseDuty = new closeRegCourseDuty(dps, department, course);
            closeRegAndSmallCourses.add(courseDuty);
            // base assumption: pool will have private state if the course in department's list
            sendMessage(courseDuty, course, pool.getPrivateState(course));
        }

        then(closeRegAndSmallCourses, new callback() {

            @Override
            public void call() {

                ArrayList<String> closedCourses = new ArrayList<>(closeRegAndSmallCourses.size());
                for (Action<ImmutablePair<Boolean, String>> act : closeRegAndSmallCourses) {
                    if (act.getResult().get().left){
                        closedCourses.add(act.getResult().get().right);
                    }
                }
                complete(new ImmutablePair<>(statuses.SUCCESS,
                        String.format("Department: %s, closed registration for: %s, closed courses: %s",
                                department, dps.getCourseList(), closedCourses) ));
            }

        });

    }

    // =================================================================================================================
    // this helper class to close regist. for specific course & close the course if it has less than 5 students
    private class closeRegCourseDuty extends  Action<ImmutablePair<Boolean, String>> {

        private final DepartmentPrivateState dps;
        private String department;
        private final String courseName;

        public closeRegCourseDuty(DepartmentPrivateState dps, String department, String courseName) {

            this.dps = dps;
            this.department = department;
            this.courseName = courseName;
        }

        @Override
        protected void start() {
            closeRegPeriod();
            CoursePrivateState cps =((CoursePrivateState)pool.getPrivateState(courseName));
            if (cps.getRegistered() < 5) {
                Action<ImmutablePair<Enum, String>> closeCourse = new CloseCourse(cps,courseName, dps, department);
                sendMessage(closeCourse, courseName, cps);
                then(new ActionHolder(closeCourse), new callback() {

                    @Override
                    public void call() {
                        complete(new ImmutablePair<>(true, courseName));
                    }

                });
            }

            // false indicates we didnt close the course
            complete(new ImmutablePair<>(false, courseName));

        }
    }
}
