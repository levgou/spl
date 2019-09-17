package bgu.spl.a2.structures;

import bgu.spl.a2.Action;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 *  Override toString of HashMap for more useful debug
 */
public class StringableHashMap extends java.util.HashMap<String,
        ImmutablePair<AtomicBoolean, ConcurrentLinkedQueue<Action<?>>>> {

    public StringableHashMap() {
        super();
    }

    @Override
    public String toString() {
        try {

            String repr = "StringableHashMap {";
            for (String key : this.keySet()) {
                repr += "\n";
                repr += String.format("Actor: %s | Taken %s | Q: %s", key, get(key).left.get(), get(key).right);
            }
            repr += "}";
            return repr;
        }
        catch (ConcurrentModificationException ex) {
            return toString();
        }
    }

}
