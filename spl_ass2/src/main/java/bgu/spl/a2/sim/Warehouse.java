package bgu.spl.a2.sim;

import bgu.spl.a2.Promise;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * represents a warehouse that holds a finite amount of computers
 * and their suspended mutexes.
 * implemented as singleton - thus same computers are available to all
 */
public class Warehouse {

    protected static HashMap<String,
                ImmutablePair<SuspendingMutex, Computer>> compMap;


    private static class WarehouseHolder {
        private static Warehouse instance = new Warehouse();
    }


    private Warehouse() {
        compMap = new HashMap<>();
    }


    public static Warehouse getInstance() {
        return WarehouseHolder.instance;
    }


    /**
     * @param computers add multiple computers to warehouse - useful for initiation of warehouse
     */
    public void addComputers(Collection<Computer> computers) {

        computers.forEach((Computer c) ->
                compMap.put(c.getType(), new ImmutablePair<>(new SuspendingMutex(c), c)));

    }

    /**
     * @param pc computer name
     * @return  promise that will hold a computer when resolved - subscribe to this accordingly
     */
    public Promise<Computer> acquire(String pc) {
        return compMap.get(pc).left.down();
    }


    /**
     * @param pc - release computer with this name - allowing the net promise to be resolved
     */
    public void release(String pc) {
        compMap.get(pc).left.up();
    }

}
