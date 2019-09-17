package bgu.spl.a2.sim.actions;

import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 *  increase places in course - using CoursePrivateState's func
 */
public class AddPlacesToCourse extends ActionServices<ImmutablePair<Enum, String>> {

    private CoursePrivateState cps;
    private int howMuch;


    public AddPlacesToCourse(CoursePrivateState cps, int howMuch) {
        this.cps = cps;
        this.howMuch = howMuch;
        this.setActionName("Add Spaces");
    }


    @Override
    protected void start() {
        cps.increaseAvailablePlaces(howMuch);
        complete(new ImmutablePair<>(statuses.SUCCESS,
                String.format("Added %d places to course", this.howMuch)));
    }
}
