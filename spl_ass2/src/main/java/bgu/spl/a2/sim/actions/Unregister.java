package bgu.spl.a2.sim.actions;

import bgu.spl.a2.Action;
import bgu.spl.a2.Promise;
import bgu.spl.a2.callback;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import bgu.spl.a2.sim.privateStates.StudentPrivateState;
import bgu.spl.a2.structures.ActionHolder;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class Unregister extends ActionServices<ImmutablePair<Enum, String>>{

    private CoursePrivateState cps;
    private volatile StudentPrivateState sps;
    private String courseName;
    private String studentName;

    public Unregister(CoursePrivateState cps, StudentPrivateState sps, String courseName, String studentName) {
        this.cps = cps;
        this.sps = sps;
        this.courseName = courseName;
        this.studentName = studentName;
        this.setActionName("Unregister");
    }

    /**
     *      sequence: ping student (by checking if he is reg.) -> remove from own list -> tell student to remove itself
     *      separate action like this to match adding sequence - this [reg->unreg] should work properly
     */
    @Override
    protected void start() {
        Unregister me = this;

        if (myRegStatus == statuses.REG_CLOSED){
            complete(new ImmutablePair<>(statuses.FAIL, "Registration period is closed!"));
            return;
        }

        // action for student to check if course in grade map
        ActionServices<Boolean> isRegistered = new ActionServices<Boolean>() {
            @Override
            protected void start() {
                if (courseIsInGradesOfStudent(courseName, sps)) {
                    complete(true);
                }
                else complete(false);
            }
        } ;

        Promise<Boolean> studentPromise = (Promise<Boolean>) sendMessage(isRegistered, studentName, sps);
        // check upon student result and remove student from list
        then(new ActionHolder(isRegistered), () ->{

            // tell student to remove itself from it's private state
            Action<Boolean> doRm = new ActionServices<Boolean>() {
                @Override
                protected void start() {
                    sps.removeCourseFromGrades(courseName);
                    complete(true);
                }
            };

            cps.removeStudentFromCourse(studentName);

            // students completes this - after finishing removing itself
            sendMessage(doRm, studentName, sps).subscribe(new callback() {
                @Override
                public void call() {
                    me.complete(new ImmutablePair<>(statuses.SUCCESS,
                            String.format("Student %s unregistered successfully from %s course",
                                    studentName, courseName)));
                }
            });

        });
    }
}
