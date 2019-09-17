package bgu.spl.a2.sim;

import bgu.spl.a2.Promise;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * this class is related to {@link Computer}
 * it indicates if a computer is free or not
 * <p>
 * Note: this class can be implemented without any synchronization.
 * However, using synchronization will be accepted as long as the implementation is blocking free.
 */

public class SuspendingMutex {

    private AtomicBoolean flag;
    private ConcurrentLinkedQueue<Promise<Computer>> pQueue;
    private Computer computer;

    /**
     * Constructor
     *
     * @param computer
     */
    public SuspendingMutex(Computer computer) {

        this.computer = computer;
        pQueue = new ConcurrentLinkedQueue<>();
        flag = new AtomicBoolean(false);
    }

    /**
     * Computer acquisition procedure
     * Note that this procedure is non-blocking and should return immediately
     *
     * @return a promise for the requested computer
     */
    public Promise<Computer> down() {

        Promise<Computer> promise = new Promise<>();

        if (flag.compareAndSet(false, true)) {
            promise.resolve(Warehouse.compMap.get(computer.getType()).right);
            return promise;

        }
        pQueue.add(promise);
        return promise;
    }

    /**
     * Computer return procedure
     * releases a computer which becomes available in the warehouse upon completion
     */
    public void up() {
        try {
            pQueue.poll().resolve(computer);
        } catch (NullPointerException e) {
        } // could be null if Q is empty

        flag.compareAndSet(true, false);
    }
}
