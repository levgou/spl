package bgu.spl.a2.sim.actions;

import bgu.spl.a2.Promise;
import bgu.spl.a2.callback;
import bgu.spl.a2.sim.privateStates.StudentPrivateState;
import bgu.spl.a2.structures.ActionHolder;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;

public class RegisterWithPreferences extends ActionServices<ImmutablePair<Enum, String>> {


    private StudentPrivateState sps;
    private String name;
    private List<ImmutablePair<String, Integer>> courses;
    private boolean reg;
    private String regedCourse;


    public RegisterWithPreferences(StudentPrivateState sps, String name, List<ImmutablePair<String, Integer>> courses) {
        this.sps = sps;
        this.name = name;
        this.courses = courses;
        this.reg = false;
        this.setActionName("Register With Preferences");
    }

    @Override
    protected void start() {

        // try to register to first course in the list
        // if fail the course will send the registration task to the next course in the list

        String firstCourseName = courses.get(0).left;
        TryToRegCourse firstReg = new TryToRegCourse(0);
        Promise<Boolean> regPromise =
                (Promise<Boolean>) sendMessage(firstReg, firstCourseName, getCourse(firstCourseName));

        then(new ActionHolder(firstReg), new callback() {

            @Override
            public void call() {
                if (regPromise.get()) {
                    complete(new ImmutablePair<>(statuses.SUCCESS, String.format("Registered to: %s", regedCourse)));
                } else {
                    complete(new ImmutablePair<>(statuses.FAIL, "Failed to reg. to all courses"));
                }
            }

        });

    }


    // =================================================================================================================

    /**
     * this class supports iteration over courses list and each time try to reg.
     * when succeed/ run out of courses - fallback completing all TryToRegCourse calls
     */
    private class TryToRegCourse extends ActionServices<Boolean> {

        private int placeInList;
        private Promise<Boolean> nextCoursePromise;

        public TryToRegCourse(int placeInList) {
            this.placeInList = placeInList;
            this.nextCoursePromise = null;
        }

        @Override
        protected void start() {
            Integer curCourseGrade = courses.get(placeInList).right;
            String curCourseName = courses.get(placeInList).left;
            ParticipateInCourse register = new ParticipateInCourse(name, sps, curCourseName, curCourseGrade);
            sendMessage(register, curCourseName, getCourse(curCourseName));

            then(new ActionHolder(register), new callback() {

                @Override
                public void call() {
                    // succeeded to reg. to current course
                    if (register.getResult().get().left == statuses.SUCCESS) {
                        complete(true);
                        regedCourse = courses.get(placeInList).left;
                        // couldn't reg. to current course - send the task to next course if possible
                    } else {
                        // tried to reg to all courses and failed
                        if (placeInList == courses.size() - 1) {
                            complete(false);
                            return;
                        }

                        String nextCourseName = courses.get(placeInList + 1).left;
                        TryToRegCourse nextReg = new TryToRegCourse(placeInList + 1);
                        nextCoursePromise =
                                (Promise<Boolean>) sendMessage(nextReg, nextCourseName, getCourse(nextCourseName));

                        // in order for the action to be re-inserted twice - it's dependencies shouldn't be resolved
                        unresolveDependencies();
                        // override callback with status checkup of next course
                        then(new ActionHolder(nextReg), new callback() {

                            @Override
                            public void call() {
                                if (nextCoursePromise.get()) {
                                    complete(true);
                                } else {
                                    complete(false);
                                }
                            }

                        });
                    }

                }

            }); // then
        } // start
    } // TryToRegCourse
}
