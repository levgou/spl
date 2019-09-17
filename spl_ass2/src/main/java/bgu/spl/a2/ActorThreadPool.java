package bgu.spl.a2;

import bgu.spl.a2.structures.SyncGetPuHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * represents an actor thread pool - to understand what this class does please
 * refer to your assignment.
 * <p>
 * Note for implementors: you may add methods and synchronize any of the
 * existing methods in this class *BUT* you must be able to explain why the
 * synchronization is needed. In addition, the methods you add can only be
 * private, protected or package protected - in other words, no new public
 * methods
 */
public class ActorThreadPool {

    private Thread[] workers;
    volatile private boolean beenShutDown;
    private SyncGetPuHashMap actors;
    private HashMap<String, PrivateState> privateStates;
    private final ActorThreadPool pool;
    private final VersionMonitor vm;
    private final Object newQLock;


    /**
     * creates a {@link ActorThreadPool} which has nthreads. Note, threads
     * should not get started until calling to the {@link #start()} method.
     * <p>
     * Implementors note: you may not add other constructors to this class nor
     * you allowed to add any other parameter to this constructor - changing
     * this may cause automatic tests to fail..
     *
     * @param nthreads the number of threads that should be started by this thread
     *                 pool
     */
    public ActorThreadPool(int nthreads) {

        workers = new Thread[nthreads];
        actors = new SyncGetPuHashMap();
        privateStates = new HashMap<>();
        pool = this;
        vm = new VersionMonitor();
        vm.init();
        beenShutDown = false;
        newQLock = new Object();

        for (int i = 0; i < nthreads; i++) {
            workers[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            // get action with info for handling <Action, ActorName, ActorPrivateState>
                            ImmutableTriple<Action<?>, String, PrivateState> actionPackage = pool.fetchAction();
                            if (actionPackage == null) {
                                vm.await(vm.getVersion());
                                continue;

                            }
                            actionPackage.left.handle(pool, actionPackage.middle, actionPackage.right);
                            releaseQ(actionPackage.middle);

                        }
                    } catch (InterruptedException ex) {
                    }
                }

            });
        }
    }


    /**
     * @param actorId   release Q to other threads after finishing working on it
     */
    private void releaseQ(String actorId) {
        ImmutablePair<AtomicBoolean, ConcurrentLinkedQueue<Action<?>>> curActor = actors.get(actorId);
        curActor.left.compareAndSet(true, false);
    }

    /**
     * getter for actors
     *
     * @return actors
     */
    public Map<String, PrivateState> getActors() {

        return privateStates;
    }


    /**
     * getter for actor's private state
     *
     * @param actorId actor's id
     * @return actor's private state
     */
    public PrivateState getPrivateState(String actorId) {

        return privateStates.get(actorId);
    }


    /**
     * iterates the actors map and looks for an un-forbidden actions queue
     *
     * @return <Action, ActorName, ActorPrivateState> if found. else, return null.
     */
    private ImmutableTriple<Action<?>, String, PrivateState> fetchAction() {

        String tmpId = null;
        Action<?> myAction = null;
        //looking for available & non-empty actions queue
        try {
            for (String id : actors.keySet()) {

                tmpId = id;

                ImmutablePair<AtomicBoolean, ConcurrentLinkedQueue<Action<?>>> curActor = actors.get(id);
                //checks the queue isn't suspended if not -> update to be suspended
                if (curActor.left.compareAndSet(false, true)) {

                    ConcurrentLinkedQueue<Action<?>> tempQ = curActor.right;
                    myAction = tempQ.poll();

                    // empty Q:
                    if (myAction == null) {
                        curActor.left.compareAndSet(true, false);
                        continue;
                    }

                    return new ImmutableTriple<>(myAction, id, privateStates.get(id));
                }
            }
        } catch (ConcurrentModificationException ex) { //in case the fail-fast iterator will recognize changes in map while iterates
            // if tmpId != null - try to re-fetch
            return fetchAction(); //re-start the fetching
        }
        //if nothing has been fetched
        return null;
    }


    /**
     * submits an action into an actor to be executed by a thread belongs to
     * this thread pool
     * NOTE: rely on the fact that submit will be handled by a single thread
     *
     * @param action     the action to execute
     * @param actorId    corresponding actor's id
     * @param actorState actor's private state (actor's information)
     */
    public void submit(Action<?> action, String actorId, PrivateState actorState) {

        if (beenShutDown) {
            return;
        }

        // if its a new actor, map his id in the map & create new actions queue
        // generated actor "is busy" until first item is added - no sense to allow access when for sure empty
        // sync this to prevent override new Q
        synchronized (newQLock) {
            if (!actors.containsKey(actorId)) {
                ImmutablePair<AtomicBoolean, ConcurrentLinkedQueue<Action<?>>> tmp =
                        new ImmutablePair<>(new AtomicBoolean(false), new ConcurrentLinkedQueue<>());

                actors.put(actorId, tmp);
                privateStates.put(actorId, actorState);
            }


            // add to Q
            actors.get(actorId).right.add(action);
            vm.inc(); // wake up some worker - its time to carry some breaks for miles

        }
    }

    /**
     * closes the thread pool - this method interrupts all the threads and waits
     * for them to stop - it is returns *only* when there are no live threads in
     * the queue.
     * <p>
     * after calling this method - one should not use the queue anymore.
     *
     * @throws InterruptedException if the thread that shut down the threads is interrupted
     */
    public void shutdown() throws InterruptedException {

        for (Thread t : workers) {
            t.interrupt();
        }
        beenShutDown = true;

    }

    /**
     * start the threads belongs to this thread pool
     */
    public void start() {

        for (Thread t : workers) {
            t.start();
        }
    }


}
