package bgu.spl.a2.structures;

import bgu.spl.a2.Action;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  sync get & put to prevent race conditions - don't use concurrent HashMap - to retain ConcurrentModificationException

 */
public class SyncGetPuHashMap extends StringableHashMap {

    @Override
    synchronized public ImmutablePair<AtomicBoolean, ConcurrentLinkedQueue<Action<?>>>
    put(String s, ImmutablePair<AtomicBoolean, ConcurrentLinkedQueue<Action<?>>> atomicBooleanConcurrentLinkedQueueImmutablePair) {

        return super.put(s, atomicBooleanConcurrentLinkedQueueImmutablePair);
    }

    @Override
    synchronized public ImmutablePair<AtomicBoolean, ConcurrentLinkedQueue<Action<?>>> get(Object o) {
        return super.get(o);
    }
}
