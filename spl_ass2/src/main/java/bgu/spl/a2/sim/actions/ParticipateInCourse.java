package bgu.spl.a2.sim.actions;

import bgu.spl.a2.Action;
import bgu.spl.a2.Promise;
import bgu.spl.a2.callback;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import bgu.spl.a2.sim.privateStates.StudentPrivateState;
import bgu.spl.a2.structures.ActionHolder;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class ParticipateInCourse extends ActionServices<ImmutablePair<Enum, String>> {

    private String studentName;
    private String courseName;
    private int grade;
    private StudentPrivateState sps;
    private CoursePrivateState cps;


    public ParticipateInCourse(String studentName, StudentPrivateState sps, String courseName, int grade) {
        this.courseName = courseName;
        this.studentName = studentName;
        this.grade = grade;
        this.sps = sps;
        setActionName("Participate In Course");
        cps = getCourse(courseName);

    }


    /**
     *  sequence: ask student to check if he can reg. -> check if this fits to add student & add if possible ->
     *      tell student to add this course to its' grades map
     */
    @Override
    protected void start() {

        ParticipateInCourse me = this;

        // check prerequisites
        ActionServices<Boolean> checkPre = new ActionServices<Boolean>() {
            @Override
            protected void start() {
                complete(checkPassedCourses(cps.getPrequisites(), sps.getGrades()));
            }
        };

        Promise<Boolean> studentPromise = (Promise<Boolean>) sendMessage(checkPre, studentName, sps);

        then(new ActionHolder(checkPre), () -> {

            // no place in course
            if (cps.getAvailableSpots() == 0) {
                complete(new ImmutablePair<>(statuses.FAIL,
                        String.format("No available spot for you in %s course :(", courseName)));
                return;
            }

            // course is closed
            else if (cps.getAvailableSpots() == -1) {
                complete(new ImmutablePair<>(statuses.ILLEGAL_ACTION,
                        String.format(" %s course is closed:(", courseName)));
                return;
            }

            // student doesn't meet prerequisites
            if (! studentPromise.get()) {
                complete(new ImmutablePair<>(statuses.FAIL,
                        String.format("Student: %s doesn't meet course: %s pre",studentName, courseName)));
                return;
            }

            // adding student
            // tell student to update it's private state
            Action<Boolean> doSign = new ActionServices<Boolean>() {
                @Override
                protected void start() {
                    sps.addCourse(courseName, grade);
                    complete(true);
                }
            };

            cps.addStudentToCourse(studentName);
            sendMessage(doSign, studentName, sps).subscribe(new callback() {
                @Override
                public void call() {
                    me.complete(new ImmutablePair<>(statuses.SUCCESS,
                            String.format("Added: %s to course: %s with grade: %d",studentName, courseName, grade)));
                }
            });


        });

    }

}

