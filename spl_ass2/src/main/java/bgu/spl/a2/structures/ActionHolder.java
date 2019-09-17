package bgu.spl.a2.structures;

import bgu.spl.a2.Action;
import java.util.LinkedList;

/**
 *  holder class for submitting 1 action to then
 */
public class ActionHolder extends LinkedList<Action<?>> {

    public ActionHolder(Action<?> act) {
        super();
        add(act);
    }
}
